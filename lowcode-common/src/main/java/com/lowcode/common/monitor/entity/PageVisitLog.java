package com.lowcode.common.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PageVisitLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String sessionId;

    private String userId;

    private String username;

    private String pagePath;

    private String pageTitle;

    private String referrer;

    private String userAgent;

    private String clientIp;

    private Long stayTime;

    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;
}
