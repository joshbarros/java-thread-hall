package com.threadhall.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ForumRulesTest {

    @Test
    void spamAndLengthDecideThePost() {
        assertEquals("REMOVED", ForumRules.decision("this is Spam"));
        assertEquals("VISIBLE", ForumRules.decision("x".repeat(500)));
        assertEquals("QUEUED", ForumRules.decision("x".repeat(501)));
        assertEquals("VISIBLE", ForumRules.decision("hello"));
    }

    @Test
    void bounceIsCaseInsensitive() {
        assertTrue(ForumRules.bounce("A@Bounce.Test"));
        assertFalse(ForumRules.bounce("a@example.com"));
    }
}
