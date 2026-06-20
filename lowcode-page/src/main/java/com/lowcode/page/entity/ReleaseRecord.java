package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_release_record")
public class ReleaseRecord extends BaseEntity {

    private Long appId;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private Long snapshotId;

    private String version;

    private String releaseTitle;

    private String releaseNote;

    private Integer releaseType;

    private Integer releaseStatus;

    private Integer grayType;

    private Integer grayPercent;

    private String grayUserGroup;

    private String grayUserIds;

    private String targetEnvironment;

    private String gitCommitId;

    private String gitCommitMessage;

    private String gitBranch;

    private LocalDateTime scheduledTime;

    private LocalDateTime releaseTime;

    private LocalDateTime rollbackTime;

    private Long rollbackFromSnapshotId;

    private String rollbackReason;

    private Integer isRollback;

    private String releaseConfig;
}
