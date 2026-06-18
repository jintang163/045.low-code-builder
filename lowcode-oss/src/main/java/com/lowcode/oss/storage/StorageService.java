package com.lowcode.oss.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface StorageService {

    String upload(MultipartFile file, String path) throws Exception;

    String upload(InputStream inputStream, String fileName, String contentType, String path) throws Exception;

    InputStream download(String path) throws Exception;

    void delete(String path) throws Exception;

    String getFileUrl(String path);

    boolean exists(String path) throws Exception;
}
