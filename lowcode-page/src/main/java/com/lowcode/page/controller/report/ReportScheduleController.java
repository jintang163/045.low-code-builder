package com.lowcode.page.controller.report;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.report.ReportSchedule;
import com.lowcode.page.service.report.ReportScheduleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Api(tags = "报表定时任务")
@RestController
@RequestMapping("/api/report/schedule")
public class ReportScheduleController {

    @Autowired
    private ReportScheduleService reportScheduleService;

    @ApiOperation("保存定时任务")
    @PostMapping
    public Result<ReportSchedule> save(@RequestBody ReportSchedule schedule) {
        return Result.success(reportScheduleService.saveSchedule(schedule));
    }

    @ApiOperation("更新定时任务")
    @PutMapping
    public Result<ReportSchedule> update(@RequestBody ReportSchedule schedule) {
        return Result.success(reportScheduleService.updateSchedule(schedule));
    }

    @ApiOperation("删除定时任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        reportScheduleService.deleteSchedule(id);
        return Result.success();
    }

    @ApiOperation("获取定时任务详情")
    @GetMapping("/{id}")
    public Result<ReportSchedule> getById(@PathVariable Long id) {
        return Result.success(reportScheduleService.getScheduleDetail(id));
    }

    @ApiOperation("获取定时任务列表")
    @GetMapping("/list/{reportId}")
    public Result<List<ReportSchedule>> list(@PathVariable Long reportId) {
        return Result.success(reportScheduleService.getScheduleList(reportId));
    }

    @ApiOperation("分页查询定时任务")
    @GetMapping("/page")
    public Result<Page<ReportSchedule>> page(@RequestParam(defaultValue = "1") Integer current,
                                             @RequestParam(defaultValue = "10") Integer size,
                                             @RequestParam(required = false) Long reportId,
                                             @RequestParam(required = false) String keyword) {
        return Result.success(reportScheduleService.getSchedulePage(current, size, reportId, keyword));
    }

    @ApiOperation("启用定时任务")
    @PostMapping("/{id}/enable")
    public Result<ReportSchedule> enable(@PathVariable Long id) {
        return Result.success(reportScheduleService.enableSchedule(id));
    }

    @ApiOperation("禁用定时任务")
    @PostMapping("/{id}/disable")
    public Result<ReportSchedule> disable(@PathVariable Long id) {
        return Result.success(reportScheduleService.disableSchedule(id));
    }

    @ApiOperation("立即执行定时任务")
    @PostMapping("/{id}/execute")
    public Result<Void> execute(@PathVariable Long id) {
        reportScheduleService.executeSchedule(id);
        return Result.success();
    }
}
