package com.lowcode.deploy.controller.dto;

import com.lowcode.deploy.entity.DeploySpec;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "部署请求参数")
public class DeployRequest implements Serializable {

    @ApiModelProperty(value = "服务名", example = "lowcode-auth", required = true)
    private String serviceName;

    @ApiModelProperty(value = "显示名称", example = "认证服务")
    private String displayName;

    @ApiModelProperty(value = "Maven模块路径", example = "lowcode-auth")
    private String modulePath;

    @ApiModelProperty(value = "云配置ID", example = "1", required = true)
    private Long cloudConfigId;

    @ApiModelProperty(value = "版本号", example = "v1.0.0")
    private String version;

    @ApiModelProperty(value = "域名", example = "auth.example.com")
    private String domain;

    @ApiModelProperty(value = "部署规格")
    private DeploySpec spec;
}
