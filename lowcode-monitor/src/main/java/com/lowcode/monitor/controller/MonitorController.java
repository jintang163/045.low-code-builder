package com.lowcode.monitor.controller;

import com.lowcode.common.monitor.entity.*;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitor")
@CrossOrigin(origins = "*")
public class MonitorController {

    @GetMapping("/metrics")
    public Result<MonitorMetrics> getMetrics() {
        return Result.ok(MonitorDataStore.getMetrics());
    }

    @GetMapping("/requests")
    public Result<List<RequestLog>> getRequestLogs(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(MonitorDataStore.getRequestLogs(limit));
    }

    @GetMapping("/slowSql")
    public Result<List<SlowSqlLog>> getSlowSqlLogs(@RequestParam(defaultValue = "100") int limit) {
        return Result.ok(MonitorDataStore.getSlowSqlLogs(limit));
    }

    @GetMapping("/slowSql/trace/{traceId}")
    public Result<List<SlowSqlLog>> getSlowSqlLogsByTraceId(@PathVariable String traceId) {
        return Result.ok(MonitorDataStore.getSlowSqlLogsByTraceId(traceId));
    }

    @PostMapping("/pageVisit")
    public Result<Void> addPageVisitLog(@RequestBody PageVisitLog pageVisitLog) {
        MonitorDataStore.addPageVisitLog(pageVisitLog);
        return Result.ok();
    }

    @PostMapping("/report/request")
    public Result<Void> reportRequestLogs(@RequestBody List<RequestLog> logs,
                                          @RequestHeader(value = "X-Service-Name", required = false) String serviceName) {
        if (logs != null && !logs.isEmpty()) {
            if (serviceName != null && !serviceName.isEmpty()) {
                for (RequestLog logItem : logs) {
                    if (logItem.getServiceName() == null || logItem.getServiceName().isEmpty()) {
                        logItem.setServiceName(serviceName);
                    }
                }
            }
            MonitorDataStore.addRequestLogs(logs);
            log.debug("接收请求日志上报: {} 条, 来源服务: {}", logs.size(), serviceName);
        }
        return Result.ok();
    }

    @PostMapping("/report/slowSql")
    public Result<Void> reportSlowSqlLogs(@RequestBody List<SlowSqlLog> logs,
                                          @RequestHeader(value = "X-Service-Name", required = false) String serviceName) {
        if (logs != null && !logs.isEmpty()) {
            MonitorDataStore.addSlowSqlLogs(logs);
            log.debug("接收慢SQL上报: {} 条, 来源服务: {}", logs.size(), serviceName);
        }
        return Result.ok();
    }

    @PostMapping("/report/pageVisit")
    public Result<Void> reportPageVisitLogs(@RequestBody List<PageVisitLog> logs,
                                            @RequestHeader(value = "X-Service-Name", required = false) String serviceName) {
        if (logs != null && !logs.isEmpty()) {
            MonitorDataStore.addPageVisitLogs(logs);
            log.debug("接收页面访问上报: {} 条, 来源服务: {}", logs.size(), serviceName);
        }
        return Result.ok();
    }

    @GetMapping("/alert/rules")
    public Result<List<AlertRule>> getAlertRules() {
        return Result.ok(MonitorDataStore.getAlertRules());
    }

    @PostMapping("/alert/rules")
    public Result<AlertRule> addAlertRule(@RequestBody AlertRule rule) {
        MonitorDataStore.addAlertRule(rule);
        return Result.ok(rule);
    }

    @PutMapping("/alert/rules")
    public Result<AlertRule> updateAlertRule(@RequestBody AlertRule rule) {
        MonitorDataStore.updateAlertRule(rule);
        return Result.ok(rule);
    }

    @DeleteMapping("/alert/rules/{id}")
    public Result<Void> deleteAlertRule(@PathVariable Long id) {
        MonitorDataStore.deleteAlertRule(id);
        return Result.ok();
    }

    @DeleteMapping("/alert/clear/{alertId}")
    public Result<Void> clearAlert(@PathVariable String alertId) {
        MonitorDataStore.clearAlert(alertId);
        return Result.ok();
    }

    @PostMapping("/alert/test")
    public Result<Map<String, Object>> testAlert(@RequestBody Map<String, Object> params) {
        log.info("测试告警: {}", params);
        return Result.ok(params);
    }

    @PostMapping("/clear")
    public Result<Void> clearAll() {
        MonitorDataStore.clearAll();
        return Result.ok();
    }
}
