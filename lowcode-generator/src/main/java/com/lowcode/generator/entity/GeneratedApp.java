package com.lowcode.generator.entity;

import lombok.Data;

import java.util.List;

@Data
public class GeneratedApp {
    private String appName;
    private String appCode;
    private String version;
    private String downloadUrl;
    private Long fileSize;
    private List<GeneratedCode> backendCodes;
    private List<GeneratedCode> frontendCodes;
    private List<GeneratedCode> configFiles;
}
