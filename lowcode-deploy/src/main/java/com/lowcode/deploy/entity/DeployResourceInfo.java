package com.lowcode.deploy.entity;

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
@ApiModel(description = "部署资源信息")
public class DeployResourceInfo implements Serializable {

    @ApiModelProperty(value = "Deployment名称")
    private String deploymentName;

    @ApiModelProperty(value = "命名空间")
    private String namespace;

    @ApiModelProperty(value = "期望实例数", example = "3")
    private Integer replicas;

    @ApiModelProperty(value = "就绪实例数", example = "3")
    private Integer readyReplicas;

    @ApiModelProperty(value = "可用实例数", example = "3")
    private Integer availableReplicas;

    @ApiModelProperty(value = "Service名称")
    private String serviceName;

    @ApiModelProperty(value = "Service类型", example = "ClusterIP")
    private String serviceType;

    @ApiModelProperty(value = "Service端口", example = "8080")
    private Integer servicePort;

    @ApiModelProperty(value = "Ingress名称")
    private String ingressName;

    @ApiModelProperty(value = "访问域名")
    private String host;

    @ApiModelProperty(value = "HPA名称")
    private String hpaName;

    @ApiModelProperty(value = "当前CPU使用率%", example = "45")
    private Integer currentCpuUtilization;

    @ApiModelProperty(value = "目标CPU使用率%", example = "80")
    private Integer targetCpuUtilization;

    @ApiModelProperty(value = "HPA当前实例数", example = "3")
    private Integer currentReplicas;
}
