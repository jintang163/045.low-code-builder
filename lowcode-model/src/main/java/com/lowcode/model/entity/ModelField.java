package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_field")
public class ModelField extends BaseEntity {

    private Long modelId;
    private String fieldName;
    private String columnName;
    private String fieldType;
    private String javaType;
    private String jdbcType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private String defaultValue;
    private String fieldComment;
    private Integer isPrimary;
    private Integer isRequired;
    private Integer isUnique;
    private Integer isIndex;
    private Integer isAutoIncrement;
    private Integer isLogicDelete;
    private Integer isVersion;
    private Integer isTenant;
    private String enumValues;
    private Long referenceModelId;
    private Long referenceFieldId;
    private String referenceType;
    private Integer sortOrder;
}
