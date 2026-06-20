package com.lowcode.page.entity.collaboration;

import lombok.Data;

import java.util.Date;

@Data
public class Collaborator {

    private String sessionId;

    private Long userId;

    private String username;

    private String avatar;

    private String color;

    private Long pageId;

    private String cursorPosition;

    private String selection;

    private Date joinTime;

    private Date lastActiveTime;

    private boolean isOnline;

}
