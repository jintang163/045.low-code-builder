package com.lowcode.page.vo;

import lombok.Data;

@Data
public class GrayReleaseResultVO {

    private Boolean shouldUseNewVersion;

    private String activeVersion;

    private Long activeSnapshotId;

    private String matchReason;

    private String matchedRule;

    private Integer grayPercent;

    private String hashValue;
}
