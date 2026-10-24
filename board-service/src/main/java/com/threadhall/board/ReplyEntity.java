package com.threadhall.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "replies")
public class ReplyEntity {
    @Id
    private String id;
    @Column(name = "thread_id")
    private String threadId;
    @Column(name = "author_name")
    private String authorName;
    @Column(columnDefinition = "text")
    private String body;
    private String status;
    @Column(name = "notice_status")
    private String noticeStatus;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getThreadId() { return threadId; }
    public void setThreadId(String threadId) { this.threadId = threadId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNoticeStatus() { return noticeStatus; }
    public void setNoticeStatus(String noticeStatus) { this.noticeStatus = noticeStatus; }
}
