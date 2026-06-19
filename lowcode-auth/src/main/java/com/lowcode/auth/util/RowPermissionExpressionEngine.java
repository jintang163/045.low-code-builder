package com.lowcode.auth.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lowcode.auth.entity.SysRowPermission;
import com.lowcode.auth.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RowPermissionExpressionEngine {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([^{}]+)}");
    private static final Pattern USER_VAR_PATTERN = Pattern.compile("^user\\.(.+)$");
    private static final Pattern DATA_VAR_PATTERN = Pattern.compile("^data\\.(.+)$");

    private static final Set<String> VALID_OPERATORS = new HashSet<>(Arrays.asList(
            "=", "!=", ">", "<", ">=", "<=", "AND", "OR", "&&", "||",
            "IN", "NOT IN", "LIKE", "NOT LIKE", "IS NULL", "IS NOT NULL", "+", "-", "*", "/"
    ));

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "DROP", "DELETE", "UPDATE", "INSERT", "CREATE", "ALTER", "TRUNCATE",
            "EXEC", "EXECUTE", "UNION", "--", ";", "/*", "*/"
    ));

    public static class ExpressionParseResult {
        private boolean valid;
        private String errorMessage;
        private String sqlCondition;
        private List<String> userVariables;
        private List<String> dataVariables;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public String getSqlCondition() { return sqlCondition; }
        public void setSqlCondition(String sqlCondition) { this.sqlCondition = sqlCondition; }
        public List<String> getUserVariables() { return userVariables; }
        public void setUserVariables(List<String> userVariables) { this.userVariables = userVariables; }
        public List<String> getDataVariables() { return dataVariables; }
        public void setDataVariables(List<String> dataVariables) { this.dataVariables = dataVariables; }
    }

    public ExpressionParseResult parseExpression(String expression) {
        ExpressionParseResult result = new ExpressionParseResult();
        result.setUserVariables(new ArrayList<>());
        result.setDataVariables(new ArrayList<>());

        if (expression == null || expression.trim().isEmpty()) {
            result.setValid(false);
            result.setErrorMessage("表达式不能为空");
            return result;
        }

        String upperExpr = expression.toUpperCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (upperExpr.contains(keyword)) {
                result.setValid(false);
                result.setErrorMessage("表达式包含非法关键字: " + keyword);
                return result;
            }
        }

        List<String> userVars = new ArrayList<>();
        List<String> dataVars = new ArrayList<>();
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varContent = matcher.group(1).trim();
            Matcher userMatcher = USER_VAR_PATTERN.matcher(varContent);
            Matcher dataMatcher = DATA_VAR_PATTERN.matcher(varContent);

            if (userMatcher.matches()) {
                String field = userMatcher.group(1);
                userVars.add(field);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(":" + field.replace('.', '_')));
            } else if (dataMatcher.matches()) {
                String field = dataMatcher.group(1);
                dataVars.add(field);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(field));
            } else {
                result.setValid(false);
                result.setErrorMessage("无效的变量格式: {" + varContent + "}，必须使用 {user.xxx} 或 {data.xxx} 格式");
                return result;
            }
        }
        matcher.appendTail(sb);

        result.setValid(true);
        result.setSqlCondition(sb.toString());
        result.setUserVariables(userVars);
        result.setDataVariables(dataVars);
        return result;
    }

    public Map<String, Object> buildUserContext(SysUser user) {
        Map<String, Object> context = new HashMap<>();
        if (user == null) {
            return context;
        }

        context.put("id", user.getId());
        context.put("username", user.getUsername());
        context.put("nickname", user.getNickname());
        context.put("email", user.getEmail());
        context.put("phone", user.getPhone());
        context.put("deptId", user.getDeptId());
        context.put("userType", user.getUserType());

        if (user.getUserAttributes() != null && !user.getUserAttributes().isEmpty()) {
            try {
                JSONObject attrs = JSON.parseObject(user.getUserAttributes());
                for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                    context.put(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                log.warn("解析用户扩展属性失败: {}", e.getMessage());
            }
        }

        return context;
    }

    public String generateSqlFilter(List<SysRowPermission> permissions, SysUser user) {
        if (permissions == null || permissions.isEmpty()) {
            return null;
        }

        Map<String, Object> userContext = buildUserContext(user);
        List<String> andConditions = new ArrayList<>();
        List<String> orConditions = new ArrayList<>();

        for (SysRowPermission permission : permissions) {
            if (permission.getStatus() == null || permission.getStatus() != 1) {
                continue;
            }

            ExpressionParseResult parseResult = parseExpression(permission.getExpression());
            if (!parseResult.isValid()) {
                log.warn("跳过无效的行级权限表达式: {}", parseResult.getErrorMessage());
                continue;
            }

            String sql = parseResult.getSqlCondition();
            for (String var : parseResult.getUserVariables()) {
                Object value = userContext.get(var);
                String paramName = var.replace('.', '_');
                if (value == null) {
                    sql = sql.replace(":" + paramName, "NULL");
                } else if (value instanceof String) {
                    sql = sql.replace(":" + paramName, "'" + escapeSql((String) value) + "'");
                } else if (value instanceof Number) {
                    sql = sql.replace(":" + paramName, value.toString());
                } else {
                    sql = sql.replace(":" + paramName, "'" + escapeSql(value.toString()) + "'");
                }
            }

            if ("OR".equalsIgnoreCase(permission.getConditionType())) {
                orConditions.add("(" + sql + ")");
            } else {
                andConditions.add("(" + sql + ")");
            }
        }

        StringBuilder result = new StringBuilder();
        if (!andConditions.isEmpty()) {
            result.append(String.join(" AND ", andConditions));
        }
        if (!orConditions.isEmpty()) {
            if (result.length() > 0) {
                result.append(" AND ");
            }
            result.append("(").append(String.join(" OR ", orConditions)).append(")");
        }

        return result.length() > 0 ? result.toString() : null;
    }

    public boolean evaluate(String expression, SysUser user, Map<String, Object> data) {
        if (expression == null || expression.trim().isEmpty()) {
            return true;
        }

        ExpressionParseResult parseResult = parseExpression(expression);
        if (!parseResult.isValid()) {
            log.warn("行级权限表达式无效: {}", parseResult.getErrorMessage());
            return false;
        }

        Map<String, Object> userContext = buildUserContext(user);
        String evalExpr = expression;

        for (String var : parseResult.getUserVariables()) {
            Object value = userContext.get(var);
            String placeholder = "{user." + var + "}";
            evalExpr = evalExpr.replace(placeholder, toExpressionValue(value));
        }

        for (String var : parseResult.getDataVariables()) {
            Object value = data != null ? data.get(var) : null;
            String placeholder = "{data." + var + "}";
            evalExpr = evalExpr.replace(placeholder, toExpressionValue(value));
        }

        return evaluateSimpleExpression(evalExpr);
    }

    private String toExpressionValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return value.toString();
    }

    private boolean evaluateSimpleExpression(String expr) {
        try {
            expr = expr.trim();

            if (expr.equalsIgnoreCase("true") || expr.equals("1")) {
                return true;
            }
            if (expr.equalsIgnoreCase("false") || expr.equals("0") || expr.equalsIgnoreCase("null")) {
                return false;
            }

            if (expr.contains(" AND ")) {
                String[] parts = expr.split(" AND ");
                for (String part : parts) {
                    if (!evaluateSimpleExpression(part.trim())) {
                        return false;
                    }
                }
                return true;
            }

            if (expr.contains(" OR ")) {
                String[] parts = expr.split(" OR ");
                for (String part : parts) {
                    if (evaluateSimpleExpression(part.trim())) {
                        return true;
                    }
                }
                return false;
            }

            return evaluateComparison(expr);
        } catch (Exception e) {
            log.error("表达式求值失败: {} - {}", expr, e.getMessage());
            return false;
        }
    }

    private boolean evaluateComparison(String expr) {
        String[] operators = { "=", "!=", ">=", "<=", ">", "<" };

        for (String op : operators) {
            int idx = expr.indexOf(op);
            if (idx > 0) {
                String left = expr.substring(0, idx).trim();
                String right = expr.substring(idx + op.length()).trim();

                Object leftVal = parseValue(left);
                Object rightVal = parseValue(right);

                if (leftVal == null || rightVal == null) {
                    return "=".equals(op) ? leftVal == rightVal : "!=".equals(op);
                }

                if (leftVal instanceof Number && rightVal instanceof Number) {
                    double l = ((Number) leftVal).doubleValue();
                    double r = ((Number) rightVal).doubleValue();
                    switch (op) {
                        case "=": return l == r;
                        case "!=": return l != r;
                        case ">": return l > r;
                        case "<": return l < r;
                        case ">=": return l >= r;
                        case "<=": return l <= r;
                    }
                }

                String lStr = leftVal.toString();
                String rStr = rightVal.toString();
                switch (op) {
                    case "=": return lStr.equals(rStr);
                    case "!=": return !lStr.equals(rStr);
                    default: return lStr.compareTo(rStr) >= 0;
                }
            }
        }

        return Boolean.parseBoolean(expr);
    }

    private Object parseValue(String val) {
        val = val.trim();
        if (val.equalsIgnoreCase("null")) {
            return null;
        }
        if (val.startsWith("'") && val.endsWith("'") && val.length() >= 2) {
            return val.substring(1, val.length() - 1);
        }
        try {
            if (val.contains(".")) {
                return Double.parseDouble(val);
            }
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return val;
        }
    }

    private String escapeSql(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''").replace("\\", "\\\\");
    }

    public List<Map<String, Object>> filterData(List<Map<String, Object>> dataList,
                                               List<SysRowPermission> permissions,
                                               SysUser user) {
        if (dataList == null || dataList.isEmpty()) {
            return dataList;
        }
        if (permissions == null || permissions.isEmpty()) {
            return dataList;
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> data : dataList) {
            boolean hasPermission = false;
            boolean hasOrCondition = false;

            for (SysRowPermission permission : permissions) {
                if (permission.getStatus() == null || permission.getStatus() != 1) {
                    continue;
                }

                boolean match = evaluate(permission.getExpression(), user, data);

                if ("OR".equalsIgnoreCase(permission.getConditionType())) {
                    hasOrCondition = true;
                    if (match) {
                        hasPermission = true;
                        break;
                    }
                } else {
                    if (!match) {
                        hasPermission = false;
                        break;
                    }
                    hasPermission = true;
                }
            }

            if (hasOrCondition && hasPermission) {
                result.add(data);
            } else if (!hasOrCondition && hasPermission) {
                result.add(data);
            } else if (!hasOrCondition && permissions.stream().allMatch(p -> "OR".equalsIgnoreCase(p.getConditionType()))) {
                result.add(data);
            }
        }

        return result;
    }
}
