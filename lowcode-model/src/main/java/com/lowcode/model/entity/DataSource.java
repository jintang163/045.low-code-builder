package com.lowcode.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_data_source")
public class DataSource extends BaseEntity {

    private Long appId;
    private String sourceName;
    private String sourceCode;
    private String sourceType;
    private String dbType;
    private String host;
    private Integer port;
    private String dbName;
    private String username;
    private String password;
    private String driverClass;
    private String connectionParams;
    private Integer initialSize;
    private Integer minIdle;
    private Integer maxActive;
    private Integer maxWait;
    private Integer timeBetweenEvictionRunsMillis;
    private Integer minEvictableIdleTimeMillis;
    private Integer maxLifetime;
    private Integer connectionTimeout;
    private String validationQuery;
    private Boolean testWhileIdle;
    private Boolean testOnBorrow;
    private Boolean testOnReturn;
    private String restApiUrl;
    private String restApiMethod;
    private String restApiHeaders;
    private String restApiBody;
    private String restApiAuthType;
    private String restApiAuthToken;
    private Integer connectTimeout;
    private Integer readTimeout;
    private Integer status;
    private LocalDateTime lastHealthCheckTime;
    private String healthCheckStatus;
}
