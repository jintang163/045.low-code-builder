package com.lowcode.flow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.result.Result;
import com.lowcode.flow.entity.WorkflowDefinition;
import com.lowcode.flow.service.WorkflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "工作流管理")
@RestController
@RequestMapping("/workflow")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @ApiOperation("保存工作流")
    @PostMapping
    public Result<WorkflowDefinition> save(@RequestBody WorkflowDefinition workflow) {
        return Result.success(workflowService.saveWorkflow(workflow));
    }

    @ApiOperation("更新工作流")
    @PutMapping
    public Result<WorkflowDefinition> update(@RequestBody WorkflowDefinition workflow) {
        return Result.success(workflowService.updateWorkflow(workflow));
    }

    @ApiOperation("删除工作流")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return Result.success();
    }

    @ApiOperation("获取工作流详情")
    @GetMapping("/{id}")
    public Result<WorkflowDefinition> getById(@PathVariable Long id) {
        return Result.success(workflowService.getWorkflowDetail(id));
    }

    @ApiOperation("获取工作流列表")
    @GetMapping("/list/{appId}")
    public Result<List<WorkflowDefinition>> list(@PathVariable Long appId) {
        return Result.success(workflowService.getWorkflowList(appId));
    }

    @ApiOperation("分页查询工作流")
    @GetMapping("/page")
    public Result<Page<WorkflowDefinition>> page(@RequestParam(defaultValue = "1") Integer current,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam Long appId) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinition::getAppId, appId);
        wrapper.orderByDesc(WorkflowDefinition::getCreatedTime);
        Page<WorkflowDefinition> page = workflowService.page(new Page<>(current, size), wrapper);
        return Result.success(page);
    }

    @ApiOperation("部署工作流")
    @PostMapping("/{id}/deploy")
    public Result<WorkflowDefinition> deploy(@PathVariable Long id) {
        return Result.success(workflowService.deployWorkflow(id));
    }

    @ApiOperation("启动流程实例")
    @PostMapping("/{id}/start")
    public Result<Map<String, Object>> startProcess(@PathVariable Long id,
                                                    @RequestBody(required = false) Map<String, Object> variables) {
        return Result.success(workflowService.startProcessInstance(id, variables));
    }

    @ApiOperation("获取我的待办任务")
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> getTasks(@RequestParam String assignee) {
        return Result.success(workflowService.getTasks(assignee));
    }

    @ApiOperation("完成任务")
    @PostMapping("/task/{taskId}/complete")
    public Result<Void> completeTask(@PathVariable String taskId,
                                     @RequestBody(required = false) Map<String, Object> variables,
                                     @RequestParam(required = false) String comment) {
        workflowService.completeTask(taskId, variables, comment);
        return Result.success();
    }

    @ApiOperation("委派任务")
    @PostMapping("/task/{taskId}/delegate")
    public Result<Void> delegateTask(@PathVariable String taskId,
                                     @RequestParam String assignee) {
        workflowService.delegateTask(taskId, assignee);
        return Result.success();
    }

    @ApiOperation("生成BPMN XML")
    @GetMapping("/{id}/generateBpmn")
    public Result<String> generateBpmnXml(@PathVariable Long id) {
        return Result.success(workflowService.generateBpmnXml(id));
    }

    @ApiOperation("解析BPMN XML")
    @PostMapping("/parseBpmn")
    public Result<Map<String, Object>> parseBpmnXml(@RequestBody String bpmnXml) {
        return Result.success(workflowService.parseBpmnXml(bpmnXml));
    }

    @ApiOperation("获取流程状态")
    @GetMapping("/process/{processInstanceId}/status")
    public Result<Map<String, Object>> getProcessStatus(@PathVariable String processInstanceId) {
        return Result.success(workflowService.getProcessStatus(processInstanceId));
    }

    @ApiOperation("获取工作流节点类型")
    @GetMapping("/nodeTypes")
    public Result<Map<String, Object>> getNodeTypes() {
        return Result.success(workflowService.getNodeTypes());
    }

    @ApiOperation("获取已部署的工作流")
    @GetMapping("/deployed")
    public Result<List<Map<String, Object>>> getDeployedWorkflows() {
        return Result.success(workflowService.getDeployedWorkflows());
    }
}
