package com.threadhall.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "counted_replies")
public class CountedReply {
    @Id
    @Column(name = "reply_id")
    private String replyId;
}
