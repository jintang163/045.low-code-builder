package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_logic_node")
public class LogicNode extends BaseEntity {

    private Long logicId;
    private String nodeId;
    private String nodeName;
    private String nodeType;
    private String nodeCategory;
    private String nodeConfig;
    private Integer positionX;
    private Integer positionY;
    private Integer width;
    private Integer height;
}
