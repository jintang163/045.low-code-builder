package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_row_permission")
public class SysRowPermission extends BaseEntity {
    private Long appId;
    private Long roleId;
    private Long modelId;
    private String permissionName;
    private String permissionCode;
    private String expression;
    private String conditionType;
    private Integer priority;
    private Integer status;
    private String remark;
}
