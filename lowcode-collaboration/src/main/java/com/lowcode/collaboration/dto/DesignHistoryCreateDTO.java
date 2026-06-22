package com.lowcode.collaboration.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class DesignHistoryCreateDTO {

    @NotNull(message = "应用ID不能为空")
    private Long appId;

    @NotBlank(message = "目标类型不能为空")
    private String targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    private String targetName;

    @NotBlank(message = "操作类型不能为空")
    private String operationType;

    private String operationDesc;

    private String beforeSnapshot;

    private String afterSnapshot;

    private String diffJson;
}
