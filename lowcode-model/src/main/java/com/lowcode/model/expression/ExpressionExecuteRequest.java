package com.lowcode.model.expression;

import lombok.Data;
import java.util.Map;

@Data
public class ExpressionExecuteRequest {
    private String expression;
    private Map<String, Object> context;
}
