package com.threadhall.board;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "threads")
public class ThreadEntity {
    @Id
    private String id;
    @Column(name = "board_id")
    private String boardId;
    @Column(name = "author_name")
    private String authorName;
    private String title;
    @Column(columnDefinition = "text")
    private String body;
    private String status;
    @Column(name = "notice_status")
    private String noticeStatus;
    @Column(name = "reply_count")
    private int replyCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getBoardId() { return boardId; }
    public void setBoardId(String boardId) { this.boardId = boardId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNoticeStatus() { return noticeStatus; }
    public void setNoticeStatus(String noticeStatus) { this.noticeStatus = noticeStatus; }
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
}
