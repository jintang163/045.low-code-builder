package com.lowcode.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "操作成功"),
    SYSTEM_ERROR(50000, "系统异常"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(40100, "未授权"),
    FORBIDDEN(40300, "禁止访问"),
    NOT_FOUND(40400, "资源不存在"),

    DATA_SOURCE_CONNECT_ERROR(10001, "数据源连接失败"),
    DATA_SOURCE_EXISTS(10002, "数据源已存在"),
    TABLE_EXISTS(10003, "表已存在"),
    MODEL_EXISTS(10004, "数据模型已存在"),
    SQL_EXECUTE_ERROR(10005, "SQL执行错误"),
    FIELD_NOT_EXISTS(10006, "字段不存在"),

    PAGE_EXISTS(20001, "页面已存在"),
    COMPONENT_EXISTS(20002, "组件已存在"),

    LOGIC_EXISTS(30001, "业务逻辑已存在"),
    WORKFLOW_EXISTS(30002, "工作流已存在"),
    WORKFLOW_DEPLOY_ERROR(30003, "工作流部署失败"),

    CODE_GENERATE_ERROR(40001, "代码生成失败"),
    CODE_DEPLOY_ERROR(40002, "代码部署失败"),

    UPLOAD_ERROR(50001, "文件上传失败"),
    DOWNLOAD_ERROR(50002, "文件下载失败"),

    USER_NOT_FOUND(60001, "用户不存在"),
    USER_DISABLED(60002, "用户已被禁用"),
    PASSWORD_ERROR(60003, "密码错误"),
    USERNAME_EXISTS(60004, "用户名已存在"),
    TOKEN_INVALID(60005, "Token无效或已过期"),
    TOKEN_EXPIRED(60006, "Token已过期");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
