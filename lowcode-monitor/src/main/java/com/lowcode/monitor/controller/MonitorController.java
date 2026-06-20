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

    @PostMapping("/pageVisit")
    public Result<Void> addPageVisitLog(@RequestBody PageVisitLog pageVisitLog) {
        MonitorDataStore.addPageVisitLog(pageVisitLog);
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
}
