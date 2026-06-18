package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_custom_component")
public class CustomComponent extends BaseEntity {

    private String componentType;
    private String componentName;
    private String componentCategory;
    private String icon;
    private String description;
    private String author;
    private String currentVersion;
    private Integer isSystem;
    private Integer status;

    @TableField(exist = false)
    private List<CustomComponentVersion> versions;

    @TableField(exist = false)
    private CustomComponentVersion currentVersionInfo;
}
