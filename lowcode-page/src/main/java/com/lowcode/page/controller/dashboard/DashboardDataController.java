package com.lowcode.page.controller.dashboard;

import com.lowcode.common.result.Result;
import com.lowcode.page.service.dashboard.DashboardDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Api(tags = "大屏数据查询")
@RestController
@RequestMapping("/api/dashboard/data")
public class DashboardDataController {

    @Autowired
    private DashboardDataService dashboardDataService;

    @ApiOperation("获取大屏全部数据")
    @GetMapping("/{dashboardId}")
    public Result<Map<String, Object>> getDashboardData(@PathVariable Long dashboardId) {
        return Result.success(dashboardDataService.getDashboardData(dashboardId));
    }

    @ApiOperation("获取大屏全部数据（带联动筛选）")
    @PostMapping("/{dashboardId}")
    public Result<Map<String, Object>> getDashboardDataWithFilters(
            @PathVariable Long dashboardId,
            @RequestBody(required = false) Map<String, Object> linkageFilters) {
        return Result.success(dashboardDataService.getDashboardData(dashboardId, linkageFilters));
    }

    @ApiOperation("获取单个组件数据")
    @GetMapping("/{dashboardId}/component/{componentId}")
    public Result<Map<String, Object>> getComponentData(
            @PathVariable Long dashboardId,
            @PathVariable String componentId) {
        return Result.success(dashboardDataService.getComponentDataByComponentId(dashboardId, componentId));
    }

    @ApiOperation("获取单个组件数据（带联动筛选）")
    @PostMapping("/{dashboardId}/component/{componentId}")
    public Result<Map<String, Object>> getComponentDataWithFilters(
            @PathVariable Long dashboardId,
            @PathVariable String componentId,
            @RequestBody(required = false) Map<String, Object> linkageFilters) {
        return Result.success(dashboardDataService.getComponentDataByComponentId(dashboardId, componentId, linkageFilters));
    }
}
