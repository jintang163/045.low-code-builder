package com.lowcode.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.lowcode.common.monitor.entity.RequestLog;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.monitor.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

@Slf4j
@Component
public class RequestLogFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String traceId = request.getHeaders().getFirst(TraceIdUtil.TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdUtil.generateTraceId();
        }
        TraceIdUtil.setTraceId(traceId);

        String requestUrl = request.getURI().getPath();
        String requestMethod = request.getMethodValue();
        String userAgent = request.getHeaders().getFirst("User-Agent");
        String clientIp = getClientIp(request);
        Long userId = null;
        String username = null;
        String userIdHeader = request.getHeaders().getFirst("X-User-Id");
        String usernameHeader = request.getHeaders().getFirst("X-Username");
        if (userIdHeader != null && !userIdHeader.isEmpty()) {
            try {
                userId = Long.parseLong(userIdHeader);
            } catch (NumberFormatException ignored) {
            }
        }
        username = usernameHeader;

        String serviceName = "unknown";
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && route.getUri() != null) {
            serviceName = route.getUri().getHost();
        }

        RequestLog requestLog = new RequestLog();
        requestLog.setTraceId(traceId);
        requestLog.setRequestUrl(requestUrl);
        requestLog.setRequestMethod(requestMethod);
        requestLog.setRequestIp(clientIp);
        requestLog.setUserId(userId);
        requestLog.setUsername(username);
        requestLog.setServiceName(serviceName);
        requestLog.setUserAgent(userAgent);
        requestLog.setRequestTime(LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(TraceIdUtil.TRACE_ID, traceId)
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doOnSuccess(v -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    requestLog.setCostTime(costTime);
                    requestLog.setResponseStatus(exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value() : 200);
                    requestLog.setResponseTime(LocalDateTime.now());
                    MonitorDataStore.addRequestLog(requestLog);
                    log.info("[RequestLog] {} {} {} {}ms", requestMethod, requestUrl,
                            requestLog.getResponseStatus(), costTime);
                })
                .doOnError(e -> {
                    long costTime = System.currentTimeMillis() - startTime;
                    requestLog.setCostTime(costTime);
                    requestLog.setResponseStatus(500);
                    requestLog.setErrorStack(getStackTrace(e));
                    requestLog.setResponseTime(LocalDateTime.now());
                    MonitorDataStore.addRequestLog(requestLog);
                    log.error("[RequestLog] {} {} {}ms Error: {}", requestMethod, requestUrl,
                            costTime, e.getMessage(), e);
                });
    }

    private String getClientIp(ServerHttpRequest request) {
        String ip = request.getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            InetSocketAddress remoteAddress = request.getRemoteAddress();
            if (remoteAddress != null) {
                ip = remoteAddress.getAddress().getHostAddress();
            }
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

    @Override
    public int getOrder() {
        return -50;
    }
}
