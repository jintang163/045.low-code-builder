package com.lowcode.flow.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
@ApiModel(description = "跨应用调用请求")
public class CrossAppCallDTO {

    @NotBlank(message = "目标应用编码不能为空")
    @ApiModelProperty(value = "目标应用编码", example = "crm-app", required = true)
    private String targetAppCode;

    @ApiModelProperty(value = "目标接口类型：API/EVENT", example = "API", required = true)
    private String callType;

    @NotBlank(message = "接口/事件编码不能为空")
    @ApiModelProperty(value = "目标接口或事件编码", example = "crm.customer.create", required = true)
    private String targetCode;

    @ApiModelProperty(value = "请求参数", example = "{\"customerName\":\"张三\"}")
    private Map<String, Object> params;

    @ApiModelProperty(value = "超时时间(毫秒)，默认5000", example = "5000")
    private Integer timeoutMs;

    @ApiModelProperty(value = "调用方应用ID（系统自动填充）")
    private Long callerAppId;

    @ApiModelProperty(value = "调用方逻辑ID（系统自动填充）")
    private Long callerLogicId;
}
