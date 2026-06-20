package com.lowcode.generator.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneratedUniAppCode extends GeneratedCode {

    private String platform;

    public GeneratedUniAppCode() {
        super();
    }

    public GeneratedUniAppCode(String codeType, String fileName, String filePath, String codeContent, String platform) {
        super(codeType, fileName, filePath, codeContent);
        this.platform = platform;
        setLanguage("Vue");
    }
}
