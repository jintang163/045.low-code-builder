package com.lowcode.page.controller.abtest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.abtest.ABTest;
import com.lowcode.page.entity.abtest.ABTestVariant;
import com.lowcode.page.service.abtest.ABTestAllocationService;
import com.lowcode.page.service.abtest.ABTestEventService;
import com.lowcode.page.service.abtest.ABTestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "A/B测试管理")
@RestController
@RequestMapping("/api/abtest")
public class ABTestController {

    @Autowired
    private ABTestService abTestService;

    @Autowired
    private ABTestEventService abTestEventService;

    @Autowired
    private ABTestAllocationService abTestAllocationService;

    @ApiOperation("获取测试详情")
    @GetMapping("/{id}")
    public Result<ABTest> getById(@PathVariable Long id) {
        return Result.success(abTestService.getTestDetail(id));
    }

    @ApiOperation("获取测试列表")
    @GetMapping("/list/{appId}")
    public Result<List<ABTest>> list(@PathVariable Long appId,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(abTestService.getTestList(appId, keyword));
    }

    @ApiOperation("分页查询测试")
    @GetMapping("/page")
    public Result<Page<ABTest>> page(@RequestParam(defaultValue = "1") Integer current,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam Long appId,
                                     @RequestParam(required = false) String keyword,
                                     @RequestParam(required = false) Integer status) {
        return Result.success(abTestService.getTestPage(current, size, appId, keyword, status));
    }

    @ApiOperation("保存测试")
    @PostMapping
    public Result<ABTest> save(@RequestBody ABTest test) {
        return Result.success(abTestService.saveTest(test));
    }

    @ApiOperation("更新测试")
    @PutMapping
    public Result<ABTest> update(@RequestBody ABTest test) {
        return Result.success(abTestService.updateTest(test));
    }

    @ApiOperation("删除测试")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        abTestService.deleteTest(id);
        return Result.success();
    }

    @ApiOperation("开始测试")
    @PostMapping("/{id}/start")
    public Result<ABTest> start(@PathVariable Long id) {
        return Result.success(abTestService.startTest(id));
    }

    @ApiOperation("暂停测试")
    @PostMapping("/{id}/pause")
    public Result<ABTest> pause(@PathVariable Long id) {
        return Result.success(abTestService.pauseTest(id));
    }

    @ApiOperation("结束测试")
    @PostMapping("/{id}/stop")
    public Result<ABTest> stop(@PathVariable Long id,
                               @RequestParam(required = false) Long winnerVariantId) {
        return Result.success(abTestService.stopTest(id, winnerVariantId));
    }

    @ApiOperation("推广优胜版本")
    @PostMapping("/{id}/promote/{variantId}")
    public Result<ABTest> promoteWinner(@PathVariable Long id,
                                        @PathVariable Long variantId) {
        return Result.success(abTestService.promoteWinner(id, variantId));
    }

    @ApiOperation("获取统计数据")
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(abTestService.getTestStats(id));
    }

    @ApiOperation("获取置信区间与显著性")
    @GetMapping("/{id}/confidence")
    public Result<Map<String, Object>> confidence(@PathVariable Long id) {
        return Result.success(abTestService.calculateConfidence(id));
    }

    @ApiOperation("获取用户分配的变体")
    @GetMapping("/{testId}/allocate")
    public Result<ABTestVariant> allocateVariant(@PathVariable Long testId,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) String userGroup) {
        return Result.success(abTestAllocationService.allocateVariant(testId, userId, userGroup));
    }

    @ApiOperation("记录事件")
    @PostMapping("/event")
    public Result<Void> recordEvent(@RequestBody Map<String, Object> eventData) {
        abTestEventService.recordEventFromMap(eventData);
        return Result.success();
    }

    @ApiOperation("批量记录事件")
    @PostMapping("/events")
    public Result<Void> batchRecordEvents(@RequestBody List<Map<String, Object>> events) {
        abTestEventService.batchRecordEventsFromMap(events);
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
}
