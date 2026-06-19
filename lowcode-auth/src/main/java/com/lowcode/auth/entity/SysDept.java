package com.lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {
    private Long parentId;
    private String deptName;
    private String deptCode;
    private String deptType;
    private Integer sortOrder;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
