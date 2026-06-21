package com.lowcode.monitor.entity;

import lombok.Data;
import java.io.Serializable;

@Data
public class LoadTestResult implements Serializable {

    private long timestamp;

    private int responseTimeMs;

    private int statusCode;

    private boolean success;

    private String errorMessage;

    private long responseSize;

    private String virtualUserId;

    private String requestId;
}
