package com.lowcode.common.monitor.util;

import cn.hutool.core.util.IdUtil;
import org.slf4j.MDC;

public class TraceIdUtil {

    public static final String TRACE_ID = "traceId";

    public static String generateTraceId() {
        return IdUtil.fastSimpleUUID();
    }

    public static String getTraceId() {
        String traceId = MDC.get(TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
            MDC.put(TRACE_ID, traceId);
        }
        return traceId;
    }

    public static void setTraceId(String traceId) {
        if (traceId == null || traceId.isEmpty()) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID, traceId);
    }

    public static void removeTraceId() {
        MDC.remove(TRACE_ID);
    }
}
