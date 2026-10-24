#!/usr/bin/env python3
"""Moderate, notify, bounce, and count overlapping replies."""

import base64
import json
import sys
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor

BOARD = "http://localhost:8141"
NOTICE = "http://localhost:8143"
MGMT = "http://localhost:15679"
AUTH = "platform:platform"
failures = []


def check(name, ok, detail=""):
    if ok:
        print(f"PASS  {name}")
    else:
        print(f"FAIL  {name} {detail}")
        failures.append(f"{name}: {detail}")


def call(method, url, body=None, auth=None, timeout=15):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        request.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed
    except Exception as ex:
        return 0, str(ex)


def wait_ok(url, seconds=180, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if call("GET", url, auth=auth)[0] == 200:
            return True
        time.sleep(1)
    return False


def until(label, fn, seconds=50):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(0.4)
    check(label, False, f"timed out last={last!r}")
    return None


def depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return None
    return body.get("messages", 0)


def grew(name, before, amount=1):
    def ready():
        now = depth(name)
        if now is not None and now >= before + amount:
            return now
        return None
    return until(f"dlq {name}", ready, 20)


def thread_state(thread_id):
    code, body = call("GET", f"{BOARD}/threads/{thread_id}")
    return body if code == 200 and isinstance(body, dict) else None


def main():
    check("board health", wait_ok(f"{BOARD}/actuator/health"))
    check("moderation health", wait_ok("http://localhost:8142/actuator/health"))
    check("notice health", wait_ok(f"{NOTICE}/actuator/health"))
    check("rabbit health", wait_ok(f"{MGMT}/api/overview", auth=AUTH))

    status, _ = call("POST", f"{BOARD}/boards", {"name": "  "})
    check("blank board is 400", status == 400, status)
    status, board = call("POST", f"{BOARD}/boards", {"name": "General"})
    check("create board", status == 201, board)
    board_id = board["id"]
    status, _ = call("POST", f"{BOARD}/boards/{board_id}/threads", {"authorName": "Ada", "title": "Hi", "body": "  "})
    check("blank body is 400", status == 400, status)
    status, _ = call("POST", f"{BOARD}/boards/missing/threads", {"authorName": "Ada", "title": "Hi", "body": "Hello"})
    check("unknown board is 404", status == 404, status)

    status, post = call("POST", f"{BOARD}/boards/{board_id}/threads",
                        {"authorName": "Ada", "title": "Hello", "body": "A short post."})
    check("create thread", status == 201 and post.get("status") == "PENDING", post)
    thread_id = post["id"]

    def visible():
        body = thread_state(thread_id)
        code, notice = call("GET", f"{NOTICE}/notices/{thread_id}")
        if body and body.get("status") == "VISIBLE" and body.get("noticeStatus") == "DELIVERED" and code == 200 and notice.get("status") == "DELIVERED":
            return body
        return None

    check("normal thread is visible and delivered", until("visible", visible) is not None)

    mod_dlq = depth("thread.moderate.dlq") or 0
    status, spam = call("POST", f"{BOARD}/boards/{board_id}/threads",
                        {"authorName": "Ada", "title": "No", "body": "this is Spam mail"})
    spam_id = spam["id"]

    def removed():
        body = thread_state(spam_id)
        code, _ = call("GET", f"{NOTICE}/notices/{spam_id}")
        if body and body.get("status") == "REMOVED" and code == 404:
            return body
        return None

    check("spam is removed with no notice", until("spam", removed) is not None)
    check("spam dead-letters moderation", grew("thread.moderate.dlq", mod_dlq) == mod_dlq + 1)

    mod_dlq = depth("thread.moderate.dlq") or 0
    status, queued = call("POST", f"{BOARD}/boards/{board_id}/threads",
                          {"authorName": "Ada", "title": "Long", "body": "x" * 501})
    queued_id = queued["id"]

    def waiting():
        body = thread_state(queued_id)
        if body and body.get("status") == "QUEUED":
            return body
        return None

    check("501 characters is queued", until("queued", waiting) is not None)
    check("long body dead-letters moderation", grew("thread.moderate.dlq", mod_dlq) == mod_dlq + 1)

    status, edge = call("POST", f"{BOARD}/boards/{board_id}/threads",
                        {"authorName": "Ada", "title": "Edge", "body": "y" * 500})
    edge_id = edge["id"]

    def edge_visible():
        body = thread_state(edge_id)
        if body and body.get("status") == "VISIBLE" and body.get("noticeStatus") == "DELIVERED":
            return body
        return None

    check("500 characters is visible", until("edge", edge_visible) is not None)

    notice_dlq = depth("thread.notice.dlq") or 0
    status, bounced = call("POST", f"{BOARD}/boards/{board_id}/threads",
                           {"authorName": "A@Bounce.Test", "title": "Hi", "body": "Deliver the post, not the mail."})
    bounced_id = bounced["id"]

    def bounce():
        body = thread_state(bounced_id)
        code, _ = call("GET", f"{NOTICE}/notices/{bounced_id}")
        if body and body.get("status") == "VISIBLE" and body.get("noticeStatus") == "BOUNCED" and code == 404:
            return body
        return None

    check("bounced author stays visible", until("bounce", bounce) is not None)
    check("bounced notice dead-letters", grew("thread.notice.dlq", notice_dlq) == notice_dlq + 1)

    def reply(i):
        return call("POST", f"{BOARD}/threads/{thread_id}/replies",
                    {"authorName": f"Person {i}", "body": f"Reply number {i} is fine."})

    with ThreadPoolExecutor(max_workers=20) as pool:
        raced = list(pool.map(reply, range(20)))
    ids = [body["id"] for code, body in raced if code == 201 and isinstance(body, dict)]
    check("twenty replies accepted", len(ids) == 20, [code for code, _ in raced])

    def counted():
        body = thread_state(thread_id)
        if body and body.get("replyCount") == 20:
            return body
        return None

    check("twenty replies are counted", until("count", counted, 60) is not None)
    visible_replies = 0
    for reply_id in ids:
        def one(reply_id=reply_id):
            code, body = call("GET", f"{BOARD}/replies/{reply_id}")
            notice_code, notice = call("GET", f"{NOTICE}/notices/{reply_id}")
            if code == 200 and body.get("status") == "VISIBLE" and body.get("noticeStatus") == "DELIVERED" and notice_code == 200 and notice.get("status") == "DELIVERED":
                return body
            return None
        if until(f"reply {reply_id[:8]}", one, 40):
            visible_replies += 1
    check("twenty replies are visible and delivered", visible_replies == 20, visible_replies)

    before = thread_state(thread_id).get("replyCount")
    status, junk = call("POST", f"{BOARD}/threads/{thread_id}/replies",
                        {"authorName": "Ada", "body": "spam reply"})
    junk_id = junk["id"]

    def junk_removed():
        code, body = call("GET", f"{BOARD}/replies/{junk_id}")
        if code == 200 and body.get("status") == "REMOVED":
            return body
        return None

    check("spam reply is removed", until("spam reply", junk_removed) is not None)
    time.sleep(1)
    check("spam reply does not change the count", thread_state(thread_id).get("replyCount") == before, thread_state(thread_id))

    print("\n== summary ==")
    if failures:
        print(f"{len(failures)} failed")
        for item in failures:
            print(f" - {item}")
        sys.exit(1)
    print("thread hall smoke passed")


if __name__ == "__main__":
    main()
