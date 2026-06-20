package com.lowcode.page.dto;

import lombok.Data;

@Data
public class VersionDiffDTO {

    private Long oldSnapshotId;

    private Long newSnapshotId;

    private String resourceType;
}
