package com.lowcode.page.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;

@Data
public class CustomComponentUploadDTO {

    @NotBlank(message = "组件类型不能为空")
    private String componentType;

    @NotBlank(message = "组件名称不能为空")
    private String componentName;

    @NotBlank(message = "组件分类不能为空")
    private String componentCategory;

    private String icon;

    private String description;

    private String author;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String changeLog;

    private MultipartFile file;
}
