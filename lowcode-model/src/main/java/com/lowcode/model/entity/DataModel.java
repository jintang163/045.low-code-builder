package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_model")
public class DataModel extends BaseEntity {

    private Long appId;
    private Long dataSourceId;
    private String modelName;
    private String tableName;
    private String entityName;
    private String modelDesc;
    private String primaryKeyStrategy;
    private String logicDeleteField;
    private String versionField;
    private String tenantField;
    private String tableCharset;
    private String tableEngine;
    private Integer positionX;
    private Integer positionY;
    private Integer status;

    @TableField(exist = false)
    private List<ModelField> fields;

    @TableField(exist = false)
    private List<ModelIndex> indexes;
}
