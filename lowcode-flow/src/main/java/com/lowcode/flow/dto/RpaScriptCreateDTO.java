package com.lowcode.flow.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RpaScriptCreateDTO {
    private Long appId;
    private String scriptName;
    private String scriptCode;
    private String description;
    private String scriptContent;
    private String scriptType;
    private String targetUrl;
    private Integer timeout;
}
