package com.lowcode.generator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("sys_app_install")
public class AppInstall implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private String templateVersion;

    private Long appId;

    private Long userId;

    private LocalDateTime installTime;

    private LocalDateTime lastUpdateTime;

    private String currentVersion;

    private String latestVersion;

    private Integer hasUpdate;

    private String updateDiff;

    @TableLogic
    private Integer deleted;
}
