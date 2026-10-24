package com.threadhall.moderation;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "moderation_decisions")
public class DecisionEntity {
    @Id
    private String postId;
    private String status;

    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
