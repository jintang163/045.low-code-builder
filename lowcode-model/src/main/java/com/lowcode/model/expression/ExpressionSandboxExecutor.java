package com.lowcode.model.expression;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.Value;
import org.springframework.stereotype.Component;

@Component
public class ExpressionSandboxExecutor {

    private static final long DEFAULT_TIMEOUT_MS = 5000;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<>(Arrays.asList(
            "eval", "Function", "process", "require", "import", "load",
            "Java", "Packages", "getClass", "forName", "newInstance",
            "exit", "exec", "Runtime", "Thread", "System"
    ));

    private static final String FUNCTION_DEFINITIONS =
            "var SUM = function() {" +
            "  var sum = 0;" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null) sum += Number(arguments[i]);" +
            "  }" +
            "  return sum;" +
            "};" +
            "var AVG = function() {" +
            "  var sum = 0; var count = 0;" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null) { sum += Number(arguments[i]); count++; }" +
            "  }" +
            "  return count > 0 ? sum / count : 0;" +
            "};" +
            "var COUNT = function() {" +
            "  var count = 0;" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null && arguments[i] !== '') count++;" +
            "  }" +
            "  return count;" +
            "};" +
            "var MAX = function() {" +
            "  var max = null;" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null && (max === null || arguments[i] > max)) max = arguments[i];" +
            "  }" +
            "  return max;" +
            "};" +
            "var MIN = function() {" +
            "  var min = null;" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null && (min === null || arguments[i] < min)) min = arguments[i];" +
            "  }" +
            "  return min;" +
            "};" +
            "var IF = function(cond, trueVal, falseVal) {" +
            "  return cond ? trueVal : falseVal;" +
            "};" +
            "var IIF = function(cond, trueVal, falseVal) {" +
            "  return cond ? trueVal : falseVal;" +
            "};" +
            "var CONTAINS = function(str, sub) {" +
            "  return str != null && sub != null && String(str).indexOf(String(sub)) >= 0;" +
            "};" +
            "var STARTSWITH = function(str, prefix) {" +
            "  return str != null && prefix != null && String(str).indexOf(String(prefix)) === 0;" +
            "};" +
            "var ENDSWITH = function(str, suffix) {" +
            "  if (str == null || suffix == null) return false;" +
            "  var s = String(str); var sf = String(suffix);" +
            "  return s.indexOf(sf, s.length - sf.length) >= 0;" +
            "};" +
            "var CONCAT = function() {" +
            "  var result = '';" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] != null) result += String(arguments[i]);" +
            "  }" +
            "  return result;" +
            "};" +
            "var LENGTH = function(str) {" +
            "  return str != null ? String(str).length : 0;" +
            "};" +
            "var TRIM = function(str) {" +
            "  return str != null ? String(str).trim() : '';" +
            "};" +
            "var UPPER = function(str) {" +
            "  return str != null ? String(str).toUpperCase() : '';" +
            "};" +
            "var LOWER = function(str) {" +
            "  return str != null ? String(str).toLowerCase() : '';" +
            "};" +
            "var SUBSTRING = function(str, start, length) {" +
            "  if (str == null) return '';" +
            "  var s = String(str);" +
            "  if (length !== undefined) {" +
            "    return s.substr(start, length);" +
            "  }" +
            "  return s.substr(start);" +
            "};" +
            "var NOW = function() {" +
            "  return new Date().toISOString();" +
            "};" +
            "var FORMAT_DATE = function(date, pattern) {" +
            "  if (date == null) return '';" +
            "  var d = new Date(date);" +
            "  var y = d.getFullYear();" +
            "  var M = ('0' + (d.getMonth() + 1)).slice(-2);" +
            "  var D = ('0' + d.getDate()).slice(-2);" +
            "  var h = ('0' + d.getHours()).slice(-2);" +
            "  var m = ('0' + d.getMinutes()).slice(-2);" +
            "  var s = ('0' + d.getSeconds()).slice(-2);" +
            "  var result = pattern;" +
            "  result = result.replace('yyyy', y);" +
            "  result = result.replace('MM', M);" +
            "  result = result.replace('dd', D);" +
            "  result = result.replace('HH', h);" +
            "  result = result.replace('mm', m);" +
            "  result = result.replace('ss', s);" +
            "  return result;" +
            "};" +
            "var DATE_ADD = function(date, amount, unit) {" +
            "  if (date == null) return null;" +
            "  var d = new Date(date);" +
            "  amount = Number(amount);" +
            "  if (unit === 'year') d.setFullYear(d.getFullYear() + amount);" +
            "  else if (unit === 'month') d.setMonth(d.getMonth() + amount);" +
            "  else if (unit === 'day') d.setDate(d.getDate() + amount);" +
            "  else if (unit === 'hour') d.setHours(d.getHours() + amount);" +
            "  else if (unit === 'minute') d.setMinutes(d.getMinutes() + amount);" +
            "  else if (unit === 'second') d.setSeconds(d.getSeconds() + amount);" +
            "  return d.toISOString();" +
            "};" +
            "var DATE_DIFF = function(start, end, unit) {" +
            "  if (start == null || end == null) return 0;" +
            "  var s = new Date(start); var e = new Date(end);" +
            "  var diff = e.getTime() - s.getTime();" +
            "  if (unit === 'year') return e.getFullYear() - s.getFullYear();" +
            "  if (unit === 'month') return (e.getFullYear() - s.getFullYear()) * 12 + e.getMonth() - s.getMonth();" +
            "  if (unit === 'day') return Math.floor(diff / 86400000);" +
            "  if (unit === 'hour') return Math.floor(diff / 3600000);" +
            "  if (unit === 'minute') return Math.floor(diff / 60000);" +
            "  if (unit === 'second') return Math.floor(diff / 1000);" +
            "  return diff;" +
            "};" +
            "var ABS = function(n) { return Math.abs(n); };" +
            "var ROUND = function(n, d) { d = d || 0; var f = Math.pow(10, d); return Math.round(n * f) / f; };" +
            "var CEIL = function(n) { return Math.ceil(n); };" +
            "var FLOOR = function(n) { return Math.floor(n); };" +
            "var MOD = function(n, d) { return n % d; };" +
            "var IS_NULL = function(v) { return v === null || v === undefined; };" +
            "var IS_EMPTY = function(v) { return v === null || v === undefined || v === ''; };" +
            "var COALESCE = function() {" +
            "  for (var i = 0; i < arguments.length; i++) {" +
            "    if (arguments[i] !== null && arguments[i] !== undefined) return arguments[i];" +
            "  }" +
            "  return null;" +
            "};";

    private final Engine sharedEngine;
    private final ExpressionValidator validator;
    private final ExecutorService timeoutExecutor;

    public ExpressionSandboxExecutor() {
        this.sharedEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        this.validator = new ExpressionValidator();
        this.timeoutExecutor = Executors.newCachedThreadPool();
    }

    public ExpressionResult execute(String expression, Map<String, Object> context) {
        long startTime = System.currentTimeMillis();

        ValidationResult validation = validator.validate(expression);
        if (!validation.isValid()) {
            long duration = System.currentTimeMillis() - startTime;
            return ExpressionResult.failure("表达式校验失败: " + String.join("; ", validation.getErrors()), duration);
        }

        String processedExpression = preprocessExpression(expression, context);

        try {
            Future<Object> future = timeoutExecutor.submit(() -> {
                try (Context ctx = Context.newBuilder("js")
                        .engine(sharedEngine)
                        .allowAllAccess(false)
                        .allowIO(false)
                        .allowNativeAccess(false)
                        .allowCreateThread(false)
                        .allowHostClassLoading(false)
                        .allowHostClassLookup(className -> false)
                        .build()) {
                    ctx.eval("js", FUNCTION_DEFINITIONS);
                    if (context != null) {
                        for (Map.Entry<String, Object> entry : context.entrySet()) {
                            ctx.getBindings("js").putMember(entry.getKey(), entry.getValue());
                        }
                    }
                    Value result = ctx.eval("js", processedExpression);
                    return convertResult(result);
                }
            });
            Object result = future.get(DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            long duration = System.currentTimeMillis() - startTime;
            return ExpressionResult.success(result, duration);
        } catch (TimeoutException e) {
            long duration = System.currentTimeMillis() - startTime;
            return ExpressionResult.failure("表达式执行超时(超过" + DEFAULT_TIMEOUT_MS + "ms)", duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return ExpressionResult.failure("表达式执行错误: " + cause.getMessage(), duration);
        }
    }

    private Object convertResult(Value result) {
        if (result.isNull()) {
            return null;
        }
        if (result.isBoolean()) {
            return result.asBoolean();
        }
        if (result.isNumber()) {
            return result.asDouble();
        }
        if (result.isString()) {
            return result.asString();
        }
        return result.as(Object.class);
    }

    private String preprocessExpression(String expression, Map<String, Object> context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1).trim();
            Object value = resolveVariable(varName, context);
            String replacement = toJsExpression(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private Object resolveVariable(String varName, Map<String, Object> context) {
        if (context == null) {
            return null;
        }

        Object value = context.get(varName);
        if (value != null) {
            return value;
        }

        String[] parts = varName.split("\\.");
        Object current = null;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (i == 0) {
                current = context.get(part);
            } else {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(part);
                } else {
                    return null;
                }
            }
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    private String toJsExpression(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number) {
            return value.toString();
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof String) {
            return "\"" + escapeJsString((String) value) + "\"";
        }
        if (value instanceof Date) {
            return "new Date(" + ((Date) value).getTime() + ")";
        }
        if (value instanceof Map) {
            return toJsObject((Map<?, ?>) value);
        }
        return "\"" + escapeJsString(value.toString()) + "\"";
    }

    private String toJsObject(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            sb.append("\"").append(escapeJsString(entry.getKey().toString())).append("\":");
            sb.append(toJsExpression(entry.getValue()));
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private String escapeJsString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @PreDestroy
    public void destroy() {
        shutdown();
    }

    public void shutdown() {
        timeoutExecutor.shutdown();
        try {
            if (!timeoutExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                timeoutExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            timeoutExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
