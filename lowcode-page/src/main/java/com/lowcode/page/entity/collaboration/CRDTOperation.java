package com.lowcode.page.entity.collaboration;

import lombok.Data;

@Data
public class CRDTOperation {

    private String id;

    private Long userId;

    private String username;

    private String type;

    private String targetType;

    private String targetId;

    private String parentId;

    private Integer position;

    private String beforeId;

    private String data;

    private String oldData;

    private long timestamp;

    private int lamportClock;

    private String sessionId;

}
