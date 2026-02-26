package com.enterprise.social.dto;

import java.time.LocalDateTime;

public class ConnectionDTO {

    private Long id;
    private Long userId;
    private Long connectedUserId;
    private String status;
    private LocalDateTime createdAt;

    public ConnectionDTO() {
    }

    public ConnectionDTO(Long id, Long userId, Long connectedUserId, String status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.connectedUserId = connectedUserId;
        this.status = status;
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

    public Long getConnectedUserId() {
        return connectedUserId;
    }

    public void setConnectedUserId(Long connectedUserId) {
        this.connectedUserId = connectedUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
