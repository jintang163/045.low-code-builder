package com.lowcode.collaboration.feign;

import lombok.Data;

@Data
public class OssFileVO {
    private Long id;
    private String fileName;
    private String originalName;
    private String fileSuffix;
    private Long fileSize;
    private String contentType;
    private String storageType;
    private String bucketName;
    private String filePath;
    private String url;
    private String md5;
}
