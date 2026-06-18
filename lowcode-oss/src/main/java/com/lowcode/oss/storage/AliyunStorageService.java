package com.lowcode.oss.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service("aliyunStorageService")
public class AliyunStorageService implements StorageService {

    @Value("${oss.aliyun.endpoint}")
    private String endpoint;

    @Value("${oss.aliyun.accessKeyId}")
    private String accessKeyId;

    @Value("${oss.aliyun.accessKeySecret}")
    private String accessKeySecret;

    @Value("${oss.aliyun.bucketName}")
    private String bucketName;

    private OSS ossClient;

    @PostConstruct
    public void init() {
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            log.info("阿里云OSS初始化成功，Bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("阿里云OSS初始化失败", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String path) throws Exception {
        String fileName = path + "/" + file.getOriginalFilename();
        ossClient.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream()));
        return fileName;
    }

    @Override
    public String upload(InputStream inputStream, String fileName, String contentType, String path) throws Exception {
        String fullPath = path + "/" + fileName;
        ossClient.putObject(new PutObjectRequest(bucketName, fullPath, inputStream));
        return fullPath;
    }

    @Override
    public InputStream download(String path) throws Exception {
        OSSObject ossObject = ossClient.getObject(bucketName, path);
        return ossObject.getObjectContent();
    }

    @Override
    public void delete(String path) throws Exception {
        ossClient.deleteObject(bucketName, path);
    }

    @Override
    public String getFileUrl(String path) {
        Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000);
        URL url = ossClient.generatePresignedUrl(bucketName, path, expiration);
        return url.toString();
    }

    @Override
    public boolean exists(String path) throws Exception {
        return ossClient.doesObjectExist(bucketName, path);
    }
}
