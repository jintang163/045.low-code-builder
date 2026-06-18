package com.lowcode.page.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CustomComponentVersionUpdateDTO {

    @NotNull(message = "组件ID不能为空")
    private Long componentId;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String changeLog;

    private MultipartFile file;
}
