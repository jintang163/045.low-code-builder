package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_virtual_view")
public class VirtualView extends BaseEntity {

    private Long appId;
    private String viewName;
    private String viewCode;
    private String viewSql;
    private String viewConfig;
    private String joinConfig;
    private Integer status;
}
