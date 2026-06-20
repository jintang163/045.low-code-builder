package com.lowcode.deploy.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Fastjson2TypeHandler;
import com.lowcode.common.entity.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sys_deploy_task", autoResultMap = true)
@ApiModel(description = "部署任务")
public class DeployTask extends BaseEntity {

    @ApiModelProperty(value = "任务ID(UUID)")
    private String taskId;

    @ApiModelProperty(value = "部署名称")
    private String deployName;

    @ApiModelProperty(value = "服务ID")
    private Long serviceId;

    @ApiModelProperty(value = "云配置ID")
    private Long cloudConfigId;

    @ApiModelProperty(value = "版本号", example = "v1.0.0")
    private String version;

    @ApiModelProperty(value = "部署状态")
    private DeployStatus status;

    @ApiModelProperty(value = "进度(0-100)", example = "50")
    private Integer progress;

    @ApiModelProperty(value = "当前步骤")
    private String currentStep;

    @ApiModelProperty(value = "错误信息")
    private String errorMessage;

    @ApiModelProperty(value = "部署规格")
    @TableField(typeHandler = Fastjson2TypeHandler.class)
    private DeploySpec spec;

    @ApiModelProperty(value = "域名")
    private String domain;

    @ApiModelProperty(value = "开始时间")
    private Date startedAt;

    @ApiModelProperty(value = "结束时间")
    private Date finishedAt;

    @ApiModelProperty(value = "回滚来源任务ID")
    private String rollbackFromTaskId;
}
