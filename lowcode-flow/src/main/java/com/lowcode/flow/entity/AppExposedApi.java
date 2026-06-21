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
@TableName("sys_app_exposed_api")
@ApiModel(description = "应用暴露的API接口")
public class AppExposedApi implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @ApiModelProperty(value = "所属应用ID", example = "1")
    private Long appId;

    @ApiModelProperty(value = "所属应用编码", example = "crm-app")
    private String appCode;

    @ApiModelProperty(value = "接口名称", example = "创建客户")
    private String apiName;

    @ApiModelProperty(value = "接口编码（唯一）", example = "crm.customer.create")
    private String apiCode;

    @ApiModelProperty(value = "接口类型：HTTP/INNER", example = "HTTP")
    private String apiType;

    @ApiModelProperty(value = "HTTP方法：GET/POST/PUT/DELETE", example = "POST")
    private String httpMethod;

    @ApiModelProperty(value = "接口路径（HTTP类型需要", example = "/api/customer/create")
    private String apiPath;

    @ApiModelProperty(value = "请求参数定义（JSON Schema）", example = "{\"customerName\":\"string\"}")
    private String requestSchema;

    @ApiModelProperty(value = "响应参数定义（JSON Schema）", example = "{\"id\":\"long\"}")
    private String responseSchema;

    @ApiModelProperty(value = "接口描述", example = "创建新客户")
    private String description;

    @ApiModelProperty(value = "鉴权方式：NONE/TOKEN/APP_KEY", example = "TOKEN")
    private String authType;

    @ApiModelProperty(value = "是否需要事务", example = "false")
    private Integer isTransactional;

    @ApiModelProperty(value = "超时时间(毫秒)", example = "5000")
    private Integer timeoutMs;

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
