package com.lowcode.model.expression;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ExpressionBatchRequest {
    private List<Item> items;

    @Data
    public static class Item {
        private String id;
        private String expression;
        private Map<String, Object> context;
    }
}
