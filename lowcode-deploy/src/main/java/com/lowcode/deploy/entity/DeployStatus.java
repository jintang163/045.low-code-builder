package com.lowcode.deploy.entity;

import lombok.Getter;

@Getter
public enum DeployStatus {

    PENDING("pending", "待部署"),
    BUILDING("building", "构建中"),
    PUSHING("pushing", "推送镜像中"),
    DEPLOYING("deploying", "部署中"),
    RUNNING("running", "运行中"),
    SUCCESS("success", "部署成功"),
    FAILED("failed", "部署失败"),
    ROLLING_BACK("rolling_back", "回滚中"),
    ROLLED_BACK("rolled_back", "已回滚");

    private final String code;
    private final String name;

    DeployStatus(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public static DeployStatus getByCode(String code) {
        for (DeployStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }
}
