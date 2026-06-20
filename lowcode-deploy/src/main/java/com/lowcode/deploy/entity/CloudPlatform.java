package com.lowcode.deploy.entity;

import lombok.Getter;

@Getter
public enum CloudPlatform {

    ALIYUN("aliyun", "阿里云"),
    TENCENT("tencent", "腾讯云"),
    CUSTOM("custom", "自定义");

    private final String code;
    private final String name;

    CloudPlatform(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static CloudPlatform getByCode(String code) {
        for (CloudPlatform platform : values()) {
            if (platform.getCode().equals(code)) {
                return platform;
            }
        }
        return CUSTOM;
    }
}
