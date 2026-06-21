package com.lowcode.monitor.engine;

import com.lowcode.monitor.entity.LoadTestConfig;
import com.lowcode.monitor.entity.LoadTestReport;
import com.lowcode.monitor.entity.LoadTestResult;
import com.lowcode.monitor.entity.LoadTestMetrics;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class LoadTestReportGenerator {

    public static LoadTestReport generateReport(
            LoadTestConfig config,
            LoadTestMetrics metrics,
            List<LoadTestResult> results,
            List<LoadTestReport.TimeSeriesData> throughputSeries,
            List<LoadTestReport.TimeSeriesData> responseTimeSeries,
            List<LoadTestReport.TimeSeriesData> errorRateSeries) {

        LoadTestReport report = new LoadTestReport();
        report.setTestId(config.getTestId());
        report.setTestName(config.getTestName());
        report.setTargetUrl(config.getTargetUrl());
        report.setHttpMethod(config.getHttpMethod());
        report.setStatus(metrics.getStatus());
        report.setStartTime(metrics.getStartTime());
        report.setEndTime(metrics.getEndTime());
        report.setDurationSeconds(metrics.getElapsedSeconds());
        report.setConfiguredVirtualUsers(config.getVirtualUsers());
        report.setConfiguredDurationSeconds(config.getDurationSeconds());
        report.setSummary(metrics);
        report.setThroughputSeries(throughputSeries);
        report.setResponseTimeSeries(responseTimeSeries);
        report.setErrorRateSeries(errorRateSeries);

        if (results != null && !results.isEmpty()) {
            report.setResponseTimeDistribution(calculateResponseTimeDistribution(results));
            report.setErrorDetails(calculateErrorDetails(results));
            report.setBottleneckAnalysis(analyzeBottlenecks(metrics, results, config));
        } else {
            report.setResponseTimeDistribution(new LoadTestReport.ResponseTimeDistribution());
            report.setErrorDetails(new ArrayList<>());
            report.setBottleneckAnalysis(new HashMap<>());
        }

        return report;
    }

    private static LoadTestReport.ResponseTimeDistribution calculateResponseTimeDistribution(
            List<LoadTestResult> results) {

        List<Integer> responseTimes = results.stream()
                .map(LoadTestResult::getResponseTimeMs)
                .sorted()
                .collect(Collectors.toList());

        int size = responseTimes.size();
        LoadTestReport.ResponseTimeDistribution dist = new LoadTestReport.ResponseTimeDistribution();

        if (size == 0) {
            return dist;
        }

        dist.setMin(responseTimes.get(0));
        dist.setMax(responseTimes.get(size - 1));
        dist.setAvg(responseTimes.stream().mapToInt(Integer::intValue).average().orElse(0));
        dist.setMedian(calculatePercentile(responseTimes, 50));
        dist.setP50(dist.getMedian());
        dist.setP75(calculatePercentile(responseTimes, 75));
        dist.setP90(calculatePercentile(responseTimes, 90));
        dist.setP95(calculatePercentile(responseTimes, 95));
        dist.setP99(calculatePercentile(responseTimes, 99));

        int maxRt = responseTimes.get(size - 1);
        int bucketCount = 10;
        long[] buckets = new long[bucketCount];
        String[] labels = new String[bucketCount];
        int bucketSize = Math.max(1, (maxRt + bucketCount - 1) / bucketCount);

        for (int rt : responseTimes) {
            int bucketIndex = Math.min(rt / bucketSize, bucketCount - 1);
            buckets[bucketIndex]++;
        }

        for (int i = 0; i < bucketCount; i++) {
            labels[i] = (i * bucketSize) + "-" + ((i + 1) * bucketSize) + "ms";
        }

        dist.setBuckets(buckets);
        dist.setBucketLabels(labels);

        return dist;
    }

    private static double calculatePercentile(List<Integer> sortedList, double percentile) {
        if (sortedList.isEmpty()) return 0;
        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    private static List<LoadTestReport.ErrorDetail> calculateErrorDetails(List<LoadTestResult> results) {
        Map<String, LoadTestReport.ErrorDetail> errorMap = new LinkedHashMap<>();

        for (LoadTestResult result : results) {
            if (!result.isSuccess()) {
                String errorType;
                if (result.getStatusCode() == 0) {
                    errorType = "Connection Error";
                } else if (result.getStatusCode() >= 500) {
                    errorType = "Server Error (" + result.getStatusCode() + ")";
                } else if (result.getStatusCode() >= 400) {
                    errorType = "Client Error (" + result.getStatusCode() + ")";
                } else {
                    errorType = "Assertion Failed (" + result.getStatusCode() + ")";
                }

                LoadTestReport.ErrorDetail detail = errorMap.computeIfAbsent(errorType, k -> {
                    LoadTestReport.ErrorDetail d = new LoadTestReport.ErrorDetail();
                    d.setErrorType(k);
                    d.setSampleStatusCodes(new ArrayList<>());
                    return d;
                });
                detail.setCount(detail.getCount() + 1);
                detail.setMessage(result.getErrorMessage());
                if (detail.getSampleStatusCodes().size() < 10) {
                    detail.getSampleStatusCodes().add(result.getStatusCode());
                }
            }
        }

        return new ArrayList<>(errorMap.values());
    }

    private static Map<String, Object> analyzeBottlenecks(
            LoadTestMetrics metrics,
            List<LoadTestResult> results,
            LoadTestConfig config) {

        Map<String, Object> analysis = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        double successRate = metrics.getSuccessRate();
        double avgResponseTime = metrics.getAvgResponseTime();
        double rps = metrics.getRequestsPerSecond();
        double errorRate = 100 - successRate;

        if (errorRate > 5) {
            warnings.add("错误率偏高: " + String.format("%.2f%%", errorRate));
            suggestions.add("建议检查服务器错误日志，定位5xx错误原因");
            if (errorRate > 20) {
                suggestions.add("错误率超过20%，建议先排查服务可用性问题再进行性能测试");
            }
        }

        if (avgResponseTime > 3000) {
            warnings.add("平均响应时间过长: " + String.format("%.0fms", avgResponseTime));
            suggestions.add("响应时间超过3秒，建议检查接口逻辑是否存在性能问题");
        } else if (avgResponseTime > 1000) {
            warnings.add("平均响应时间偏慢: " + String.format("%.0fms", avgResponseTime));
            suggestions.add("建议优化数据库查询或增加缓存");
        }

        LoadTestReport.ResponseTimeDistribution dist = calculateResponseTimeDistribution(results);
        if (dist.getP95() > 5000) {
            warnings.add("P95响应时间过高: " + String.format("%.0fms", dist.getP95()));
            suggestions.add("95%的请求响应时间超过5秒，存在严重的性能瓶颈");
        }

        double maxRPS = config.getVirtualUsers() * 1000.0 / Math.max(1, config.getThinkTimeMs());
        if (rps < maxRPS * 0.5) {
            warnings.add("吞吐量未达预期: 当前RPS=" + String.format("%.2f", rps) +
                    ", 理论RPS≈" + String.format("%.2f", maxRPS));
            suggestions.add("可能存在连接池、线程池或数据库连接瓶颈");
        }

        if (dist.getMax() - dist.getMin() > 5000) {
            warnings.add("响应时间波动过大: 极差=" + (dist.getMax() - dist.getMin()) + "ms");
            suggestions.add("响应时间不稳定，可能存在资源竞争或GC问题");
        }

        if (metrics.getActiveUsers() < config.getVirtualUsers() * 0.8) {
            suggestions.add("并发用户数未达到配置值的80%，可能是压测机性能不足");
        }

        analysis.put("overall", generateOverallAssessment(metrics));
        analysis.put("warnings", warnings);
        analysis.put("suggestions", suggestions);
        analysis.put("performanceGrade", calculateGrade(metrics, dist));

        return analysis;
    }

    private static String generateOverallAssessment(LoadTestMetrics metrics) {
        double successRate = metrics.getSuccessRate();
        double avgResponseTime = metrics.getAvgResponseTime();

        if (successRate >= 99 && avgResponseTime < 500) {
            return "优秀 - 接口性能表现出色，响应快且稳定";
        } else if (successRate >= 95 && avgResponseTime < 1000) {
            return "良好 - 接口性能满足生产要求";
        } else if (successRate >= 90 && avgResponseTime < 2000) {
            return "一般 - 接口能正常工作，但性能有待提升";
        } else if (successRate >= 80) {
            return "较差 - 存在明显的性能问题，建议优化后再上线";
        } else {
            return "严重 - 接口稳定性或性能存在严重问题";
        }
    }

    private static String calculateGrade(LoadTestMetrics metrics, LoadTestReport.ResponseTimeDistribution dist) {
        int score = 100;

        if (metrics.getSuccessRate() < 99) score -= 10;
        if (metrics.getSuccessRate() < 95) score -= 15;
        if (metrics.getSuccessRate() < 90) score -= 20;
        if (metrics.getSuccessRate() < 80) score -= 30;

        if (metrics.getAvgResponseTime() > 500) score -= 5;
        if (metrics.getAvgResponseTime() > 1000) score -= 10;
        if (metrics.getAvgResponseTime() > 2000) score -= 15;
        if (metrics.getAvgResponseTime() > 3000) score -= 20;

        if (dist.getP95() > 1000) score -= 5;
        if (dist.getP95() > 2000) score -= 10;
        if (dist.getP95() > 5000) score -= 20;

        score = Math.max(0, Math.min(100, score));

        if (score >= 90) return "A (" + score + ")";
        if (score >= 80) return "B (" + score + ")";
        if (score >= 70) return "C (" + score + ")";
        if (score >= 60) return "D (" + score + ")";
        return "F (" + score + ")";
    }
}
