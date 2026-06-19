package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_field_mapping")
public class FieldMapping extends BaseEntity {

    private Long appId;
    private Long dataSourceId;
    private Long pageId;
    private Long componentId;
    private String sourceTable;
    private String sourceField;
    private String sourceType;
    private String targetComponent;
    private String targetComponentId;
    private String targetProperty;
    private String mappingType;
    private Integer sortOrder;
    private String description;
}
