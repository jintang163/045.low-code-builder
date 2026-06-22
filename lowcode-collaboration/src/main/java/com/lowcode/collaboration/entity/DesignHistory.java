package com.lowcode.collaboration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_design_history")
public class DesignHistory extends BaseEntity {

    private Long appId;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String operationType;

    private String operationDesc;

    private String beforeSnapshot;

    private String afterSnapshot;

    private String diffJson;

    private String ipAddress;

    @lombok.experimental.Accessors(chain = true)
    private String createdByName;

    private String createdByAvatar;
}
