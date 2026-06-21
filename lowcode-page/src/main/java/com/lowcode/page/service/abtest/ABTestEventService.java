package com.lowcode.page.service.abtest;

import com.lowcode.page.entity.abtest.ABTestEvent;

import java.util.List;
import java.util.Map;

public interface ABTestEventService {

    void recordEvent(ABTestEvent event);

    void batchRecordEvents(List<ABTestEvent> events);

    Map<String, Object> getEventStats(Long testId, Long variantId, String eventType, Long startTime, Long endTime);
}
