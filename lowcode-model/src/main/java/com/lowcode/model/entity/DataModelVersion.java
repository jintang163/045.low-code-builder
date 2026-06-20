package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_version")
public class DataModelVersion extends BaseEntity {

    private Long modelId;
    private String version;
    private String snapshot;
    private Integer fieldCount;
    private Integer indexCount;
    private String changeDescription;
    private Integer changeType;

    @TableField(exist = false)
    private DataModel modelData;
}
