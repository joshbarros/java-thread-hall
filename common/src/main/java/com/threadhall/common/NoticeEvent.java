package com.threadhall.common;

public record NoticeEvent(String postId, String threadId, String kind, String status) {
}
