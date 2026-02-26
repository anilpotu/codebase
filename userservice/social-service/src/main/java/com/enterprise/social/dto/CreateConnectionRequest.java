package com.enterprise.social.dto;

import javax.validation.constraints.NotNull;

public class CreateConnectionRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long connectedUserId;

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
}
