package com.lowcode.common.enums;

import lombok.Getter;

@Getter
public enum DbTypeEnum {

    MYSQL("mysql", "MySQL", "com.mysql.cj.jdbc.Driver",
            "jdbc:mysql://{host}:{port}/{dbName}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"),
    ORACLE("oracle", "Oracle", "oracle.jdbc.OracleDriver",
            "jdbc:oracle:thin:@//{host}:{port}/{dbName}"),
    SQLSERVER("sqlserver", "SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver",
            "jdbc:sqlserver://{host}:{port};databaseName={dbName};encrypt=false"),
    POSTGRESQL("postgresql", "PostgreSQL", "org.postgresql.Driver",
            "jdbc:postgresql://{host}:{port}/{dbName}?useUnicode=true&characterEncoding=utf8"),
    DM("dm", "达梦", "dm.jdbc.driver.DmDriver",
            "jdbc:dm://{host}:{port}/{dbName}?useUnicode=true&characterEncoding=utf8"),
    REST_API("rest_api", "REST API", "", "");

    private final String code;
    private final String name;
    private final String driverClass;
    private final String urlPattern;

    DbTypeEnum(String code, String name, String driverClass, String urlPattern) {
        this.code = code;
        this.name = name;
        this.driverClass = driverClass;
        this.urlPattern = urlPattern;
    }

    public static DbTypeEnum getByCode(String code) {
        for (DbTypeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return MYSQL;
    }

    public String buildUrl(String host, Integer port, String dbName) {
        return urlPattern.replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{dbName}", dbName);
    }
}
