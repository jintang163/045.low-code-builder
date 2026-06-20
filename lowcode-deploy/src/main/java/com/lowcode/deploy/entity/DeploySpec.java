package com.lowcode.deploy.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "部署规格")
public class DeploySpec implements Serializable {

    @ApiModelProperty(value = "实例数", example = "2")
    private Integer replicas;

    @ApiModelProperty(value = "CPU请求", example = "500m")
    private String cpuRequest;

    @ApiModelProperty(value = "内存请求", example = "512Mi")
    private String memoryRequest;

    @ApiModelProperty(value = "CPU限制", example = "1000m")
    private String cpuLimit;

    @ApiModelProperty(value = "内存限制", example = "1Gi")
    private String memoryLimit;

    @ApiModelProperty(value = "是否启用HPA", example = "true")
    private Boolean enableHpa;

    @ApiModelProperty(value = "最小实例数(HPA)", example = "2")
    private Integer minReplicas;

    @ApiModelProperty(value = "最大实例数(HPA)", example = "10")
    private Integer maxReplicas;

    @ApiModelProperty(value = "CPU使用率阈值%", example = "80")
    private Integer cpuThreshold;

    @ApiModelProperty(value = "发布策略: RollingUpdate/Recreate", example = "RollingUpdate")
    private String rolloutStrategy;

    @ApiModelProperty(value = "最大激增数", example = "25%")
    private String maxSurge;

    @ApiModelProperty(value = "最大不可用数", example = "25%")
    private String maxUnavailable;

    @ApiModelProperty(value = "节点选择器")
    private Map<String, String> nodeSelector;

    @ApiModelProperty(value = "环境变量")
    private Map<String, String> envVars;
}
