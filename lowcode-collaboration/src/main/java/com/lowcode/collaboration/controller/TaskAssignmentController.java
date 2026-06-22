package com.lowcode.collaboration.controller;

import com.lowcode.collaboration.dto.TaskAssignmentDTO;
import com.lowcode.collaboration.dto.TaskUpdateDTO;
import com.lowcode.collaboration.entity.TaskAssignment;
import com.lowcode.collaboration.service.TaskAssignmentService;
import com.lowcode.common.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "任务分配")
@RestController
@RequestMapping("/api/collaboration/task")
public class TaskAssignmentController {

    @Autowired
    private TaskAssignmentService taskService;

    @ApiOperation("创建任务")
    @PostMapping
    public Result<TaskAssignment> create(@Validated @RequestBody TaskAssignmentDTO dto,
                                          @RequestParam Long appId,
                                          @RequestParam String targetType,
                                          @RequestParam Long targetId,
                                          @RequestParam(required = false) String targetName,
                                          @RequestParam(required = false) Long commentId) {
        return Result.success(taskService.createTask(dto, appId, targetType, targetId, targetName, commentId));
    }

    @ApiOperation("获取任务详情")
    @GetMapping("/{id}")
    public Result<TaskAssignment> getById(@PathVariable Long id) {
        return Result.success(taskService.getTaskById(id));
    }

    @ApiOperation("获取目标的任务列表")
    @GetMapping("/list")
    public Result<List<TaskAssignment>> listByTarget(@RequestParam Long appId,
                                                      @RequestParam String targetType,
                                                      @RequestParam Long targetId) {
        return Result.success(taskService.getTasksByTarget(appId, targetType, targetId));
    }

    @ApiOperation("获取指派给我的任务")
    @GetMapping("/mine")
    public Result<List<TaskAssignment>> listByAssignee(@RequestParam Long assigneeId) {
        return Result.success(taskService.getTasksByAssignee(assigneeId));
    }

    @ApiOperation("更新任务")
    @PutMapping("/{id}")
    public Result<TaskAssignment> update(@PathVariable Long id, @RequestBody TaskUpdateDTO dto) {
        return Result.success(taskService.updateTask(id, dto));
    }

    @ApiOperation("删除任务")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }
}
