package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_rpa_execution")
public class RpaExecution extends BaseEntity {

    private Long scriptId;

    private String executionNo;

    private String triggerType;

    private Long triggerLogicId;

    private String triggerNodeId;

    private String inputParams;

    private String outputResult;

    private String status;

    private String errorMessage;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long duration;

    private String executionLog;
}
