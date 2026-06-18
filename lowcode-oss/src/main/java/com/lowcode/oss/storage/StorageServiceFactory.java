package com.lowcode.oss.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageServiceFactory {

    @Value("${oss.type:minio}")
    private String storageType;

    @Autowired
    private MinioStorageService minioStorageService;

    @Autowired
    private AliyunStorageService aliyunStorageService;

    @Autowired
    private TencentStorageService tencentStorageService;

    public StorageService getStorageService() {
        return getStorageService(storageType);
    }

    public StorageService getStorageService(String type) {
        switch (type.toLowerCase()) {
            case "aliyun":
                return aliyunStorageService;
            case "tencent":
                return tencentStorageService;
            case "minio":
            default:
                return minioStorageService;
        }
    }
}
