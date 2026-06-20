package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class CRDTOperation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String operationId;

    private OperationType type;

    private String componentId;

    private String parentId;

    private Integer position;

    private Map<String, Object> props;

    private Map<String, Object> style;

    private Map<String, Object> events;

    private String propName;

    private Object propValue;

    private String componentType;

    private String componentName;

    private String userId;

    private String username;

    private long lamportClock;

    private long timestamp;

    private String baseVersion;

    public enum OperationType {
        INSERT,
        DELETE,
        UPDATE,
        MOVE,
        PROP_CHANGE
    }

    public CRDTOperation() {
        this.timestamp = System.currentTimeMillis();
    }

    public CRDTOperation(OperationType type, String componentId, String userId) {
        this.type = type;
        this.componentId = componentId;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
}
