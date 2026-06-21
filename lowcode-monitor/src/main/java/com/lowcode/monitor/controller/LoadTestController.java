package com.lowcode.monitor.controller;

import com.lowcode.common.result.Result;
import com.lowcode.monitor.entity.LoadTestConfig;
import com.lowcode.monitor.entity.LoadTestMetrics;
import com.lowcode.monitor.entity.LoadTestReport;
import com.lowcode.monitor.service.LoadTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/monitor/loadtest")
@CrossOrigin(origins = "*")
public class LoadTestController {

    @Autowired
    private LoadTestService loadTestService;

    @PostMapping("/start")
    public Result<LoadTestMetrics> startTest(@RequestBody LoadTestConfig config) {
        log.info("收到压力测试请求: testName={}, vUsers={}, target={}",
                config.getTestName(), config.getVirtualUsers(), config.getTargetUrl());

        if (config.getTargetUrl() == null || config.getTargetUrl().isEmpty()) {
            return Result.error("目标URL不能为空");
        }
        if (config.getVirtualUsers() <= 0) {
            return Result.error("虚拟用户数必须大于0");
        }
        if (config.getDurationSeconds() <= 0) {
            return Result.error("测试时长必须大于0");
        }

        if (config.getVirtualUsers() > 500) {
            return Result.error("虚拟用户数不能超过500（单机限制）");
        }
        if (config.getDurationSeconds() > 3600) {
            return Result.error("测试时长不能超过3600秒（1小时）");
        }

        LoadTestMetrics metrics = loadTestService.startTest(config);
        return Result.ok(metrics);
    }

    @PostMapping("/stop/{testId}")
    public Result<LoadTestMetrics> stopTest(@PathVariable String testId) {
        log.info("停止压力测试: testId={}", testId);
        LoadTestMetrics metrics = loadTestService.stopTest(testId);
        if (metrics == null) {
            return Result.error("测试不存在或已停止");
        }
        return Result.ok(metrics);
    }

    @GetMapping("/metrics/{testId}")
    public Result<LoadTestMetrics> getTestMetrics(@PathVariable String testId) {
        LoadTestMetrics metrics = loadTestService.getTestMetrics(testId);
        if (metrics == null) {
            return Result.error("测试不存在");
        }
        return Result.ok(metrics);
    }

    @GetMapping("/report/{testId}")
    public Result<LoadTestReport> getTestReport(@PathVariable String testId) {
        LoadTestReport report = loadTestService.getTestReport(testId);
        if (report == null) {
            return Result.error("测试报告不存在");
        }
        return Result.ok(report);
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listTests() {
        return Result.ok(loadTestService.listTests());
    }

    @DeleteMapping("/{testId}")
    public Result<Void> deleteTest(@PathVariable String testId) {
        log.info("删除压力测试记录: testId={}", testId);
        boolean deleted = loadTestService.deleteTest(testId);
        return deleted ? Result.ok() : Result.error("删除失败，测试不存在");
    }
}
