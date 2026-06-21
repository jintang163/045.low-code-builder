package com.lowcode.monitor.entity;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class LoadTestReport implements Serializable {

    private String testId;

    private String testName;

    private String targetUrl;

    private String httpMethod;

    private String status;

    private long startTime;

    private long endTime;

    private long durationSeconds;

    private int configuredVirtualUsers;

    private int configuredDurationSeconds;

    private LoadTestMetrics summary;

    private ResponseTimeDistribution responseTimeDistribution;

    private List<TimeSeriesData> throughputSeries;

    private List<TimeSeriesData> responseTimeSeries;

    private List<TimeSeriesData> errorRateSeries;

    private List<ErrorDetail> errorDetails;

    private Map<String, Object> bottleneckAnalysis;

    @Data
    public static class ResponseTimeDistribution implements Serializable {
        private long min;
        private long max;
        private double avg;
        private double median;
        private double p50;
        private double p75;
        private double p90;
        private double p95;
        private double p99;
        private long[] buckets;
        private String[] bucketLabels;
    }

    @Data
    public static class TimeSeriesData implements Serializable {
        private long timestamp;
        private double value;
    }

    @Data
    public static class ErrorDetail implements Serializable {
        private String errorType;
        private String message;
        private int count;
        private List<Integer> sampleStatusCodes;
    }
}
