package com.lowcode.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class LoadTestMetrics implements Serializable {

    private String testId;

    private volatile String status;

    private long startTime;

    private long endTime;

    private long elapsedSeconds;

    private AtomicInteger totalRequests = new AtomicInteger(0);

    private AtomicInteger successRequests = new AtomicInteger(0);

    private AtomicInteger failedRequests = new AtomicInteger(0);

    private AtomicLong totalResponseTime = new AtomicLong(0);

    private AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);

    private AtomicLong maxResponseTime = new AtomicLong(0);

    private double requestsPerSecond;

    private double successRate;

    private double avgResponseTime;

    private int activeUsers;

    public void recordResult(LoadTestResult result) {
        totalRequests.incrementAndGet();
        if (result.isSuccess()) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalResponseTime.addAndGet(result.getResponseTimeMs());
        minResponseTime.updateAndGet(v -> Math.min(v, result.getResponseTimeMs()));
        maxResponseTime.updateAndGet(v -> Math.max(v, result.getResponseTimeMs()));
    }

    public void calculateDerivedMetrics() {
        long elapsed = Math.max(1, elapsedSeconds);
        this.requestsPerSecond = (double) totalRequests.get() / elapsed;
        int total = totalRequests.get();
        this.successRate = total > 0 ? (double) successRequests.get() / total * 100 : 0;
        this.avgResponseTime = total > 0 ? (double) totalResponseTime.get() / total : 0;
        if (minResponseTime.get() == Long.MAX_VALUE) {
            minResponseTime.set(0);
        }
    }
}
