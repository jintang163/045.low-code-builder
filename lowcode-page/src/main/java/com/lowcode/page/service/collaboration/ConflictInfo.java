package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class ConflictInfo implements Serializable {

    private static final long serialVersionUID = 1L;

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

    private String resolvedBy;

    public ConflictInfo() {
        this.createTime = new Date();
        this.status = "PENDING";
    }
}
