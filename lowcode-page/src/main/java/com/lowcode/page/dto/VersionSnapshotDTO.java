package com.lowcode.page.dto;

import lombok.Data;

@Data
public class VersionSnapshotDTO {

    private Long appId;

    private String resourceType;

    private Long resourceId;

    private String description;

    private String tag;

    private Boolean autoCreate;
}
