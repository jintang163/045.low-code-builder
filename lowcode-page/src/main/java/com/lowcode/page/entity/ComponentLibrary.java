package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_component_library")
public class ComponentLibrary extends BaseEntity {

    private String componentType;
    private String componentName;
    private String componentCategory;
    private String icon;
    private String description;
    private String defaultProps;
    private String defaultStyle;
    private String propSchema;
    private Integer isCustom;
    private String componentCode;
    private Integer status;
}
