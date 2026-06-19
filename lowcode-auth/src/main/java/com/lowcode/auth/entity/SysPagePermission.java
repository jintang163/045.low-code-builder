package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_page_permission")
public class SysPagePermission extends BaseEntity {
    private Long appId;
    private Long roleId;
    private Long pageId;
    private Integer canAccess;
}
