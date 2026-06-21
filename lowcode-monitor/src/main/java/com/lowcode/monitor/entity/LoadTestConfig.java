package com.lowcode.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class LoadTestConfig implements Serializable {

    private String testId;

    private String testName;

    private String targetUrl;

    private String httpMethod = "GET";

    private Map<String, String> headers;

    private String requestBody;

    private int virtualUsers = 10;

    private int durationSeconds = 60;

    private int rampUpSeconds = 10;

    private int thinkTimeMs = 1000;

    private int timeoutMs = 30000;

    private String contentType = "application/json";

    private List<String> assertionStatusCodes;

    private String assertionBodyContains;

    private boolean collectDetailedMetrics = true;
}
