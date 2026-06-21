package com.lowcode.flow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_app_exposed_event")
@ApiModel(description = "应用暴露的事件")
public class AppExposedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属应用ID", example = "1")
    private Long appId;

    @ApiModelProperty(value = "所属应用编码", example = "crm-app")
    private String appCode;

    @ApiModelProperty(value = "事件名称", example = "客户创建事件")
    private String eventName;

    @ApiModelProperty(value = "事件编码（唯一）", example = "crm.customer.created")
    private String eventCode;

    @ApiModelProperty(value = "事件载荷定义（JSON Schema）", example = "{\"customerId\":\"long\",\"customerName\":\"string\"}")
    private String payloadSchema;

    @ApiModelProperty(value = "事件描述", example = "当客户创建成功后触发")
    private String description;

    @ApiModelProperty(value = "状态：0-禁用 1-启用", example = "1")
    private Integer status;

    @ApiModelProperty(value = "创建人")
    private Long createdBy;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createdTime;

    @ApiModelProperty(value = "更新人")
    private Long updatedBy;

    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updatedTime;

    @TableLogic
    private Integer deleted;
}
