package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_workflow_definition")
public class WorkflowDefinition extends BaseEntity {

    private Long appId;
    private String workflowName;
    private String workflowCode;
    private String description;
    private String bpmnXml;
    private String flowableDefinitionId;
    private String flowableDeploymentId;
    private Integer version;
    private String status;
    private String category;
}
