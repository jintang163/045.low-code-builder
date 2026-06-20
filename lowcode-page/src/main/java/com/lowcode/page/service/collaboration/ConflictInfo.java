package com.lowcode.page.service.collaboration;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ConflictInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String conflictId;

    private ConflictType type;

    private String componentId;

    private String propName;

    private List<CRDTOperation> conflictingOperations;

    private long timestamp;

    private ConflictStatus status;

    private String resolution;

    private String chosenUserId;

    private String resolvedBy;

    private long resolvedAt;

    public enum ConflictType {
        PROPERTY_CONFLICT,
        STRUCTURE_CONFLICT,
        DELETE_UPDATE_CONFLICT
    }

    public enum ConflictStatus {
        PENDING,
        RESOLVED,
        AUTO_RESOLVED
    }

    public ConflictInfo() {
        this.conflictingOperations = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
        this.status = ConflictStatus.PENDING;
    }

    public ConflictInfo(ConflictType type, String componentId) {
        this();
        this.type = type;
        this.componentId = componentId;
    }

    public void addConflictingOperation(CRDTOperation op) {
        if (this.conflictingOperations == null) {
            this.conflictingOperations = new ArrayList<>();
        }
        this.conflictingOperations.add(op);
    }
}
