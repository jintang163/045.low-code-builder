package com.lowcode.common.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AlertRule implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String ruleName;

    private String ruleType;

    private String metricName;

    private String operator;

    private Double threshold;

    private Integer duration;

    private String notifyType;

    private List<String> notifyTargets;

    private String webhookUrl;

    private Boolean enabled;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
