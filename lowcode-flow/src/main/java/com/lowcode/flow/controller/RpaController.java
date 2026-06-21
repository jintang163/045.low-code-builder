package com.lowcode.flow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.flow.dto.RpaExecuteDTO;
import com.lowcode.flow.dto.RpaScheduleDTO;
import com.lowcode.flow.dto.RpaScriptCreateDTO;
import com.lowcode.flow.entity.RpaExecution;
import com.lowcode.flow.entity.RpaScript;
import com.lowcode.flow.rpa.RpaExecutorClient;
import com.lowcode.flow.schedule.RpaScheduleService;
import com.lowcode.flow.service.RpaExecutionService;
import com.lowcode.flow.service.RpaScriptService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "RPA自动化管理")
@RestController
@RequestMapping("/api/rpa")
public class RpaController {

    @Autowired
    private RpaScriptService rpaScriptService;

    @Autowired
    private RpaExecutionService rpaExecutionService;

    @Autowired
    private RpaExecutorClient rpaExecutorClient;

    @Autowired
    private RpaScheduleService rpaScheduleService;

    @ApiOperation("获取RPA脚本列表")
    @GetMapping("/script/list/{appId}")
    public Result<List<RpaScript>> listScripts(@PathVariable Long appId) {
        return Result.success(rpaScriptService.getScriptList(appId));
    }

    @ApiOperation("分页查询RPA脚本")
    @GetMapping("/script/page")
    public Result<Page<RpaScript>> pageScripts(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam Long appId) {
        return Result.success(rpaScriptService.getScriptPage(appId, current, size));
    }

    @ApiOperation("获取RPA脚本详情")
    @GetMapping("/script/{id}")
    public Result<RpaScript> getScript(@PathVariable Long id) {
        return Result.success(rpaScriptService.getScriptDetail(id));
    }

    @ApiOperation("创建RPA脚本")
    @PostMapping("/script")
    public Result<RpaScript> createScript(@RequestBody RpaScriptCreateDTO dto) {
        return Result.success(rpaScriptService.createScript(dto));
    }

    @ApiOperation("更新RPA脚本")
    @PutMapping("/script/{id}")
    public Result<RpaScript> updateScript(@PathVariable Long id, @RequestBody RpaScriptCreateDTO dto) {
        return Result.success(rpaScriptService.updateScript(id, dto));
    }

    @ApiOperation("删除RPA脚本")
    @DeleteMapping("/script/{id}")
    public Result<Void> deleteScript(@PathVariable Long id) {
        rpaScriptService.deleteScript(id);
        return Result.success();
    }

    @ApiOperation("验证RPA脚本")
    @PostMapping("/script/{id}/validate")
    public Result<String> validateScript(@PathVariable Long id) {
        String error = rpaScriptService.validateScript(id);
        if (error != null) {
            return Result.error(500, error);
        }
        return Result.success("脚本验证通过");
    }

    @ApiOperation("发布RPA脚本")
    @PostMapping("/script/{id}/publish")
    public Result<RpaScript> publishScript(@PathVariable Long id) {
        return Result.success(rpaScriptService.publishScript(id));
    }

    @ApiOperation("执行RPA脚本")
    @PostMapping("/execute")
    public Result<RpaExecution> executeScript(@RequestBody RpaExecuteDTO dto) {
        return Result.success(rpaExecutionService.executeScript(dto));
    }

    @ApiOperation("获取RPA执行记录详情")
    @GetMapping("/execution/{id}")
    public Result<RpaExecution> getExecution(@PathVariable Long id) {
        return Result.success(rpaExecutionService.getExecutionDetail(id));
    }

    @ApiOperation("分页查询RPA执行记录")
    @GetMapping("/execution/page")
    public Result<Page<RpaExecution>> pageExecutions(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long scriptId) {
        return Result.success(rpaExecutionService.getExecutionPage(scriptId, current, size));
    }

    @ApiOperation("检查RPA执行器健康状态")
    @GetMapping("/executor/health")
    public Result<Map<String, Object>> checkExecutorHealth() {
        boolean healthy = rpaExecutorClient.checkHealth();
        Map<String, Object> result = Map.of(
                "status", healthy ? "UP" : "DOWN",
                "message", healthy ? "RPA执行器运行正常" : "RPA执行器不可用"
        );
        return Result.success(result);
    }

    @ApiOperation("启用RPA脚本定时调度")
    @PostMapping("/script/{id}/schedule/enable")
    public Result<RpaScript> enableSchedule(@PathVariable Long id, @RequestBody RpaScheduleDTO dto) {
        return Result.success(rpaScheduleService.enableSchedule(id, dto.getCronExpression(), dto.getScheduleParams()));
    }

    @ApiOperation("禁用RPA脚本定时调度")
    @PostMapping("/script/{id}/schedule/disable")
    public Result<RpaScript> disableSchedule(@PathVariable Long id) {
        return Result.success(rpaScheduleService.disableSchedule(id));
    }

    @ApiOperation("获取定时执行的RPA脚本列表")
    @GetMapping("/schedule/list")
    public Result<List<RpaScript>> getScheduledScripts() {
        return Result.success(rpaScheduleService.getScheduledScripts());
    }

    @ApiOperation("计算Cron表达式下次执行时间")
    @GetMapping("/schedule/next-execution")
    public Result<Map<String, Object>> calculateNextExecution(@RequestParam String cronExpression) {
        LocalDateTime nextTime = rpaScheduleService.calculateNextExecuteTime(cronExpression);
        Map<String, Object> result = new HashMap<>();
        result.put("cronExpression", cronExpression);
        result.put("nextExecutionTime", nextTime);
        result.put("valid", nextTime != null);
        return Result.success(result);
    }
}
