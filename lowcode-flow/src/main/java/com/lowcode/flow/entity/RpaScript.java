package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_rpa_script")
public class RpaScript extends BaseEntity {

    private Long appId;

    private String scriptName;

    private String scriptCode;

    private String description;

    private String scriptContent;

    private String scriptType;

    private String targetUrl;

    private Integer timeout;

    private String status;

    private String version;

    private Integer scheduleEnabled;

    private String cronExpression;

    private String scheduleParams;

    private LocalDateTime lastExecuteTime;

    private LocalDateTime nextExecuteTime;

    private Long executeCount;

    private Long successCount;

    private Long failCount;
}
