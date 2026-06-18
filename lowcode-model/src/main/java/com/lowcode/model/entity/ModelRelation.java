package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_relation")
public class ModelRelation extends BaseEntity {

    private Long appId;
    private Long sourceModelId;
    private Long targetModelId;
    private String relationType;
    private Long sourceFieldId;
    private Long targetFieldId;
    private String foreignKeyName;
    private String onDelete;
    private String onUpdate;
}
