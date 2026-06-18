package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_custom_component_version")
public class CustomComponentVersion extends BaseEntity {

    private Long componentId;
    private String version;
    private String changeLog;
    private String packagePath;
    private Long packageSize;
    private String mainFile;
    private String propSchema;
    private String eventSchema;
    private String exposedEvents;
    private String defaultProps;
    private String defaultStyle;
    private Integer isDeprecated;
    private Integer status;
}
