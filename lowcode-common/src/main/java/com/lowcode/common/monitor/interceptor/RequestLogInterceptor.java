package com.lowcode.common.monitor.interceptor;

import com.lowcode.common.monitor.entity.RequestLog;
import com.lowcode.common.monitor.report.MonitorReportClient;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.monitor.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;

@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "request_start_time";
    private static final String REQUEST_LOG_ATTR = "request_log";

    @Autowired(required = false)
    @Lazy
    private MonitorReportClient reportClient;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String traceId = request.getHeader(TraceIdUtil.TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdUtil.generateTraceId();
        }
        TraceIdUtil.setTraceId(traceId);
        response.setHeader(TraceIdUtil.TRACE_ID, traceId);

        RequestLog requestLog = new RequestLog();
        requestLog.setTraceId(traceId);
        requestLog.setRequestUrl(request.getRequestURI());
        requestLog.setRequestMethod(request.getMethod());
        requestLog.setRequestIp(getClientIp(request));
        requestLog.setUserAgent(request.getHeader("User-Agent"));
        requestLog.setRequestTime(LocalDateTime.now());

        String userIdHeader = request.getHeader("X-User-Id");
        String usernameHeader = request.getHeader("X-Username");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                requestLog.setUserId(Long.parseLong(userIdHeader));
            } catch (NumberFormatException ignored) {
            }
        }
        if (usernameHeader != null && !usernameHeader.isEmpty()) {
            requestLog.setUsername(usernameHeader);
        }

        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        request.setAttribute(REQUEST_LOG_ATTR, requestLog);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        try {
            RequestLog requestLog = (RequestLog) request.getAttribute(REQUEST_LOG_ATTR);
            Long startTime = (Long) request.getAttribute(START_TIME_ATTR);

            if (requestLog != null && startTime != null) {
                long costTime = System.currentTimeMillis() - startTime;
                requestLog.setCostTime(costTime);
                requestLog.setResponseStatus(response.getStatus());
                requestLog.setResponseTime(LocalDateTime.now());

                if (ex != null) {
                    requestLog.setErrorStack(getStackTrace(ex));
                    if (response.getStatus() < 400) {
                        requestLog.setResponseStatus(500);
                    }
                }

                if (isErrorRequest(requestLog.getResponseStatus())) {
                    requestLog.setErrorStack(requestLog.getErrorStack() != null
                            ? requestLog.getErrorStack()
                            : "HTTP Error: " + response.getStatus());
                }

                MonitorDataStore.addRequestLog(requestLog);

                if (reportClient != null) {
                    reportClient.reportRequestLog(requestLog);
                }

                org.slf4j.Logger reqLogger = org.slf4j.LoggerFactory.getLogger("REQUEST_LOG");
                reqLogger.info("{} {} {} {}ms",
                        requestLog.getRequestMethod(),
                        requestLog.getRequestUrl(),
                        requestLog.getResponseStatus(),
                        costTime);
            }
        } finally {
            TraceIdUtil.removeTraceId();
        }
    }

    private boolean isErrorRequest(Integer status) {
        return status != null && status >= 500;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 5000) break;
        }
        return sb.toString();
    }
}
