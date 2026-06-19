package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app_role")
public class SysAppRole extends BaseEntity {
    private Long appId;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String description;
    private Integer isDefault;
}
