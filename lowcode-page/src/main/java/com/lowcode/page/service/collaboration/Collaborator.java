package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class Collaborator implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String userId;

    private String username;

    private String avatar;

    private long joinTime;

    private long lastActiveTime;

    private boolean online;

    private Map<String, Object> cursorPosition;

    private String color;

    public Collaborator() {
        this.joinTime = System.currentTimeMillis();
        this.lastActiveTime = System.currentTimeMillis();
        this.online = true;
    }

    public Collaborator(String sessionId, String userId, String username, String avatar) {
        this();
        this.sessionId = sessionId;
        this.userId = userId;
        this.username = username;
        this.avatar = avatar;
    }

    public void updateActiveTime() {
        this.lastActiveTime = System.currentTimeMillis();
    }
}
