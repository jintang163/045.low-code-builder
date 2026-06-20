package com.lowcode.generator.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class MobilePreview implements Serializable {

    private static final long serialVersionUID = 1L;

    private String previewId;

    private String appId;

    private String pageId;

    private String previewToken;

    private String previewUrl;

    private String qrCodeUrl;

    private String platform;

    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createdTime;
}
