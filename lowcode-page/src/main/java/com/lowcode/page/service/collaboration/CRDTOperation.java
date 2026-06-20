package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;

@Data
public class CRDTOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String userId;

    private String username;

    private String type;

    private String targetType;

    private String targetId;

    private String parentId;

    private Integer position;

    private Object data;

    private Object oldData;

    private long timestamp;

    private int lamportClock;

    private String sessionId;

    public CRDTOperation() {
        this.timestamp = System.currentTimeMillis();
    }
}
