package com.lowcode.page.controller.abtest;

import com.lowcode.common.result.Result;
import com.lowcode.page.entity.abtest.ABTestEvent;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.service.abtest.ABTestAllocationService;
import com.lowcode.page.service.abtest.ABTestEventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "A/B测试事件管理")
@RestController
@RequestMapping("/api/abtest")
public class ABTestEventController {

    @Autowired
    private ABTestEventService abTestEventService;

    @Autowired
    private ABTestAllocationService abTestAllocationService;

    @ApiOperation("记录事件")
    @PostMapping("/event")
    public Result<Void> recordEvent(@RequestBody ABTestEvent event) {
        abTestEventService.recordEvent(event);
        return Result.success();
    }

    @ApiOperation("批量记录事件")
    @PostMapping("/events")
    public Result<Void> batchRecordEvents(@RequestBody List<ABTestEvent> events) {
        abTestEventService.batchRecordEvents(events);
        return Result.success();
    }

    @ApiOperation("获取事件统计")
    @GetMapping("/{testId}/events/stats")
    public Result<Map<String, Object>> getEventStats(@PathVariable Long testId,
                                                     @RequestParam(required = false) Long variantId,
                                                     @RequestParam(required = false) String eventType,
                                                     @RequestParam(required = false) Long startTime,
                                                     @RequestParam(required = false) Long endTime) {
        return Result.success(abTestEventService.getEventStats(testId, variantId, eventType, startTime, endTime));
    }

    @ApiOperation("获取用户分配的变体")
    @GetMapping("/allocate/{testId}")
    public Result<ABTestVariant> allocateVariant(@PathVariable Long testId,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String userGroup) {
        return Result.success(abTestAllocationService.allocateVariant(testId, userId, userGroup));
    }
}
