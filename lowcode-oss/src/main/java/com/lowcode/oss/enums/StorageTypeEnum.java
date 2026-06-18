package com.lowcode.oss.enums;

import lombok.Getter;

@Getter
public enum StorageTypeEnum {
    MINIO("minio", "MinIO"),
    ALIYUN("aliyun", "阿里云OSS"),
    TENCENT("tencent", "腾讯云COS");

    private final String code;
    private final String name;

    StorageTypeEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }
}
