package com.lowcode.common.monitor.store;

import com.lowcode.common.monitor.entity.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
public class MonitorDataStore {

    private static final int MAX_LOG_SIZE = 10000;
    private static final int SLOW_SQL_THRESHOLD = 1000;

    private static final List<RequestLog> requestLogs = new CopyOnWriteArrayList<>();
    private static final List<SlowSqlLog> slowSqlLogs = new CopyOnWriteArrayList<>();
    private static final List<PageVisitLog> pageVisitLogs = new CopyOnWriteArrayList<>();
    private static final Map<String, Set<String>> dailyUv = new ConcurrentHashMap<>();
    private static final List<AlertRule> alertRules = new CopyOnWriteArrayList<>();
    private static final List<Map<String, Object>> activeAlerts = new CopyOnWriteArrayList<>();
    private static final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_COOLDOWN_MS = 60000;

    public static int getSlowSqlThreshold() {
        return SLOW_SQL_THRESHOLD;
    }

    public static void addRequestLog(RequestLog log) {
        requestLogs.add(0, log);
        if (requestLogs.size() > MAX_LOG_SIZE) {
            requestLogs.remove(requestLogs.size() - 1);
        }
        checkAlertRules();
    }

    public static void addRequestLogs(List<RequestLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        requestLogs.addAll(0, logs);
        while (requestLogs.size() > MAX_LOG_SIZE) {
            requestLogs.remove(requestLogs.size() - 1);
        }
        checkAlertRules();
    }

    public static void addSlowSqlLog(SlowSqlLog log) {
        slowSqlLogs.add(0, log);
        if (slowSqlLogs.size() > MAX_LOG_SIZE) {
            slowSqlLogs.remove(slowSqlLogs.size() - 1);
        }
        linkSlowSqlToRequest(log);
    }

    public static void addSlowSqlLogs(List<SlowSqlLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        for (SlowSqlLog log : logs) {
            slowSqlLogs.add(0, log);
            linkSlowSqlToRequest(log);
        }
        while (slowSqlLogs.size() > MAX_LOG_SIZE) {
            slowSqlLogs.remove(slowSqlLogs.size() - 1);
        }
    }

    public static void addPageVisitLog(PageVisitLog log) {
        pageVisitLogs.add(0, log);
        if (pageVisitLogs.size() > MAX_LOG_SIZE) {
            pageVisitLogs.remove(pageVisitLogs.size() - 1);
        }
        String dateKey = LocalDate.now().toString();
        dailyUv.computeIfAbsent(dateKey, k -> ConcurrentHashMap.newKeySet());
        if (log.getUserId() != null && !log.getUserId().isEmpty()) {
            dailyUv.get(dateKey).add(log.getUserId());
        } else if (log.getSessionId() != null && !log.getSessionId().isEmpty()) {
            dailyUv.get(dateKey).add(log.getSessionId());
        }
    }

    public static void addPageVisitLogs(List<PageVisitLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        for (PageVisitLog log : logs) {
            addPageVisitLog(log);
        }
    }

    private static void linkSlowSqlToRequest(SlowSqlLog slowSql) {
        String traceId = slowSql.getTraceId();
        if (traceId == null || traceId.isEmpty()) return;
        for (RequestLog req : requestLogs) {
            if (traceId.equals(req.getTraceId())) {
                if (req.getErrorStack() == null) {
                    req.setErrorStack("");
                }
                return;
            }
        }
    }

    public static List<RequestLog> getRequestLogs(int limit) {
        return requestLogs.stream().limit(limit).collect(Collectors.toList());
    }

    public static List<SlowSqlLog> getSlowSqlLogs(int limit) {
        return slowSqlLogs.stream().limit(limit).collect(Collectors.toList());
    }

    public static List<SlowSqlLog> getSlowSqlLogsByTraceId(String traceId) {
        return slowSqlLogs.stream()
                .filter(s -> traceId.equals(s.getTraceId()))
                .collect(Collectors.toList());
    }

    public static MonitorMetrics getMetrics() {
        MonitorMetrics metrics = new MonitorMetrics();
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        List<RequestLog> todayRequests = getRecentRequestLogs(todayStart);

        long pv = pageVisitLogs.stream()
                .filter(l -> l.getVisitTime() != null && l.getVisitTime().isAfter(todayStart))
                .count();
        metrics.setPvTotal(pv);

        String todayKey = LocalDate.now().toString();
        Set<String> uvSet = dailyUv.getOrDefault(todayKey, Collections.emptySet());
        metrics.setUvTotal((long) uvSet.size());

        long reqTotal = todayRequests.size();
        metrics.setRequestTotal(reqTotal);

        long errTotal = todayRequests.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 500)
                .count();
        metrics.setErrorTotal(errTotal);
        metrics.setErrorRate(reqTotal > 0 ? (errTotal * 100.0 / reqTotal) : 0.0);

        OptionalDouble avgTime = todayRequests.stream()
                .filter(l -> l.getCostTime() != null)
                .mapToLong(RequestLog::getCostTime)
                .average();
        metrics.setAvgResponseTime(avgTime.orElse(0.0));

        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long qps = getRecentRequestLogs(oneMinuteAgo).size() / 60;
        metrics.setQps(qps);

        Map<String, long[]> apiStats = new HashMap<>();
        todayRequests.stream()
                .filter(l -> l.getRequestUrl() != null)
                .forEach(l -> {
                    String url = l.getRequestUrl();
                    long[] stats = apiStats.computeIfAbsent(url, k -> new long[3]);
                    stats[0]++;
                    if (l.getCostTime() != null) {
                        stats[1] += l.getCostTime();
                    }
                    if (l.getResponseStatus() != null && l.getResponseStatus() >= 500) {
                        stats[2]++;
                    }
                });

        List<Map<String, Object>> apiTop10 = apiStats.entrySet().stream()
                .sorted((a, b) -> Long.compare(
                        b.getValue()[1] / Math.max(b.getValue()[0], 1),
                        a.getValue()[1] / Math.max(a.getValue()[0], 1)
                ))
                .limit(10)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("url", e.getKey());
                    m.put("count", e.getValue()[0]);
                    m.put("avgTime", e.getValue()[0] > 0 ? e.getValue()[1] / e.getValue()[0] : 0);
                    m.put("errorCount", e.getValue()[2]);
                    return m;
                })
                .collect(Collectors.toList());
        metrics.setApiTop10(apiTop10);

        List<Map<String, Object>> slowSqlList = slowSqlLogs.stream()
                .limit(10)
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("sql", s.getSql());
                    m.put("executeTime", s.getExecuteTime());
                    m.put("dataSource", s.getDataSource());
                    m.put("happenTime", s.getHappenTime());
                    m.put("traceId", s.getTraceId());
                    return m;
                })
                .collect(Collectors.toList());
        metrics.setSlowSqlList(slowSqlList);

        List<Map<String, Object>> pageVisitTrend = buildTrendData(pageVisitLogs, "visitTime", 7);
        metrics.setPageVisitTrend(pageVisitTrend);

        List<Map<String, Object>> errorTrend = buildErrorTrend(7);
        metrics.setErrorTrend(errorTrend);

        List<Map<String, Object>> serviceStatus = buildServiceStatus();
        metrics.setServiceStatus(serviceStatus);

        metrics.setActiveAlerts(new ArrayList<>(activeAlerts));

        return metrics;
    }

    private static List<RequestLog> getRecentRequestLogs(LocalDateTime startTime) {
        return requestLogs.stream()
                .filter(l -> l.getRequestTime() != null && l.getRequestTime().isAfter(startTime))
                .collect(Collectors.toList());
    }

    private static List<Map<String, Object>> buildTrendData(List<?> logs, String timeField, int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        String getterName = "get" + timeField.substring(0, 1).toUpperCase() + timeField.substring(1);
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);
            long count = logs.stream()
                    .filter(l -> {
                        try {
                            LocalDateTime t = (LocalDateTime) l.getClass().getMethod(getterName).invoke(l);
                            return t != null && !t.isBefore(start) && !t.isAfter(end);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", date.toString());
            m.put("count", count);
            trend.add(m);
        }
        return trend;
    }

    private static List<Map<String, Object>> buildErrorTrend(int days) {
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            List<RequestLog> dayLogs = requestLogs.stream()
                    .filter(l -> l.getRequestTime() != null
                            && !l.getRequestTime().isBefore(start)
                            && !l.getRequestTime().isAfter(end))
                    .collect(Collectors.toList());

            long total = dayLogs.size();
            long errors = dayLogs.stream()
                    .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 500)
                    .count();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", date.toString());
            m.put("total", total);
            m.put("errors", errors);
            m.put("errorRate", total > 0 ? (errors * 100.0 / total) : 0.0);
            trend.add(m);
        }
        return trend;
    }

    private static List<Map<String, Object>> buildServiceStatus() {
        Map<String, long[]> serviceStats = new HashMap<>();
        requestLogs.stream()
                .filter(l -> l.getServiceName() != null)
                .forEach(l -> {
                    String svc = l.getServiceName();
                    long[] stats = serviceStats.computeIfAbsent(svc, k -> new long[2]);
                    stats[0]++;
                    if (l.getResponseStatus() != null && l.getResponseStatus() >= 500) {
                        stats[1]++;
                    }
                });

        String[] allServices = {"lowcode-auth", "lowcode-model", "lowcode-page", "lowcode-flow",
                "lowcode-generator", "lowcode-oss", "lowcode-gateway", "lowcode-monitor"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (String svc : allServices) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", svc);
            long[] stats = serviceStats.getOrDefault(svc, new long[2]);
            m.put("totalRequests", stats[0]);
            m.put("errorCount", stats[1]);
            String status;
            if (stats[0] == 0) {
                status = "unknown";
            } else if (stats[1] * 100.0 / stats[0] > 10) {
                status = "warning";
            } else {
                status = "healthy";
            }
            m.put("status", status);
            result.add(m);
        }
        return result;
    }

    public static void addAlertRule(AlertRule rule) {
        rule.setId(System.currentTimeMillis());
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        alertRules.add(rule);
    }

    public static List<AlertRule> getAlertRules() {
        return new ArrayList<>(alertRules);
    }

    public static void updateAlertRule(AlertRule rule) {
        for (int i = 0; i < alertRules.size(); i++) {
            if (alertRules.get(i).getId().equals(rule.getId())) {
                rule.setUpdateTime(LocalDateTime.now());
                alertRules.set(i, rule);
                break;
            }
        }
    }

    public static void deleteAlertRule(Long id) {
        alertRules.removeIf(r -> r.getId().equals(id));
    }

    private static void checkAlertRules() {
        for (AlertRule rule : alertRules) {
            if (!rule.getEnabled()) continue;

            String alertKey = rule.getId() + "_" + rule.getRuleType();
            Long lastTime = lastAlertTime.get(alertKey);
            if (lastTime != null && System.currentTimeMillis() - lastTime < ALERT_COOLDOWN_MS) {
                continue;
            }

            if ("error_rate".equals(rule.getRuleType())) {
                checkErrorRateAlert(rule, alertKey);
            } else if ("slow_api".equals(rule.getRuleType())) {
                checkSlowApiAlert(rule, alertKey);
            } else if ("slow_sql".equals(rule.getRuleType())) {
                checkSlowSqlAlert(rule, alertKey);
            }
        }
    }

    private static void checkErrorRateAlert(AlertRule rule, String alertKey) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(Math.max(rule.getDuration(), 1));
        List<RequestLog> recentLogs = getRecentRequestLogs(startTime);
        if (recentLogs.isEmpty()) return;

        long errors = recentLogs.stream()
                .filter(l -> l.getResponseStatus() != null && l.getResponseStatus() >= 500)
                .count();
        double errorRate = errors * 100.0 / recentLogs.size();

        boolean trigger = evaluateCondition(rule.getOperator(), errorRate, rule.getThreshold());

        if (trigger) {
            triggerAlert(rule, "错误率告警: 当前错误率 " + String.format("%.2f", errorRate) + "% "
                    + rule.getOperator() + " 阈值 " + rule.getThreshold() + "%", alertKey);
        }
    }

    private static void checkSlowApiAlert(AlertRule rule, String alertKey) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(Math.max(rule.getDuration(), 1));
        List<RequestLog> recentLogs = getRecentRequestLogs(startTime);

        for (RequestLog reqLog : recentLogs) {
            if (reqLog.getCostTime() != null && evaluateCondition(rule.getOperator(), reqLog.getCostTime(), rule.getThreshold())) {
                triggerAlert(rule, "慢接口告警: " + reqLog.getRequestUrl()
                        + " 耗时 " + reqLog.getCostTime() + "ms " + rule.getOperator()
                        + " 阈值 " + rule.getThreshold() + "ms", alertKey);
                break;
            }
        }
    }

    private static void checkSlowSqlAlert(AlertRule rule, String alertKey) {
        LocalDateTime startTime = LocalDateTime.now().minusMinutes(Math.max(rule.getDuration(), 1));
        List<SlowSqlLog> recentSlowSql = slowSqlLogs.stream()
                .filter(s -> s.getHappenTime() != null && s.getHappenTime().isAfter(startTime))
                .collect(Collectors.toList());

        for (SlowSqlLog sql : recentSlowSql) {
            if (sql.getExecuteTime() != null && evaluateCondition(rule.getOperator(), sql.getExecuteTime(), rule.getThreshold())) {
                triggerAlert(rule, "慢SQL告警: " + (sql.getSql().length() > 50 ? sql.getSql().substring(0, 50) + "..." : sql.getSql())
                        + " 耗时 " + sql.getExecuteTime() + "ms " + rule.getOperator()
                        + " 阈值 " + rule.getThreshold() + "ms", alertKey);
                break;
            }
        }
    }

    private static boolean evaluateCondition(String operator, double actual, double threshold) {
        switch (operator) {
            case ">":
                return actual > threshold;
            case ">=":
                return actual >= threshold;
            case "<":
                return actual < threshold;
            case "<=":
                return actual <= threshold;
            case "==":
                return actual == threshold;
            case "!=":
                return actual != threshold;
            default:
                return false;
        }
    }

    private static void triggerAlert(AlertRule rule, String message, String alertKey) {
        lastAlertTime.put(alertKey, System.currentTimeMillis());

        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("id", UUID.randomUUID().toString());
        alert.put("ruleId", rule.getId());
        alert.put("ruleName", rule.getRuleName());
        alert.put("message", message);
        alert.put("level", "warning");
        alert.put("triggerTime", LocalDateTime.now());
        activeAlerts.add(0, alert);
        if (activeAlerts.size() > 100) {
            activeAlerts.remove(activeAlerts.size() - 1);
        }
        log.warn("告警触发: {} - {}", rule.getRuleName(), message);
        DingTalkNotifier.sendAlert(rule, message);
    }

    public static void clearAlert(String alertId) {
        activeAlerts.removeIf(a -> alertId.equals(a.get("id")));
    }

    public static void clearAll() {
        requestLogs.clear();
        slowSqlLogs.clear();
        pageVisitLogs.clear();
        dailyUv.clear();
        activeAlerts.clear();
        lastAlertTime.clear();
    }
}
