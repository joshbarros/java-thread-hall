package com.threadhall.notice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notices")
public class NoticeEntity {
    @Id
    @Column(name = "post_id")
    private String postId;
    @Column(name = "author_name")
    private String authorName;
    private String status;

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
