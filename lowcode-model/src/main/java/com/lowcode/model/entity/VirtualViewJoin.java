package com.lowcode.model.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class VirtualViewJoin implements Serializable {

    private Long leftDataSourceId;
    private String leftTable;
    private String leftAlias;
    private String leftColumn;
    private Long rightDataSourceId;
    private String rightTable;
    private String rightAlias;
    private String rightColumn;
    private String joinType;
}
