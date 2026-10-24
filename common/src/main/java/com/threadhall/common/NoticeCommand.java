package com.threadhall.common;

public record NoticeCommand(String postId, String threadId, String kind, String authorName) {
}
