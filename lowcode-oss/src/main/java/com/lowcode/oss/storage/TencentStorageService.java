package com.lowcode.oss.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.*;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

@Slf4j
@Service("tencentStorageService")
public class TencentStorageService implements StorageService {

    @Value("${oss.tencent.region}")
    private String region;

    @Value("${oss.tencent.secretId}")
    private String secretId;

    @Value("${oss.tencent.secretKey}")
    private String secretKey;

    @Value("${oss.tencent.bucketName}")
    private String bucketName;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        try {
            COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
            ClientConfig clientConfig = new ClientConfig(new Region(region));
            cosClient = new COSClient(cred, clientConfig);
            log.info("腾讯云COS初始化成功，Bucket: {}", bucketName);
        } catch (Exception e) {
            log.error("腾讯云COS初始化失败", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String path) throws Exception {
        String fileName = path + "/" + file.getOriginalFilename();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());
        cosClient.putObject(new PutObjectRequest(bucketName, fileName, file.getInputStream(), metadata));
        return fileName;
    }

    @Override
    public String upload(InputStream inputStream, String fileName, String contentType, String path) throws Exception {
        String fullPath = path + "/" + fileName;
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(inputStream.available());
        metadata.setContentType(contentType);
        cosClient.putObject(new PutObjectRequest(bucketName, fullPath, inputStream, metadata));
        return fullPath;
    }

    @Override
    public InputStream download(String path) throws Exception {
        COSObject cosObject = cosClient.getObject(new GetObjectRequest(bucketName, path));
        return cosObject.getObjectContent();
    }

    @Override
    public void delete(String path) throws Exception {
        cosClient.deleteObject(bucketName, path);
    }

    @Override
    public String getFileUrl(String path) {
        Date expiration = new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000);
        URL url = cosClient.generatePresignedUrl(bucketName, path, expiration);
        return url.toString();
    }

    @Override
    public boolean exists(String path) throws Exception {
        return cosClient.doesObjectExist(bucketName, path);
    }
}
