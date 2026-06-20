package com.lowcode.deploy.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_cloud_config")
@ApiModel(description = "云平台配置")
public class CloudConfig extends BaseEntity {

    @ApiModelProperty(value = "云平台类型", example = "ALIYUN")
    private CloudPlatform platform;

    @ApiModelProperty(value = "区域", example = "cn-hangzhou")
    private String region;

    @ApiModelProperty(value = "访问密钥ID")
    private String accessKey;

    @ApiModelProperty(value = "访问密钥Secret")
    private String accessSecret;

    @ApiModelProperty(value = "K8s集群ID")
    private String clusterId;

    @ApiModelProperty(value = "镜像仓库地址", example = "registry.cn-hangzhou.aliyuncs.com")
    private String registryUrl;

    @ApiModelProperty(value = "镜像仓库命名空间")
    private String registryNamespace;

    @ApiModelProperty(value = "镜像仓库用户名")
    private String registryUsername;

    @ApiModelProperty(value = "镜像仓库密码")
    private String registryPassword;

    @ApiModelProperty(value = "VPC ID")
    private String vpcId;

    @ApiModelProperty(value = "安全组ID")
    private String securityGroupId;

    @ApiModelProperty(value = "描述")
    private String description;
}
