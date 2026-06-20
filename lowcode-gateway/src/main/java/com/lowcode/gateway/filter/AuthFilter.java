package com.lowcode.gateway.filter;

import com.alibaba.fastjson2.JSON;
import com.lowcode.common.monitor.entity.RequestLog;
import com.lowcode.common.monitor.report.MonitorReportClient;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.monitor.util.TraceIdUtil;
import com.lowcode.common.result.Result;
import com.lowcode.gateway.feign.AuthFeignClient;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String SECRET = "lowcode-platform-secret-key-2024-abcdefghijklmnopqrstuvwxyz";

    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/validate",
            "/api/monitor",
            "/api/deploy/presets",
            "/v2/api-docs",
            "/v3/api-docs",
            "/swagger-resources",
            "/swagger-ui",
            "/doc.html",
            "/webjars",
            "/favicon.ico",
            "/ws/deploy"
    );

    @Autowired(required = false)
    private AuthFeignClient authFeignClient;

    @Autowired(required = false)
    private MonitorReportClient reportClient;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        String traceId = request.getHeaders().getFirst(TraceIdUtil.TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = TraceIdUtil.generateTraceId();
        }
        TraceIdUtil.setTraceId(traceId);

        String serviceName = "lowcode-gateway";
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null && route.getUri() != null) {
            serviceName = route.getUri().getHost();
        }

        RequestLog requestLog = new RequestLog();
        requestLog.setTraceId(traceId);
        requestLog.setRequestUrl(path);
        requestLog.setRequestMethod(request.getMethodValue());
        requestLog.setRequestIp(getClientIp(request));
        requestLog.setUserAgent(request.getHeaders().getFirst("User-Agent"));
        requestLog.setServiceName(serviceName);
        requestLog.setRequestTime(LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        if (isWhiteList(path)) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header(TraceIdUtil.TRACE_ID, traceId)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
                    .doFinally(s -> {
                        completeRequestLog(requestLog, startTime, exchange);
                        TraceIdUtil.removeTraceId();
                    });
        }

        String token = request.getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            completeRequestLog(requestLog, startTime, exchange, 401, "未提供认证Token");
            TraceIdUtil.removeTraceId();
            return unauthorized(exchange, "未提供认证Token");
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                completeRequestLog(requestLog, startTime, exchange, 401, "Token无效或已过期");
                TraceIdUtil.removeTraceId();
                return unauthorized(exchange, "Token无效或已过期");
            }

            Long userId = claims.get("userId", Long.class);
            String username = claims.get("username", String.class);

            requestLog.setUserId(userId);
            requestLog.setUsername(username);

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-Username", username)
                    .header(TraceIdUtil.TRACE_ID, traceId)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build())
                    .doFinally(s -> {
                        completeRequestLog(requestLog, startTime, exchange);
                        TraceIdUtil.removeTraceId();
                    });

        } catch (Exception e) {
            log.error("Token验证失败", e);
            completeRequestLog(requestLog, startTime, exchange, 401, e.getMessage());
            TraceIdUtil.removeTraceId();
            return unauthorized(exchange, "Token验证失败: " + e.getMessage());
        }
    }

    private void completeRequestLog(RequestLog requestLog, long startTime, ServerWebExchange exchange) {
        Integer status = exchange.getResponse().getStatusCode() != null
                ? exchange.getResponse().getStatusCode().value() : 200;
        completeRequestLog(requestLog, startTime, exchange, status, null);
    }

    private void completeRequestLog(RequestLog requestLog, long startTime, ServerWebExchange exchange,
                                    int status, String errorMsg) {
        long costTime = System.currentTimeMillis() - startTime;
        requestLog.setCostTime(costTime);
        requestLog.setResponseStatus(status);
        requestLog.setResponseTime(LocalDateTime.now());
        if (errorMsg != null) {
            requestLog.setErrorStack(errorMsg);
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

    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Result<?> result = Result.fail(401, message);
        String json = JSON.toJSONString(result);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Claims parseToken(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            return null;
        }
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

    @Override
    public int getOrder() {
        return -100;
    }
}
