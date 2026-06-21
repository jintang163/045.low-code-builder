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

    public synchronized LoadTestMetrics stopTest(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null && !engine.isStopped()) {
            engine.stop();
            moveToCompleted(testId, engine);
            return engine.getMetrics();
        }
        LoadTestReport report = completedReports.get(testId);
        return report != null ? report.getSummary() : null;
    }

    private synchronized void moveToCompleted(String testId, LoadTestEngine engine) {
        if (completedReports.containsKey(testId)) {
            return;
        }
        LoadTestConfig config = testConfigs.get(testId);
        if (config != null) {
            LoadTestMetrics finalMetrics = engine.getMetrics();
            LoadTestReport report = LoadTestReportGenerator.generateReport(
                    config,
                    finalMetrics,
                    engine.getAllResults(),
                    throughputHistory.get(testId),
                    responseTimeHistory.get(testId),
                    errorRateHistory.get(testId)
            );
            completedReports.put(testId, report);
        }
        runningTests.remove(testId);
    }

    public LoadTestMetrics getTestMetrics(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null) {
            if (engine.isFinished()) {
                moveToCompleted(testId, engine);
                LoadTestReport report = completedReports.get(testId);
                return report != null ? report.getSummary() : null;
            }
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
            if (engine.isFinished()) {
                moveToCompleted(testId, engine);
                return completedReports.get(testId);
            }
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
        checkAndMoveCompletedTests();

        List<Map<String, Object>> tests = new ArrayList<>();
        Set<String> addedTestIds = new HashSet<>();

        for (Map.Entry<String, LoadTestEngine> entry : runningTests.entrySet()) {
            String testId = entry.getKey();
            LoadTestEngine engine = entry.getValue();
            if (engine.isFinished()) {
                continue;
            }
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("testId", testId);
            test.put("status", engine.getMetrics().getStatus());
            LoadTestConfig config = testConfigs.get(testId);
            if (config != null) {
                test.put("testName", config.getTestName());
                test.put("targetUrl", config.getTargetUrl());
                test.put("virtualUsers", config.getVirtualUsers());
            }
            test.put("metrics", engine.getMetrics());
            tests.add(test);
            addedTestIds.add(testId);
        }

        for (Map.Entry<String, LoadTestReport> entry : completedReports.entrySet()) {
            if (addedTestIds.contains(entry.getKey())) {
                continue;
            }
            Map<String, Object> test = new LinkedHashMap<>();
            test.put("testId", entry.getKey());
            test.put("status", entry.getValue().getStatus());
            LoadTestReport report = entry.getValue();
            test.put("testName", report.getTestName());
            test.put("targetUrl", report.getTargetUrl());
            test.put("virtualUsers", report.getConfiguredVirtualUsers());
            test.put("duration", report.getDurationSeconds());
            test.put("metrics", report.getSummary());
            tests.add(test);
            addedTestIds.add(entry.getKey());
        }

        tests.sort((a, b) -> {
            Long aTime = a.containsKey("metrics") ? ((LoadTestMetrics) a.get("metrics")).getStartTime() : 0L;
            Long bTime = b.containsKey("metrics") ? ((LoadTestMetrics) b.get("metrics")).getStartTime() : 0L;
            return Long.compare(bTime, aTime);
        });

        return tests;
    }

    private synchronized void checkAndMoveCompletedTests() {
        List<String> toMove = new ArrayList<>();
        for (Map.Entry<String, LoadTestEngine> entry : runningTests.entrySet()) {
            if (entry.getValue().isFinished()) {
                toMove.add(entry.getKey());
            }
        }
        for (String testId : toMove) {
            LoadTestEngine engine = runningTests.get(testId);
            if (engine != null) {
                moveToCompleted(testId, engine);
            }
        }
    }

    public synchronized boolean deleteTest(String testId) {
        LoadTestEngine engine = runningTests.get(testId);
        if (engine != null && engine.isRunning()) {
            engine.stop();
        }
        runningTests.remove(testId);
        completedReports.remove(testId);
        testConfigs.remove(testId);
        throughputHistory.remove(testId);
        responseTimeHistory.remove(testId);
        errorRateHistory.remove(testId);
        return true;
    }

    private String generateTestId() {
        return "LOAD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
