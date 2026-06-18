package com.lowcode.flow.controller;

import com.lowcode.common.result.Result;
import com.lowcode.flow.entity.LogicDebug;
import com.lowcode.flow.service.LogicDebugService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "逻辑调试")
@RestController
@RequestMapping("/api/debug")
public class LogicDebugController {

    @Autowired
    private LogicDebugService logicDebugService;

    @ApiOperation("开始调试")
    @PostMapping("/start")
    public Result<LogicDebug> startDebug(@RequestParam Long logicId,
                                         @RequestBody(required = false) Map<String, Object> inputParams) {
        return Result.success(logicDebugService.startDebug(logicId, inputParams));
    }

    @ApiOperation("单步执行")
    @PostMapping("/stepForward")
    public Result<LogicDebug> stepForward(@RequestParam String sessionId) {
        return Result.success(logicDebugService.stepForward(sessionId));
    }

    @ApiOperation("获取调试状态")
    @GetMapping("/status")
    public Result<LogicDebug> getDebugStatus(@RequestParam String sessionId) {
        return Result.success(logicDebugService.getDebugStatus(sessionId));
    }

    @ApiOperation("停止调试")
    @PostMapping("/stop")
    public Result<LogicDebug> stopDebug(@RequestParam String sessionId) {
        return Result.success(logicDebugService.stopDebug(sessionId));
    }

    @ApiOperation("设置断点")
    @PostMapping("/breakpoint/set")
    public Result<LogicDebug> setBreakpoint(@RequestParam String sessionId,
                                            @RequestParam String nodeId) {
        return Result.success(logicDebugService.setBreakpoint(sessionId, nodeId));
    }

    @ApiOperation("移除断点")
    @PostMapping("/breakpoint/remove")
    public Result<LogicDebug> removeBreakpoint(@RequestParam String sessionId,
                                               @RequestParam String nodeId) {
        return Result.success(logicDebugService.removeBreakpoint(sessionId, nodeId));
    }

    @ApiOperation("获取调试历史")
    @GetMapping("/history/{logicId}")
    public Result<List<LogicDebug>> getDebugHistory(@PathVariable Long logicId) {
        return Result.success(logicDebugService.getDebugHistory(logicId));
    }
}
