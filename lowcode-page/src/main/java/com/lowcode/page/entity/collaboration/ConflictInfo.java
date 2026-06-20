package com.lowcode.page.entity.collaboration;

import lombok.Data;

import java.util.Date;

@Data
public class ConflictInfo {

    private String conflictId;

    private Long pageId;

    private CRDTOperation operationA;

    private CRDTOperation operationB;

    private String conflictType;

    private String description;

    private String status;

    private String resolution;

    private Date createTime;

    private Date resolveTime;

    private Long resolvedBy;

}
