package com.threadhall.common;

import java.util.Locale;

public final class ForumRules {
    private ForumRules() {
    }

    public static String decision(String body) {
        if (body != null && body.toLowerCase(Locale.ROOT).contains("spam")) {
            return "REMOVED";
        }
        if (body != null && body.length() > 500) {
            return "QUEUED";
        }
        return "VISIBLE";
    }

    public static boolean bounce(String authorName) {
        return authorName != null && authorName.toLowerCase(Locale.ROOT).endsWith("@bounce.test");
    }
}
