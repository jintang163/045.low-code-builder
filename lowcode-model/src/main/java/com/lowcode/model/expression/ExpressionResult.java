package com.lowcode.model.expression;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExpressionResult implements Serializable {

    private boolean success;
    private Object result;
    private String error;
    private long duration;
    private String resultType;

    public ExpressionResult() {
    }

    public ExpressionResult(boolean success, Object result, String error, long duration, String resultType) {
        this.success = success;
        this.result = result;
        this.error = error;
        this.duration = duration;
        this.resultType = resultType;
    }

    public static ExpressionResult success(Object result, long duration) {
        return new ExpressionResult(true, result, null, duration, detectType(result));
    }

    public static ExpressionResult failure(String error, long duration) {
        return new ExpressionResult(false, null, error, duration, null);
    }

    private static String detectType(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return "number";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        if (value instanceof String) {
            return "string";
        }
        if (value.getClass().isArray() || value instanceof java.util.List) {
            return "array";
        }
        if (value instanceof java.util.Map) {
            return "object";
        }
        return "object";
    }
}
