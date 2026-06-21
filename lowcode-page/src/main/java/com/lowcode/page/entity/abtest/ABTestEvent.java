package com.lowcode.page.entity.abtest;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_ab_test_event")
public class ABTestEvent extends BaseEntity {

    private Long testId;

    private Long variantId;

    private String eventType;

    private String eventKey;

    private Long userId;

    private String sessionId;

    private String pageUrl;

    private Long componentId;

    private BigDecimal eventValue;

    private LocalDateTime timestamp;

    private String userAgent;

    private String ipAddress;
}
