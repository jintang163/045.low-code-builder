package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_business_logic")
public class BusinessLogic extends BaseEntity {

    private Long appId;
    private String logicName;
    private String logicCode;
    private String logicType;
    private String description;
    private String triggerType;
    private String triggerConfig;
    private String logicGraph;
    private String status;
    private String version;
    private String generatedCode;
    private String deployedPath;

    @TableField(exist = false)
    private List<LogicNode> nodes;

    @TableField(exist = false)
    private List<LogicEdge> edges;
}
