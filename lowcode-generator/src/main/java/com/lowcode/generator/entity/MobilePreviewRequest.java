package com.lowcode.generator.entity;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class MobilePreviewRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "应用ID不能为空")
    private String appId;

    private String pageId;

    @NotBlank(message = "平台不能为空")
    private String platform;
}
