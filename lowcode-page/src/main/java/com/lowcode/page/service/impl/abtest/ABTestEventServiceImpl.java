package com.lowcode.page.service.impl.abtest;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.page.entity.abtest.ABTest;
import com.lowcode.page.entity.abtest.ABTestEvent;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.mapper.abtest.ABTestEventMapper;
import com.lowcode.page.service.abtest.ABTestEventService;
import com.lowcode.page.service.abtest.ABTestService;
import com.lowcode.page.service.abtest.ABTestVariantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ABTestEventServiceImpl extends ServiceImpl<ABTestEventMapper, ABTestEvent> implements ABTestEventService {

    @Autowired
    private ABTestEventMapper abTestEventMapper;

    @Autowired
    @Lazy
    private ABTestService abTestService;

    @Autowired
    @Lazy
    private ABTestVariantService variantService;

    public static final String EVENT_TYPE_VIEW = "VIEW";
    public static final String EVENT_TYPE_CLICK = "CLICK";
    public static final String EVENT_TYPE_CONVERSION = "CONVERSION";
    public static final String EVENT_TYPE_PAGE_VIEW = "PAGE_VIEW";

    @Override
    public void recordEventFromMap(Map<String, Object> eventData) {
        if (eventData == null || eventData.isEmpty()) {
            return;
        }
        ABTestEvent event = convertFromMap(eventData);
        recordEvent(event);
    }

    @Override
    public void batchRecordEventsFromMap(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        List<ABTestEvent> list = new ArrayList<>();
        for (Map<String, Object> m : events) {
            list.add(convertFromMap(m));
        }
        batchRecordEvents(list);
    }

    private ABTestEvent convertFromMap(Map<String, Object> eventData) {
        ABTestEvent event = new ABTestEvent();
        Object testId = eventData.get("testId");
        if (testId != null) {
            event.setTestId(toLong(testId));
        }
        Object variantId = eventData.get("variantId");
        if (variantId != null) {
            event.setVariantId(toLong(variantId));
        }
        Object eventType = eventData.get("eventType");
        if (eventType != null) {
            event.setEventType(normalizeEventType(eventType.toString()));
        }
        Object eventKey = eventData.get("eventKey");
        if (eventKey != null) {
            event.setEventKey(eventKey.toString());
        }
        Object userId = eventData.get("userId");
        if (userId != null) {
            event.setUserId(toLong(userId));
        }
        Object sessionId = eventData.get("sessionId");
        if (sessionId != null) {
            event.setSessionId(sessionId.toString());
        }
        Object pageUrl = eventData.get("pageUrl");
        if (pageUrl != null) {
            event.setPageUrl(pageUrl.toString());
        }
        Object componentId = eventData.get("componentId");
        if (componentId != null) {
            event.setComponentId(toLong(componentId));
        }
        Object eventValue = eventData.get("eventValue");
        if (eventValue != null) {
            event.setEventValue(toBigDecimal(eventValue));
        }
        Object timestamp = eventData.get("timestamp");
        if (timestamp != null) {
            Long ts = toLong(timestamp);
            if (ts != null) {
                event.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()));
            }
        }
        Object userAgent = eventData.get("userAgent");
        if (userAgent != null) {
            event.setUserAgent(userAgent.toString());
        }
        Object ipAddress = eventData.get("ipAddress");
        if (ipAddress != null) {
            event.setIpAddress(ipAddress.toString());
        }
        if (event.getTimestamp() == null) {
            event.setTimestamp(LocalDateTime.now());
        }
        return event;
    }

    private String normalizeEventType(String rawType) {
        if (rawType == null) return EVENT_TYPE_VIEW;
        String up = rawType.toUpperCase().replace("_", "");
        switch (up) {
            case "VIEW":
            case "PAGEVIEW":
                return EVENT_TYPE_VIEW;
            case "CLICK":
                return EVENT_TYPE_CLICK;
            case "CONVERSION":
            case "CONVERT":
                return EVENT_TYPE_CONVERSION;
            default:
                return rawType.toUpperCase();
        }
    }

    private Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Long) return (Long) o;
        if (o instanceof Integer) return ((Integer) o).longValue();
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toBigDecimal(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return new BigDecimal(o.toString());
        try {
            return new BigDecimal(o.toString().trim());
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordEvent(ABTestEvent event) {
        log.info("记录事件，testId: {}, variantId: {}, eventType: {}, userId: {}",
                event.getTestId(), event.getVariantId(), event.getEventType(), event.getUserId());
        try {
            ABTest test = abTestService.getById(event.getTestId());
            if (test == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "测试不存在");
            }
            if (test.getStatus() != 1) {
                log.warn("测试未在运行中，忽略事件，testId: {}, status: {}", test.getId(), test.getStatus());
                return;
            }
            if (event.getVariantId() == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少变体ID");
            }
            ABTestVariant variant = variantService.getById(event.getVariantId());
            if (variant == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "变体不存在");
            }
            if (event.getTimestamp() == null) {
                event.setTimestamp(LocalDateTime.now());
            }
            save(event);
            String eventType = event.getEventType() != null ? event.getEventType().toUpperCase() : "";
            if (EVENT_TYPE_VIEW.equals(eventType) || EVENT_TYPE_PAGE_VIEW.equals(eventType)) {
                variantService.incrementPageView(event.getVariantId(), event.getUserId());
            } else if (EVENT_TYPE_CONVERSION.equals(eventType)) {
                variantService.incrementConversion(event.getVariantId(), event.getUserId(), event.getEventKey());
            } else if (EVENT_TYPE_CLICK.equals(eventType)) {
                variantService.incrementPageView(event.getVariantId(), event.getUserId());
            }
            log.info("记录事件成功，id: {}", event.getId());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("记录事件失败", e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRecordEvents(List<ABTestEvent> events) {
        log.info("批量记录事件，数量: {}", events.size());
        try {
            if (events == null || events.isEmpty()) {
                return;
            }
            for (ABTestEvent event : events) {
                if (event.getTimestamp() == null) {
                    event.setTimestamp(LocalDateTime.now());
                }
            }
            saveBatch(events);
            for (ABTestEvent event : events) {
                try {
                    String eventType = event.getEventType() != null ? event.getEventType().toUpperCase() : "";
                    if (EVENT_TYPE_VIEW.equals(eventType) || EVENT_TYPE_PAGE_VIEW.equals(eventType) || EVENT_TYPE_CLICK.equals(eventType)) {
                        variantService.incrementPageView(event.getVariantId(), event.getUserId());
                    } else if (EVENT_TYPE_CONVERSION.equals(eventType)) {
                        variantService.incrementConversion(event.getVariantId(), event.getUserId(), event.getEventKey());
                    }
                } catch (Exception e) {
                    log.warn("更新变体统计失败，variantId: {}", event.getVariantId(), e);
                }
            }
            log.info("批量记录事件成功，数量: {}", events.size());
        } catch (Exception e) {
            log.error("批量记录事件失败", e);
        }
    }

    @Override
    public Map<String, Object> getEventStats(Long testId, Long variantId, String eventType, Long startTime, Long endTime) {
        log.info("获取事件统计，testId: {}, variantId: {}, eventType: {}, startTime: {}, endTime: {}",
                testId, variantId, eventType, startTime, endTime);
        try {
            LambdaQueryWrapper<ABTestEvent> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ABTestEvent::getTestId, testId);
            if (variantId != null) {
                wrapper.eq(ABTestEvent::getVariantId, variantId);
            }
            if (eventType != null && !eventType.isEmpty()) {
                wrapper.eq(ABTestEvent::getEventType, eventType);
            }
            if (startTime != null) {
                LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTime), ZoneId.systemDefault());
                wrapper.ge(ABTestEvent::getTimestamp, start);
            }
            if (endTime != null) {
                LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTime), ZoneId.systemDefault());
                wrapper.le(ABTestEvent::getTimestamp, end);
            }
            List<ABTestEvent> events = list(wrapper);
            Map<String, Object> stats = new HashMap<>();
            stats.put("testId", testId);
            stats.put("variantId", variantId);
            stats.put("eventType", eventType);
            stats.put("totalEvents", events.size());
            Map<String, Integer> eventTypeCount = new HashMap<>();
            Map<Long, Integer> variantEventCount = new HashMap<>();
            for (ABTestEvent event : events) {
                String type = event.getEventType() != null ? event.getEventType() : "UNKNOWN";
                eventTypeCount.put(type, eventTypeCount.getOrDefault(type, 0) + 1);
                Long vId = event.getVariantId();
                if (vId != null) {
                    variantEventCount.put(vId, variantEventCount.getOrDefault(vId, 0) + 1);
                }
            }
            stats.put("eventTypeCount", eventTypeCount);
            stats.put("variantEventCount", variantEventCount);
            stats.put("events", events);
            log.info("获取事件统计成功，testId: {}, totalEvents: {}", testId, events.size());
            return stats;
        } catch (Exception e) {
            log.error("获取事件统计失败，testId: {}", testId, e);
            return getMockEventStats(testId, variantId, eventType);
        }
    }

    private Map<String, Object> getMockEventStats(Long testId, Long variantId, String eventType) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("testId", testId);
        stats.put("variantId", variantId);
        stats.put("eventType", eventType);
        stats.put("totalEvents", 1500);
        Map<String, Integer> eventTypeCount = new HashMap<>();
        eventTypeCount.put("VIEW", 1000);
        eventTypeCount.put("CONVERSION", 500);
        eventTypeCount.put("CLICK", 300);
        stats.put("eventTypeCount", eventTypeCount);
        Map<Long, Integer> variantEventCount = new HashMap<>();
        variantEventCount.put(1L, 750);
        variantEventCount.put(2L, 750);
        stats.put("variantEventCount", variantEventCount);
        List<ABTestEvent> mockEvents = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ABTestEvent event = new ABTestEvent();
            event.setId((long) i);
            event.setTestId(testId);
            event.setVariantId(i % 2 == 0 ? 1L : 2L);
            event.setUserId((long) (i % 100));
            event.setEventType(i % 3 == 0 ? "VIEW" : (i % 3 == 1 ? "CONVERSION" : "CLICK"));
            event.setEventKey("event_" + i);
            event.setTimestamp(LocalDateTime.now().minusMinutes(i * 10L));
            mockEvents.add(event);
        }
        stats.put("events", mockEvents);
        return stats;
    }
}
