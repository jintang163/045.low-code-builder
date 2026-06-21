package com.lowcode.page.controller.abtest;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.abtest.ABTest;
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
                               @RequestParam Long winnerVariantId) {
        return Result.success(abTestService.stopTest(winnerVariantId));
    }

    @ApiOperation("获取统计数据")
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable Long id) {
        return Result.success(abTestService.getTestStats(id));
    }
}
