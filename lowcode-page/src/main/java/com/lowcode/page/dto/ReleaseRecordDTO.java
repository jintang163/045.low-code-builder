package com.lowcode.page.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReleaseRecordDTO {

    private Long appId;

    private String resourceType;

    private Long resourceId;

    private Long snapshotId;

    private String version;

    private String releaseTitle;

    private String releaseNote;

    private Integer releaseType;

    private Integer grayType;

    private Integer grayPercent;

    private String grayUserGroup;

    private String grayUserIds;

    private String targetEnvironment;

    private LocalDateTime scheduledTime;

    private String releaseConfig;
}
