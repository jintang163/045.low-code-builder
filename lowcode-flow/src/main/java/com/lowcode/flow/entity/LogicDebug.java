package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_logic_debug")
public class LogicDebug extends BaseEntity {

    private Long logicId;
    private String debugSessionId;
    private String currentNodeId;
    private String variableSnapshot;
    private String executionLog;
    private String status;
    private Integer stepIndex;
    private String breakpoints;
}
