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
@TableName("sys_app_service")
@ApiModel(description = "应用服务")
public class AppService extends BaseEntity {

    @ApiModelProperty(value = "服务名", example = "lowcode-auth")
    private String serviceName;

    @ApiModelProperty(value = "显示名称", example = "认证服务")
    private String displayName;

    @ApiModelProperty(value = "Maven模块路径", example = "lowcode-auth")
    private String modulePath;

    @ApiModelProperty(value = "镜像名称", example = "lowcode/lowcode-auth")
    private String imageName;

    @ApiModelProperty(value = "镜像标签", example = "latest")
    private String imageTag;

    @ApiModelProperty(value = "Dockerfile路径", example = "docker/lowcode-auth/Dockerfile")
    private String dockerfilePath;

    @ApiModelProperty(value = "Jar包路径", example = "target/lowcode-auth.jar")
    private String jarPath;
}
