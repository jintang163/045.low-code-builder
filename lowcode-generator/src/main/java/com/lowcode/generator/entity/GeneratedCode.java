package com.lowcode.generator.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class GeneratedCode implements Serializable {

    private String codeType;
    private String fileName;
    private String filePath;
    private String codeContent;
    private String language;

    public GeneratedCode() {
    }

    public GeneratedCode(String codeType, String fileName, String filePath, String codeContent) {
        this.codeType = codeType;
        this.fileName = fileName;
        this.filePath = filePath;
        this.codeContent = codeContent;
        this.language = "Java";
    }
}
