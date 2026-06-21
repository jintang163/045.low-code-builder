package com.lowcode.page.controller.report;

import com.lowcode.common.result.Result;
import com.lowcode.page.service.report.ReportDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Api(tags = "报表数据查询")
@RestController
@RequestMapping("/api/report/data")
public class ReportDataController {

    @Autowired
    private ReportDataService reportDataService;

    @ApiOperation("获取报表全部数据")
    @GetMapping("/{reportId}")
    public Result<Map<String, Object>> getReportData(@PathVariable Long reportId) {
        return Result.success(reportDataService.getReportData(reportId));
    }

    @ApiOperation("获取报表全部数据（带联动筛选）")
    @PostMapping("/{reportId}")
    public Result<Map<String, Object>> getReportDataWithFilters(
            @PathVariable Long reportId,
            @RequestBody(required = false) Map<String, Object> linkageFilters) {
        return Result.success(reportDataService.getReportData(reportId, linkageFilters));
    }

    @ApiOperation("获取单个组件数据")
    @GetMapping("/{reportId}/component/{componentId}")
    public Result<List<Map<String, Object>>> getComponentData(
            @PathVariable Long reportId,
            @PathVariable String componentId) {
        return Result.success(reportDataService.getComponentDataByComponentId(reportId, componentId));
    }

    @ApiOperation("获取单个组件数据（带联动筛选）")
    @PostMapping("/{reportId}/component/{componentId}")
    public Result<List<Map<String, Object>>> getComponentDataWithFilters(
            @PathVariable Long reportId,
            @PathVariable String componentId,
            @RequestBody(required = false) Map<String, Object> linkageFilters) {
        return Result.success(reportDataService.getComponentDataByComponentId(reportId, componentId, linkageFilters));
    }
}
