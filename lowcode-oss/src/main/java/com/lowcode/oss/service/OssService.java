package com.lowcode.oss.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.oss.entity.OssFile;
import com.lowcode.oss.enums.StorageTypeEnum;
import com.lowcode.oss.mapper.OssFileMapper;
import com.lowcode.oss.storage.StorageService;
import com.lowcode.oss.storage.StorageServiceFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class OssService {

    @Autowired
    private OssFileMapper ossFileMapper;

    @Autowired
    private StorageServiceFactory storageServiceFactory;

    @Value("${oss.type:minio}")
    private String defaultStorageType;

    public IPage<OssFile> getFilePage(int pageNum, int pageSize, String fileName, String storageType) {
        Page<OssFile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OssFile> wrapper = new LambdaQueryWrapper<>();
        if (fileName != null && !fileName.isEmpty()) {
            wrapper.like(OssFile::getOriginalName, fileName);
        }
        if (storageType != null && !storageType.isEmpty()) {
            wrapper.eq(OssFile::getStorageType, storageType);
        }
        wrapper.orderByDesc(OssFile::getCreateTime);
        return ossFileMapper.selectPage(page, wrapper);
    }

    public OssFile getFileById(Long id) {
        return ossFileMapper.selectById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public OssFile upload(MultipartFile file, String path, String storageType, HttpServletRequest request) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "上传文件不能为空");
        }

        String actualStorageType = (storageType != null && !storageType.isEmpty()) ? storageType : defaultStorageType;
        StorageService storageService = storageServiceFactory.getStorageService(actualStorageType);

        String originalName = file.getOriginalFilename();
        String fileSuffix = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : "";
        String fileName = IdUtil.simpleUUID() + fileSuffix;

        String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fullPath = (path != null && !path.isEmpty() ? path : "default") + "/" + datePath;

        String md5 = DigestUtil.md5Hex(file.getInputStream());

        OssFile existFile = ossFileMapper.selectOne(
                new LambdaQueryWrapper<OssFile>().eq(OssFile::getMd5, md5)
        );
        if (existFile != null) {
            return existFile;
        }

        String storedPath = storageService.upload(file.getInputStream(), fileName, file.getContentType(), fullPath);
        String url = storageService.getFileUrl(storedPath);

        Long userId = request.getHeader("X-User-Id") != null ? Long.parseLong(request.getHeader("X-User-Id")) : null;
        String username = request.getHeader("X-Username");

        OssFile ossFile = new OssFile();
        ossFile.setFileName(fileName);
        ossFile.setOriginalName(originalName);
        ossFile.setFileSuffix(fileSuffix);
        ossFile.setFileSize(file.getSize());
        ossFile.setContentType(file.getContentType());
        ossFile.setStorageType(actualStorageType);
        ossFile.setBucketName(getBucketName(actualStorageType));
        ossFile.setFilePath(storedPath);
        ossFile.setUrl(url);
        ossFile.setMd5(md5);
        ossFile.setUploadUserId(userId);
        ossFile.setUploadUsername(username);
        ossFile.setStatus(0);

        ossFileMapper.insert(ossFile);
        return ossFile;
    }

    public void download(Long id, HttpServletResponse response) throws Exception {
        OssFile ossFile = ossFileMapper.selectById(id);
        if (ossFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        StorageService storageService = storageServiceFactory.getStorageService(ossFile.getStorageType());
        InputStream inputStream = storageService.download(ossFile.getFilePath());

        response.setContentType(ossFile.getContentType());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" +
                URLEncoder.encode(ossFile.getOriginalName(), "UTF-8"));
        response.setHeader("Content-Length", String.valueOf(ossFile.getFileSize()));

        OutputStream outputStream = response.getOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, len);
        }
        outputStream.flush();
        inputStream.close();
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) throws Exception {
        OssFile ossFile = ossFileMapper.selectById(id);
        if (ossFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }

        StorageService storageService = storageServiceFactory.getStorageService(ossFile.getStorageType());
        try {
            storageService.delete(ossFile.getFilePath());
        } catch (Exception e) {
            log.warn("删除OSS文件失败: {}", e.getMessage());
        }

        ossFileMapper.deleteById(id);
    }

    public String getFileUrl(Long id) {
        OssFile ossFile = ossFileMapper.selectById(id);
        if (ossFile == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        StorageService storageService = storageServiceFactory.getStorageService(ossFile.getStorageType());
        return storageService.getFileUrl(ossFile.getFilePath());
    }

    private String getBucketName(String storageType) {
        StorageTypeEnum typeEnum = StorageTypeEnum.valueOf(storageType.toUpperCase());
        switch (typeEnum) {
            case ALIYUN:
                return "lowcode";
            case TENCENT:
                return "lowcode-1250000000";
            case MINIO:
            default:
                return "lowcode";
        }
    }
}
