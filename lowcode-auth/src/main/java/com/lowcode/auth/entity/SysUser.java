package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {
    private String username;
    private String password;
    private String salt;
    private String nickname;
    private String avatar;
    private String email;
    private String phone;
    private Integer status;
    private Integer userType;
    private Long deptId;
    private String remark;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
}
