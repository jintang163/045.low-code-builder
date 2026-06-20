package com.lowcode.common.monitor.report;

import com.alibaba.fastjson2.JSON;
import com.lowcode.common.monitor.entity.RequestLog;
import com.lowcode.common.monitor.entity.SlowSqlLog;
import com.lowcode.common.monitor.entity.PageVisitLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MonitorReportClient {

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Value("${monitor.report.url:http://localhost:8088}")
    private String monitorServerUrl;

    @Value("${monitor.report.enabled:true}")
    private boolean reportEnabled;

    @Value("${monitor.report.batch-size:100}")
    private int batchSize;

    @Value("${monitor.report.interval-ms:1000}")
    private long reportIntervalMs;

    private static final int MAX_QUEUE_SIZE = 10000;
    private final BlockingQueue<Object> reportQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private ThreadPoolExecutor reportExecutor;
    private volatile boolean running = false;

    @PostConstruct
    public void init() {
        if (!reportEnabled) {
            log.info("监控上报已禁用");
            return;
        }
        running = true;
        reportExecutor = new ThreadPoolExecutor(
                2, 4, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                r -> {
                    Thread t = new Thread(r, "monitor-reporter");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.DiscardOldestPolicy()
        );

        reportExecutor.submit(this::batchReportLoop);
        log.info("监控上报客户端已启动，目标服务: {}, 服务名: {}", monitorServerUrl, serviceName);
    }

    @PreDestroy
    public void destroy() {
        running = false;
        if (reportExecutor != null) {
            reportExecutor.shutdown();
            try {
                if (!reportExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    reportExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                reportExecutor.shutdownNow();
            }
        }
        flushRemaining();
    }

    public void reportRequestLog(RequestLog requestLog) {
        if (!reportEnabled || requestLog == null) return;
        if (requestLog.getServiceName() == null) {
            requestLog.setServiceName(serviceName);
        }
        enqueue(requestLog);
    }

    public void reportSlowSql(SlowSqlLog slowSqlLog) {
        if (!reportEnabled || slowSqlLog == null) return;
        enqueue(slowSqlLog);
    }

    public void reportPageVisit(PageVisitLog pageVisitLog) {
        if (!reportEnabled || pageVisitLog == null) return;
        enqueue(pageVisitLog);
    }

    private void enqueue(Object data) {
        if (reportQueue.offer(data)) {
            log.debug("监控数据已加入队列，当前队列大小: {}", reportQueue.size());
        } else {
            log.warn("监控上报队列已满，丢弃数据: {}", data);
        }
    }

    private void batchReportLoop() {
        while (running) {
            try {
                long start = System.currentTimeMillis();
                int count = 0;
                java.util.List<Object> batch = new java.util.ArrayList<>();

                while (count < batchSize && System.currentTimeMillis() - start < reportIntervalMs) {
                    Object item = reportQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (item != null) {
                        batch.add(item);
                        count++;
                    }
                    if (Thread.interrupted()) {
                        return;
                    }
                }

                if (!batch.isEmpty()) {
                    sendBatchReport(batch);
                }

            } catch (InterruptedException e) {
                log.info("监控上报线程被中断");
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("批量上报异常", e);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void sendBatchReport(java.util.List<Object> batch) {
        try {
            java.util.List<RequestLog> requestLogs = new java.util.ArrayList<>();
            java.util.List<SlowSqlLog> slowSqlLogs = new java.util.ArrayList<>();
            java.util.List<PageVisitLog> pageVisitLogs = new java.util.ArrayList<>();

            for (Object item : batch) {
                if (item instanceof RequestLog) {
                    requestLogs.add((RequestLog) item);
                } else if (item instanceof SlowSqlLog) {
                    slowSqlLogs.add((SlowSqlLog) item);
                } else if (item instanceof PageVisitLog) {
                    pageVisitLogs.add((PageVisitLog) item);
                }
            }

            if (!requestLogs.isEmpty()) {
                sendRequestLogs(requestLogs);
            }
            if (!slowSqlLogs.isEmpty()) {
                sendSlowSqlLogs(slowSqlLogs);
            }
            if (!pageVisitLogs.isEmpty()) {
                sendPageVisitLogs(pageVisitLogs);
            }

            log.debug("批量上报完成，请求日志: {}, 慢SQL: {}, PV日志: {}",
                    requestLogs.size(), slowSqlLogs.size(), pageVisitLogs.size());

        } catch (Exception e) {
            log.error("发送批量上报失败，数据条数: {}", batch.size(), e);
        }
    }

    private void sendRequestLogs(java.util.List<RequestLog> logs) throws Exception {
        String url = monitorServerUrl + "/monitor/report/request";
        sendPostRequest(url, logs);
    }

    private void sendSlowSqlLogs(java.util.List<SlowSqlLog> logs) throws Exception {
        String url = monitorServerUrl + "/monitor/report/slowSql";
        sendPostRequest(url, logs);
    }

    private void sendPageVisitLogs(java.util.List<PageVisitLog> logs) throws Exception {
        String url = monitorServerUrl + "/monitor/report/pageVisit";
        sendPostRequest(url, logs);
    }

    private void sendPostRequest(String url, Object body) throws Exception {
        String jsonBody = JSON.toJSONString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Service-Name", serviceName)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(3))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        log.debug("上报请求失败: {}", url, throwable);
                    } else if (response.statusCode() != 200) {
                        log.debug("上报请求异常，状态码: {}, URL: {}", response.statusCode(), url);
                    }
                });
    }

    private void flushRemaining() {
        if (reportQueue.isEmpty()) return;
        try {
            java.util.List<Object> remaining = new java.util.ArrayList<>();
            reportQueue.drainTo(remaining);
            if (!remaining.isEmpty()) {
                sendBatchReport(remaining);
                log.info("已刷清剩余监控数据: {} 条", remaining.size());
            }
        } catch (Exception e) {
            log.error("刷清剩余监控数据失败", e);
        }
    }

    public int getQueueSize() {
        return reportQueue.size();
    }

    public boolean isRunning() {
        return running;
    }
}
