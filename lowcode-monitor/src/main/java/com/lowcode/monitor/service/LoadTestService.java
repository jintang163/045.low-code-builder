package com.lowcode.monitor.service;

import com.lowcode.monitor.engine.LoadTestEngine;
import com.lowcode.monitor.engine.LoadTestReportGenerator;
import com.lowcode.monitor.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LoadTestService {

    private final Map<String, LoadTestEngine> runningTests = new ConcurrentHashMap<>();
    private final Map<String, LoadTestReport> completedReports = new ConcurrentHashMap<>();
    private final Map<String, LoadTestConfig> testConfigs = new ConcurrentHashMap<>();
    private final Map<String, List<LoadTestReport.TimeSeriesData>> throughputHistory = new ConcurrentHashMap<>();
    private final Map<String, List<LoadTestReport.TimeSeriesData>> responseTimeHistory = new ConcurrentHashMap<>();
    private final Map<String, List<LoadTestReport.TimeSeriesData>> errorRateHistory = new ConcurrentHashMap<>();
    private final ScheduledExecutorService metricsCollector = Executors.newScheduledThreadPool(2);

    public LoadTestService() {
        metricsCollector.scheduleAtFixedRate(this::collectTimeSeriesMetrics, 0, 1, TimeUnit.SECONDS);
    }

    public synchronized LoadTestMetrics startTest(LoadTestConfig config) {
        String testId = generateTestId();
        config.setTestId(testId);

        if (config.getTestName() == null || config.getTestName().isEmpty()) {
            config.setTestName("压力测试-" + System.currentTimeMillis());
        }

        LoadTestEngine engine = new LoadTestEngine(config);
        runningTests.put(testId, engine);
        testConfigs.put(testId, config);
        throughputHistory.put(testId, Collections.synchronizedList(new ArrayList<>()));
        responseTimeHistory.put(testId, Collections.synchronizedList(new ArrayList<>()));
        errorRateHistory.put(testId, Collections.synchronizedList(new ArrayList<>()));

        engine.start();

        log.info("启动压力测试: testId={}, testName={}, vUsers={}, duration={}s, target={}",
                testId, config.getTestName(), config.getVirtualUsers(),
                config.getDurationSeconds(), config.getTargetUrl());

        return engine.getMetrics();
    }

    public LoadTestMetrics stopTest(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null) {
            engine.stop();
            LoadTestReport report = generateReport(testId);
            completedReports.put(testId, report);
            return engine.getMetrics();
        }
        return null;
    }

    public LoadTestMetrics getTestMetrics(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null) {
            return engine.getMetrics();
        }
        LoadTestReport report = completedReports.get(testId);
        if (report != null) {
            return report.getSummary();
        }
        return null;
    }

    public LoadTestReport getTestReport(String testId) {
        LoadTestReport report = completedReports.get(testId);
        if (report != null) {
            return report;
        }

        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null) {
            LoadTestConfig config = testConfigs.get(testId);
            LoadTestMetrics interimMetrics = engine.getMetrics();
            interimMetrics.setElapsedSeconds((System.currentTimeMillis() - interimMetrics.getStartTime()) / 1000);
            interimMetrics.calculateDerivedMetrics();

            return LoadTestReportGenerator.generateReport(
                    config,
                    interimMetrics,
                    engine.getAllResults(),
                    throughputHistory.get(testId),
                    responseTimeHistory.get(testId),
                    errorRateHistory.get(testId)
            );
        }

        return null;
    }

    public List<Map<String, Object>> listTests() {
        List<Map<String, Object>> tests = new ArrayList<>();

        for (Map.Entry<String, LoadTestEngine> entry : runningTests.entrySet()) {
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("testId", entry.getKey());
            test.put("status", "RUNNING");
            LoadTestConfig config = testConfigs.get(entry.getKey());
            if (config != null) {
                test.put("testName", config.getTestName());
                test.put("targetUrl", config.getTargetUrl());
                test.put("virtualUsers", config.getVirtualUsers());
            }
            LoadTestMetrics metrics = entry.getValue().getMetrics();
            test.put("metrics", metrics);
            tests.add(test);
        }

        for (Map.Entry<String, LoadTestReport> entry : completedReports.entrySet()) {
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("testId", entry.getKey());
            test.put("status", "COMPLETED");
            LoadTestReport report = entry.getValue();
            test.put("testName", report.getTestName());
            test.put("targetUrl", report.getTargetUrl());
            test.put("virtualUsers", report.getConfiguredVirtualUsers());
            test.put("duration", report.getDurationSeconds());
            test.put("metrics", report.getSummary());
            tests.add(test);
        }

        return tests;
    }

    public boolean deleteTest(String testId) {
        runningTests.remove(testId);
        completedReports.remove(testId);
        testConfigs.remove(testId);
        throughputHistory.remove(testId);
        responseTimeHistory.remove(testId);
        errorRateHistory.remove(testId);
        return true;
    }

    private LoadTestReport generateReport(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        LoadTestConfig config = testConfigs.get(testId);
        if (engine == null || config == null) {
            return null;
        }

        LoadTestMetrics finalMetrics = engine.getMetrics();
        finalMetrics.calculateDerivedMetrics();

        return LoadTestReportGenerator.generateReport(
                config,
                finalMetrics,
                engine.getAllResults(),
                throughputHistory.get(testId),
                responseTimeHistory.get(testId),
                errorRateHistory.get(testId)
        );
    }

    private void collectTimeSeriesMetrics() {
        long timestamp = System.currentTimeMillis();

        for (Map.Entry<String, LoadTestEngine> entry : runningTests.entrySet()) {
            String testId = entry.getKey();
            LoadTestEngine engine = entry.getValue();
            LoadTestMetrics metrics = engine.getMetrics();

            if (!"RUNNING".equals(metrics.getStatus())) {
                continue;
            }

            LoadTestReport.TimeSeriesData tp = new LoadTestReport.TimeSeriesData();
            tp.setTimestamp(timestamp);
            tp.setValue(metrics.getRequestsPerSecond());
            throughputHistory.computeIfAbsent(testId, k -> Collections.synchronizedList(new ArrayList<>())).add(tp);

            LoadTestReport.TimeSeriesData rt = new LoadTestReport.TimeSeriesData();
            rt.setTimestamp(timestamp);
            rt.setValue(metrics.getAvgResponseTime());
            responseTimeHistory.computeIfAbsent(testId, k -> Collections.synchronizedList(new ArrayList<>())).add(rt);

            LoadTestReport.TimeSeriesData er = new LoadTestReport.TimeSeriesData();
            er.setTimestamp(timestamp);
            er.setValue(100 - metrics.getSuccessRate());
            errorRateHistory.computeIfAbsent(testId, k -> Collections.synchronizedList(new ArrayList<>())).add(er);
        }
    }

    private String generateTestId() {
        return "LOAD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
