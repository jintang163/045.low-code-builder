package com.lowcode.model.expression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class ExpressionValidator {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("\\b([A-Z_]+)\\s*\\(");
    private static final Pattern VALID_VARIABLE_INNER = Pattern.compile("^[a-zA-Z0-9_.]+$");

    private static final Set<String> VALID_OPERATORS = new HashSet<>(Arrays.asList(
            "+", "-", "*", "/", "%", "==", "!=", ">", "<", ">=", "<=", "&&", "||", "!"
    ));

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "eval", "Function", "process", "require", "import", "load", "Java",
            "java", "Packages", "getClass", "forName", "newInstance", "exit",
            "exec", "Runtime", "Thread", "System", "javax", "sun", "com.sun",
            "org.apache", "java.lang.reflect", "invoke"
    ));

    private static final Set<String> ALL_FUNCTION_NAMES = new HashSet<>(ExpressionFunction.getAllFunctionNames());

    public ValidationResult validate(String expression) {
        ValidationResult result = new ValidationResult();

        if (expression == null || expression.trim().isEmpty()) {
            result.addError("表达式不能为空");
            return result;
        }

        validateBrackets(expression, result);
        validateQuotes(expression, result);
        validateVariables(expression, result);
        validateFunctions(expression, result);
        validateOperators(expression, result);
        validateSecurity(expression, result);

        return result;
    }

    private void validateBrackets(String expression, ValidationResult result) {
        int parenCount = 0;
        int bracketCount = 0;
        int braceCount = 0;

        for (char c : expression.toCharArray()) {
            switch (c) {
                case '(':
                    parenCount++;
                    break;
                case ')':
                    parenCount--;
                    break;
                case '[':
                    bracketCount++;
                    break;
                case ']':
                    bracketCount--;
                    break;
                case '{':
                    braceCount++;
                    break;
                case '}':
                    braceCount--;
                    break;
            }
            if (parenCount < 0) {
                result.addError("多余的右括号 ')'");
                return;
            }
            if (bracketCount < 0) {
                result.addError("多余的右中括号 ']'");
                return;
            }
        }

        if (parenCount > 0) {
            result.addError("缺少右括号 ')'");
        }
        if (parenCount < 0) {
            result.addError("多余的右括号 ')'");
        }
        if (bracketCount > 0) {
            result.addError("缺少右中括号 ']'");
        }
        if (bracketCount < 0) {
            result.addError("多余的右中括号 ']'");
        }
    }

    private void validateQuotes(String expression, ValidationResult result) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\\' && i + 1 < expression.length()) {
                i++;
                continue;
            }
            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            }
        }

        if (inSingleQuote) {
            result.addError("单引号未闭合");
        }
        if (inDoubleQuote) {
            result.addError("双引号未闭合");
        }
    }

    private void validateVariables(String expression, ValidationResult result) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        while (matcher.find()) {
            String varContent = matcher.group(1).trim();
            if (varContent.isEmpty()) {
                result.addError("变量引用不能为空: ${}");
                continue;
            }
            if (!VALID_VARIABLE_INNER.matcher(varContent).matches()) {
                result.addError("变量引用格式不合法: ${" + varContent + "}，仅允许字母、数字、下划线和点号");
            }
            String[] parts = varContent.split("\\.");
            if (parts.length > 3) {
                result.addWarning("变量引用层级较深: ${" + varContent + "}，支持格式: ${fieldName} 或 ${dataSourceId.tableName.fieldName}");
            }
        }
    }

    private void validateFunctions(String expression, ValidationResult result) {
        Matcher matcher = FUNCTION_PATTERN.matcher(expression);
        while (matcher.find()) {
            String funcName = matcher.group(1);
            if (!ALL_FUNCTION_NAMES.contains(funcName)) {
                result.addError("未知函数: " + funcName);
            }
        }
    }

    private void validateOperators(String expression, ValidationResult result) {
        String cleaned = removeStrings(expression);
        String opPattern = "[^\\s\\w.()${},]+";
        Pattern pattern = Pattern.compile(opPattern);
        Matcher matcher = pattern.matcher(cleaned);

        while (matcher.find()) {
            String op = matcher.group().trim();
            if (op.isEmpty()) {
                continue;
            }
            if (!VALID_OPERATORS.contains(op)) {
                result.addWarning("可能存在非法运算符: " + op);
            }
        }
    }

    private void validateSecurity(String expression, ValidationResult result) {
        String lowerExpr = expression.toLowerCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (lowerExpr.contains(keyword.toLowerCase())) {
                result.addError("表达式包含危险关键字: " + keyword);
            }
        }

        if (expression.contains("__proto__")) {
            result.addError("表达式包含危险属性: __proto__");
        }
        if (expression.contains("constructor")) {
            result.addError("表达式包含危险属性: constructor");
        }
        if (expression.contains("prototype")) {
            result.addError("表达式包含危险属性: prototype");
        }
    }

    private String removeStrings(String expression) {
        return expression.replaceAll("'[^']*'", "''").replaceAll("\"[^\"]*\"", "\"\"");
    }
}
