package com.lowcode.page.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lowcode.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_version_snapshot")
public class VersionSnapshot extends BaseEntity {

    private Long appId;

    private String resourceType;

    private Long resourceId;

    private String resourceName;

    private String version;

    private Integer snapshotType;

    private String snapshotData;

    private String pageSnapshot;

    private String dataModelSnapshot;

    private String logicSnapshot;

    private String description;

    private String gitCommitId;

    private String gitCommitMessage;

    private String gitBranch;

    private Integer isPublished;

    private String publishedVersion;

    private String tag;
}
