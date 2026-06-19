package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_field_permission")
public class SysFieldPermission extends BaseEntity {
    private Long appId;
    private Long roleId;
    private Long modelId;
    private Long fieldId;
    private Integer canView;
    private Integer canEdit;
}
