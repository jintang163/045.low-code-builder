package com.lowcode.model.expression;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult implements Serializable {

    private boolean valid;
    private List<String> errors;
    private List<String> warnings;

    public ValidationResult() {
        this.valid = true;
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    public void addError(String error) {
        this.valid = false;
        this.errors.add(error);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public static ValidationResult ok() {
        return new ValidationResult();
    }

    public static ValidationResult error(String error) {
        ValidationResult result = new ValidationResult();
        result.addError(error);
        return result;
    }
}
