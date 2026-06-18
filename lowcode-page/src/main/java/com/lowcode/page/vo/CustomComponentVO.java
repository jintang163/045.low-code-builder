package com.lowcode.page.vo;

import com.lowcode.page.entity.CustomComponentVersion;
import lombok.Data;

import java.util.List;

@Data
public class CustomComponentVO {

    private Long id;
    private String componentType;
    private String componentName;
    private String componentCategory;
    private String icon;
    private String description;
    private String author;
    private String currentVersion;
    private Integer isSystem;
    private Integer status;
    private List<CustomComponentVersion> versions;
    private CustomComponentVersion currentVersionInfo;
    private String packageUrl;
}
