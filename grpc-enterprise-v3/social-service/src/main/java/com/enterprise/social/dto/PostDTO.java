package com.enterprise.social.dto;

import java.time.LocalDateTime;

public class PostDTO {

    private Long id;
    private Long userId;
    private String content;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;

    public PostDTO() {
    }

    public PostDTO(Long id, Long userId, String content, Integer likesCount,
                   Integer commentsCount, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.likesCount = likesCount;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
