package com.lowcode.generator.entity;

import lombok.Data;

import java.util.List;

@Data
public class AppGenerateConfig {
    private String appName;
    private String appCode;
    private String appDesc;
    private String version;
    private String author;
    private String basePackage;
    private String moduleName;

    private List<Long> dataModelIds;
    private List<Long> pageIds;
    private List<Long> logicIds;
    private List<Long> workflowIds;

    private String dbType;
    private String dbHost;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;

    private String redisHost;
    private Integer redisPort;
    private String redisPassword;
    private Integer redisDatabase;

    private boolean generateDocker;
    private boolean generateReadme;
    private boolean includeFrontend;
    private boolean includeBackend;
}
