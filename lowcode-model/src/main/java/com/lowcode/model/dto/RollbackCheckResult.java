package com.lowcode.model.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RollbackCheckResult {

    private boolean allowed;
    private List<String> warnings;
    private List<String> errors;

    private int deletedFieldCount;
    private int modifiedFieldCount;
    private int addedFieldCount;

    private boolean primaryKeyChanged;
    private boolean dataLossRisk;

    private VersionCompareResult compareResult;

    public RollbackCheckResult() {
        this.warnings = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.allowed = true;
    }

    public void addError(String error) {
        this.errors.add(error);
        this.allowed = false;
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }
}
