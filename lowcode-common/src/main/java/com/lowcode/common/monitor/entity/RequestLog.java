package com.lowcode.common.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RequestLog implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;

    private String requestUrl;

    private String requestMethod;

    private String requestParams;

    private String requestIp;

    private Long userId;

    private String username;

    private String serviceName;

    private Integer responseStatus;

    private Long costTime;

    private String errorStack;

    private String userAgent;

    private LocalDateTime requestTime;

    private LocalDateTime responseTime;
}
