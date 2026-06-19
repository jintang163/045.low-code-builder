package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SysPermission extends BaseEntity {
    private Long appId;
    private Long parentId;
    private String permissionName;
    private String permissionCode;
    private Integer permissionType;
    private String resourceType;
    private String resourceId;
    private String action;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private String remark;
}
