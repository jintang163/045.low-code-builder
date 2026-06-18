package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_model_index")
public class ModelIndex extends BaseEntity {

    private Long modelId;
    private String indexName;
    private String indexType;
    private String indexFields;
    private String indexComment;
}
