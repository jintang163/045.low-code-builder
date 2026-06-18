package com.lowcode.flow.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.flow.entity.WorkflowDefinition;
import com.lowcode.flow.mapper.WorkflowDefinitionMapper;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkflowService extends ServiceImpl<WorkflowDefinitionMapper, WorkflowDefinition> {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public List<WorkflowDefinition> getWorkflowList(Long appId) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinition::getAppId, appId);
        wrapper.orderByDesc(WorkflowDefinition::getCreatedTime);
        return list(wrapper);
    }

    public WorkflowDefinition getWorkflowDetail(Long id) {
        WorkflowDefinition workflow = getById(id);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }
        return workflow;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition saveWorkflow(WorkflowDefinition workflow) {
        LambdaQueryWrapper<WorkflowDefinition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkflowDefinition::getWorkflowCode, workflow.getWorkflowCode());
        wrapper.eq(WorkflowDefinition::getAppId, workflow.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("工作流编码已存在");
        }

        workflow.setVersion(1);
        workflow.setStatus("DRAFT");
        save(workflow);
        return workflow;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition updateWorkflow(WorkflowDefinition workflow) {
        WorkflowDefinition existing = getById(workflow.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }
        updateById(workflow);
        return getById(workflow.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteWorkflow(Long id) {
        WorkflowDefinition workflow = getById(id);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }

        if (workflow.getFlowableDeploymentId() != null) {
            try {
                repositoryService.deleteDeployment(workflow.getFlowableDeploymentId(), true);
            } catch (Exception e) {
                log.warn("删除Flowable部署失败: {}", e.getMessage());
            }
        }

        removeById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition deployWorkflow(Long id) {
        WorkflowDefinition workflow = getById(id);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }

        if (workflow.getBpmnXml() == null || workflow.getBpmnXml().isEmpty()) {
            throw new BusinessException("BPMN XML不能为空");
        }

        try {
            if (workflow.getFlowableDeploymentId() != null) {
                repositoryService.deleteDeployment(workflow.getFlowableDeploymentId(), true);
            }

            String resourceName = workflow.getWorkflowCode() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment()
                    .name(workflow.getWorkflowName())
                    .addString(resourceName, workflow.getBpmnXml())
                    .deploy();

            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            workflow.setFlowableDeploymentId(deployment.getId());
            workflow.setFlowableDefinitionId(processDefinition.getId());
            workflow.setStatus("DEPLOYED");
            workflow.setVersion(workflow.getVersion() + 1);
            updateById(workflow);

            log.info("工作流部署成功: {} -> {}", workflow.getWorkflowName(), deployment.getId());
            return workflow;
        } catch (Exception e) {
            log.error("工作流部署失败", e);
            throw new BusinessException("工作流部署失败: " + e.getMessage());
        }
    }

    public Map<String, Object> startProcessInstance(Long workflowId, Map<String, Object> variables) {
        WorkflowDefinition workflow = getById(workflowId);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }

        if (!"DEPLOYED".equals(workflow.getStatus())) {
            throw new BusinessException("工作流未部署");
        }

        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(
                    workflow.getFlowableDefinitionId(), variables);

            Map<String, Object> result = new HashMap<>();
            result.put("processInstanceId", processInstance.getId());
            result.put("businessKey", processInstance.getBusinessKey());
            result.put("status", processInstance.isEnded() ? "ENDED" : "RUNNING");

            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstance.getId())
                    .list();
            result.put("currentTasks", tasks.stream().map(this::convertTask).collect(Collectors.toList()));

            return result;
        } catch (Exception e) {
            log.error("启动流程实例失败", e);
            throw new BusinessException("启动流程失败: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> getTasks(String assignee) {
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(assignee)
                .orderByTaskCreateTime()
                .desc()
                .list();

        return tasks.stream().map(this::convertTask).collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void completeTask(String taskId, Map<String, Object> variables, String comment) {
        try {
            Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
            if (task == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
            }

            if (comment != null && !comment.isEmpty()) {
                taskService.addComment(taskId, task.getProcessInstanceId(), comment);
            }

            taskService.complete(taskId, variables);
            log.info("任务完成: {}", taskId);
        } catch (Exception e) {
            log.error("完成任务失败", e);
            throw new BusinessException("完成任务失败: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delegateTask(String taskId, String assignee) {
        try {
            taskService.delegateTask(taskId, assignee);
        } catch (Exception e) {
            log.error("委派任务失败", e);
            throw new BusinessException("委派任务失败: " + e.getMessage());
        }
    }

    public String generateBpmnXml(Long id) {
        WorkflowDefinition workflow = getById(id);
        if (workflow == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }

        BpmnModel model = new BpmnModel();

        Process process = new Process();
        process.setId(workflow.getWorkflowCode());
        process.setName(workflow.getWorkflowName());
        model.addProcess(process);

        StartEvent startEvent = new StartEvent();
        startEvent.setId("startEvent");
        startEvent.setName("开始");
        process.addFlowElement(startEvent);

        UserTask userTask = new UserTask();
        userTask.setId("approvalTask");
        userTask.setName("审批节点");
        userTask.setAssignee("${approver}");
        process.addFlowElement(userTask);

        EndEvent endEvent = new EndEvent();
        endEvent.setId("endEvent");
        endEvent.setName("结束");
        process.addFlowElement(endEvent);

        SequenceFlow flow1 = new SequenceFlow("startEvent", "approvalTask");
        flow1.setId("flow1");
        process.addFlowElement(flow1);

        SequenceFlow flow2 = new SequenceFlow("approvalTask", "endEvent");
        flow2.setId("flow2");
        process.addFlowElement(flow2);

        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(model);
        String bpmnXml = new String(xmlBytes, StandardCharsets.UTF_8);

        workflow.setBpmnXml(bpmnXml);
        updateById(workflow);

        return bpmnXml;
    }

    public Map<String, Object> parseBpmnXml(String bpmnXml) {
        try {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(
                    new InputStreamReader(new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8))));

            BpmnXMLConverter converter = new BpmnXMLConverter();
            BpmnModel model = converter.convertToBpmnModel(reader);

            Map<String, Object> result = new HashMap<>();
            List<Process> processes = model.getProcesses();
            if (!processes.isEmpty()) {
                Process process = processes.get(0);
                result.put("processId", process.getId());
                result.put("processName", process.getName());

                List<Map<String, Object>> nodes = new ArrayList<>();
                List<Map<String, Object>> edges = new ArrayList<>();

                for (FlowElement element : process.getFlowElements()) {
                    if (element instanceof FlowNode) {
                        Map<String, Object> node = new LinkedHashMap<>();
                        node.put("id", element.getId());
                        node.put("name", element.getName());
                        node.put("type", getNodeType(element));
                        nodes.add(node);
                    }
                    if (element instanceof SequenceFlow) {
                        SequenceFlow flow = (SequenceFlow) element;
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("id", flow.getId());
                        edge.put("source", flow.getSourceRef());
                        edge.put("target", flow.getTargetRef());
                        edge.put("condition", flow.getConditionExpression() != null ?
                                flow.getConditionExpression().getExpressionText() : null);
                        edges.add(edge);
                    }
                }

                result.put("nodes", nodes);
                result.put("edges", edges);
            }

            return result;
        } catch (Exception e) {
            log.error("解析BPMN XML失败", e);
            throw new BusinessException("解析BPMN XML失败: " + e.getMessage());
        }
    }

    public Map<String, Object> getProcessStatus(String processInstanceId) {
        Map<String, Object> result = new HashMap<>();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance != null) {
            result.put("processInstanceId", processInstance.getId());
            result.put("status", processInstance.isEnded() ? "ENDED" : "RUNNING");
            result.put("suspended", processInstance.isSuspended());

            List<Task> tasks = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .list();
            result.put("currentTasks", tasks.stream().map(this::convertTask).collect(Collectors.toList()));
        } else {
            result.put("status", "NOT_FOUND");
        }

        return result;
    }

    private String getNodeType(FlowElement element) {
        if (element instanceof StartEvent) return "START";
        if (element instanceof EndEvent) return "END";
        if (element instanceof UserTask) return "USER_TASK";
        if (element instanceof ServiceTask) return "SERVICE_TASK";
        if (element instanceof ExclusiveGateway) return "EXCLUSIVE_GATEWAY";
        if (element instanceof ParallelGateway) return "PARALLEL_GATEWAY";
        if (element instanceof ScriptTask) return "SCRIPT_TASK";
        if (element instanceof CallActivity) return "CALL_ACTIVITY";
        return "UNKNOWN";
    }

    private Map<String, Object> convertTask(Task task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", task.getId());
        map.put("name", task.getName());
        map.put("assignee", task.getAssignee());
        map.put("createTime", task.getCreateTime());
        map.put("dueDate", task.getDueDate());
        map.put("priority", task.getPriority());
        map.put("description", task.getDescription());
        map.put("processInstanceId", task.getProcessInstanceId());
        map.put("processDefinitionId", task.getProcessDefinitionId());
        return map;
    }

    public Map<String, Object> getNodeTypes() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> events = Arrays.asList(
                createNodeType("START", "开始事件", "EVENT", "流程开始"),
                createNodeType("END", "结束事件", "EVENT", "流程结束"),
                createNodeType("TIMER", "定时事件", "EVENT", "定时触发")
        );
        result.put("events", events);

        List<Map<String, Object>> tasks = Arrays.asList(
                createNodeType("USER_TASK", "用户任务", "TASK", "人工审批节点"),
                createNodeType("SERVICE_TASK", "服务任务", "TASK", "自动调用服务"),
                createNodeType("SCRIPT_TASK", "脚本任务", "TASK", "执行脚本"),
                createNodeType("CALL_ACTIVITY", "调用活动", "TASK", "调用子流程")
        );
        result.put("tasks", tasks);

        List<Map<String, Object>> gateways = Arrays.asList(
                createNodeType("EXCLUSIVE_GATEWAY", "排他网关", "GATEWAY", "条件分支"),
                createNodeType("PARALLEL_GATEWAY", "并行网关", "GATEWAY", "并行执行"),
                createNodeType("INCLUSIVE_GATEWAY", "包容网关", "GATEWAY", "多条件分支")
        );
        result.put("gateways", gateways);

        return result;
    }

    private Map<String, Object> createNodeType(String type, String name, String category, String description) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", type);
        node.put("name", name);
        node.put("category", category);
        node.put("description", description);
        return node;
    }

    public List<Map<String, Object>> getDeployedWorkflows() {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list();

        return definitions.stream().map(def -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", def.getId());
            map.put("key", def.getKey());
            map.put("name", def.getName());
            map.put("version", def.getVersion());
            map.put("deploymentId", def.getDeploymentId());
            map.put("suspended", def.isSuspended());
            return map;
        }).collect(Collectors.toList());
    }
}
