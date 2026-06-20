package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_gray_release_config")
public class GrayReleaseConfig extends BaseEntity {

    private Long appId;

    private String resourceType;

    private Long resourceId;

    private Long releaseRecordId;

    private Long newSnapshotId;

    private Long oldSnapshotId;

    private String newVersion;

    private String oldVersion;

    private Integer grayType;

    private Integer grayPercent;

    private String grayUserGroup;

    private String grayUserIds;

    private String whiteListUserIds;

    private String blackListUserIds;

    private String hashField;

    private Integer status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String ruleConfig;
}
