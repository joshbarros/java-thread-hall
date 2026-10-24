package com.threadhall.common;

public record ModerateCommand(String postId, String threadId, String kind, String authorName, String body) {
}
