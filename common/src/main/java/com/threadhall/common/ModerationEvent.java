package com.threadhall.common;

public record ModerationEvent(String postId, String threadId, String kind, String status) {
}
