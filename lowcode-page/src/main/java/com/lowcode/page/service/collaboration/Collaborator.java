package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class Collaborator implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String userId;

    private String username;

    private String avatar;

    private String color;

    private Long pageId;

    private Object cursorPosition;

    private Object selection;

    private Date joinTime;

    private Date lastActiveTime;

    private boolean online;

    public Collaborator() {
        this.joinTime = new Date();
        this.lastActiveTime = new Date();
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
        this.lastActiveTime = new Date();
    }
}
