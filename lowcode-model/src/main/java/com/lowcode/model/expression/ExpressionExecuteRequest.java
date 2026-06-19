package com.lowcode.model.expression;

import java.util.Map;

public class ExpressionExecuteRequest {

    private String expression;
    private Map<String, Object> context;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }
}
