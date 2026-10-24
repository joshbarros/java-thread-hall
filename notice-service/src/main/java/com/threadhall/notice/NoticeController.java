package com.threadhall.notice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class NoticeController {

    private final NoticeRepository notices;

    public NoticeController(NoticeRepository notices) {
        this.notices = notices;
    }

    @GetMapping("/notices/{postId}")
    public NoticeEntity notice(@PathVariable String postId) {
        return notices.findById(postId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "notice not found"));
    }
}
