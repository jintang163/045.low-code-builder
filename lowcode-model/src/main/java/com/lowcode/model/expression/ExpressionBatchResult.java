package com.lowcode.model.expression;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExpressionBatchResult {
    private List<ItemResult> results;

    @Data
    public static class ItemResult {
        private String id;
        private boolean success;
        private Object result;
        private String error;
        private long duration;
        private String resultType;
    }
}
