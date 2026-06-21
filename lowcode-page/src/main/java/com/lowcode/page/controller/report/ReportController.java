package com.lowcode.page.controller.report;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.report.Report;
import com.lowcode.page.service.report.ReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "报表管理")
@RestController
@RequestMapping("/api/report")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @ApiOperation("保存报表")
    @PostMapping
    public Result<Report> save(@RequestBody Report report) {
        return Result.success(reportService.saveReport(report));
    }

    @ApiOperation("更新报表")
    @PutMapping
    public Result<Report> update(@RequestBody Report report) {
        return Result.success(reportService.updateReport(report));
    }

    @ApiOperation("删除报表")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportService.deleteReport(id);
        return Result.success();
    }

    @ApiOperation("获取报表详情")
    @GetMapping("/{id}")
    public Result<Report> getById(@PathVariable Long id) {
        return Result.success(reportService.getReportDetail(id));
    }

    @ApiOperation("获取报表列表")
    @GetMapping("/list/{appId}")
    public Result<List<Report>> list(@PathVariable Long appId) {
        return Result.success(reportService.getReportList(appId));
    }

    @ApiOperation("分页查询报表")
    @GetMapping("/page")
    public Result<Page<Report>> page(@RequestParam(defaultValue = "1") Integer current,
                                     @RequestParam(defaultValue = "10") Integer size,
                                     @RequestParam Long appId,
                                     @RequestParam(required = false) String keyword) {
        return Result.success(reportService.getReportPage(current, size, appId, keyword));
    }

    @ApiOperation("发布报表")
    @PostMapping("/{id}/publish")
    public Result<Report> publish(@PathVariable Long id) {
        return Result.success(reportService.publishReport(id));
    }

    @ApiOperation("复制报表")
    @PostMapping("/copy/{id}")
    public Result<Report> copy(@PathVariable Long id,
                               @RequestParam(required = false) String newName,
                               @RequestParam(required = false) String newCode) {
        return Result.success(reportService.copyReport(id, newName, newCode));
    }
}
