package com.lowcode.monitor.engine;

import com.lowcode.monitor.entity.LoadTestConfig;
import com.lowcode.monitor.entity.LoadTestMetrics;
import com.lowcode.monitor.entity.LoadTestResult;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class LoadTestEngine {

    private final LoadTestConfig config;
    @Getter
    private final LoadTestMetrics metrics;
    @Getter
    private final List<LoadTestResult> allResults = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;
    private ExecutorService userPool;
    private final RestTemplate restTemplate = new RestTemplate();
    private final List<Future<?>> userFutures = new ArrayList<>();
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    public LoadTestEngine(LoadTestConfig config) {
        this.config = config;
        this.metrics = new LoadTestMetrics();
        this.metrics.setTestId(config.getTestId());
        this.metrics.setStatus("READY");
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        stopped.set(false);
        metrics.setStatus("RUNNING");
        metrics.setStartTime(System.currentTimeMillis());

        scheduler = Executors.newScheduledThreadPool(2);
        userPool = Executors.newFixedThreadPool(config.getVirtualUsers());

        scheduler.scheduleAtFixedRate(this::updateMetrics, 1, 1, TimeUnit.SECONDS);

        long startDelay = (long) config.getRampUpSeconds() * 1000 / config.getVirtualUsers();
        for (int i = 0; i < config.getVirtualUsers(); i++) {
            final int userId = i;
            scheduler.schedule(() -> {
                if (!stopped.get()) {
                    Future<?> future = userPool.submit(() -> runVirtualUser(userId));
                    userFutures.add(future);
                }
            }, startDelay * i, TimeUnit.MILLISECONDS);
        }

        scheduler.schedule(() -> stop(), config.getDurationSeconds(), TimeUnit.SECONDS);
    }

    public synchronized void stop() {
        if (stopped.get()) {
            return;
        }
        stopped.set(true);
        running.set(false);
        metrics.setStatus("STOPPED");
        metrics.setEndTime(System.currentTimeMillis());
        metrics.setElapsedSeconds((metrics.getEndTime() - metrics.getStartTime()) / 1000);
        metrics.calculateDerivedMetrics();

        userFutures.forEach(f -> f.cancel(true));
        userPool.shutdownNow();
        scheduler.shutdownNow();

        log.info("压力测试结束: testId={}, totalRequests={}, successRate={}%, avgResponseTime={}ms, RPS={}",
                metrics.getTestId(),
                metrics.getTotalRequests(),
                String.format("%.2f", metrics.getSuccessRate()),
                String.format("%.0f", metrics.getAvgResponseTime()),
                String.format("%.2f", metrics.getRequestsPerSecond()));
    }

    private void runVirtualUser(int userId) {
        activeUsers.incrementAndGet();
        String virtualUserId = "VU-" + userId;
        try {
            while (!stopped.get() && !Thread.currentThread().isInterrupted()) {
                LoadTestResult result = executeRequest(virtualUserId);
                allResults.add(result);
                metrics.recordResult(result);

                if (config.getThinkTimeMs() > 0 && !stopped.get()) {
                    Thread.sleep(config.getThinkTimeMs());
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            activeUsers.decrementAndGet();
        }
    }

    private LoadTestResult executeRequest(String virtualUserId) {
        LoadTestResult result = new LoadTestResult();
        result.setTimestamp(System.currentTimeMillis());
        result.setVirtualUserId(virtualUserId);
        result.setRequestId(java.util.UUID.randomUUID().toString());

        long startTime = System.nanoTime();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(config.getContentType()));
            if (config.getHeaders() != null) {
                config.getHeaders().forEach(headers::set);
            }

            HttpEntity<String> entity = new HttpEntity<>(config.getRequestBody(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getTargetUrl(),
                    HttpMethod.valueOf(config.getHttpMethod()),
                    entity,
                    String.class
            );

            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            result.setResponseTimeMs((int) responseTimeMs);
            result.setStatusCode(response.getStatusCodeValue());

            boolean success = response.getStatusCode().is2xxSuccessful();
            if (config.getAssertionStatusCodes() != null && !config.getAssertionStatusCodes().isEmpty()) {
                success = config.getAssertionStatusCodes().contains(String.valueOf(response.getStatusCodeValue()));
            }
            if (config.getAssertionBodyContains() != null && !config.getAssertionBodyContains().isEmpty()) {
                String body = response.getBody();
                success = success && (body != null && body.contains(config.getAssertionBodyContains()));
            }

            result.setSuccess(success);
            result.setResponseSize(response.getBody() != null ? response.getBody().length() : 0);

        } catch (Exception e) {
            long responseTimeMs = (System.nanoTime() - startTime) / 1_000_000;
            result.setResponseTimeMs((int) Math.min(responseTimeMs, Integer.MAX_VALUE));
            result.setStatusCode(0);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setResponseSize(0);
        }

        return result;
    }

    private void updateMetrics() {
        if (!running.get()) return;
        metrics.setElapsedSeconds((System.currentTimeMillis() - metrics.getStartTime()) / 1000);
        metrics.setActiveUsers(activeUsers.get());
        metrics.calculateDerivedMetrics();
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isStopped() {
        return stopped.get();
    }
}
