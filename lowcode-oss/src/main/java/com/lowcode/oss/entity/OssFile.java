package com.lowcode.oss.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_file")
public class OssFile extends BaseEntity {
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
    private Long uploadUserId;
    private String uploadUsername;
    private Integer status;
    private String remark;
}
