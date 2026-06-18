package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_logic_edge")
public class LogicEdge extends BaseEntity {

    private Long logicId;
    private String edgeId;
    private String sourceNodeId;
    private String targetNodeId;
    private String sourcePort;
    private String targetPort;
    private String edgeConfig;
    private String conditionExpression;
}
