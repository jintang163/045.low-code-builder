package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_app_role")
public class SysUserAppRole extends BaseEntity {
    private Long userId;
    private Long appId;
    private Long roleId;
}
