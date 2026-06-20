package com.lowcode.deploy.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "部署进度事件")
public class DeployProgressEvent implements Serializable {

    @ApiModelProperty(value = "任务ID")
    private String taskId;

    @ApiModelProperty(value = "步骤")
    private String step;

    @ApiModelProperty(value = "消息")
    private String message;

    @ApiModelProperty(value = "时间戳")
    private Date timestamp;

    @ApiModelProperty(value = "进度(0-100)", example = "50")
    private Integer progress;

    @ApiModelProperty(value = "状态")
    private String status;

    @ApiModelProperty(value = "日志级别: INFO/WARN/ERROR", example = "INFO")
    private String logLevel;
}
