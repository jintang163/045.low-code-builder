package com.lowcode.auth.dto;

import lombok.Data;

@Data
public class RowPermissionDTO {
    private Long appId;
    private Long roleId;
    private Long modelId;
    private String permissionName;
    private String permissionCode;
    private String expression;
    private String conditionType;
    private Integer priority;
    private Integer status;
    private String remark;
}
