package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_workflow_definition")
public class WorkflowDefinition extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long appId;

    private String processKey;

    private String processName;

    private String processDesc;

    private String bpmnXml;

    private String flowGraph;

    private String flowableDeploymentId;

    private String flowableProcessDefId;

    private Integer status;

    private Integer version;

    private LocalDateTime deployTime;

    private Long createdBy;

    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
