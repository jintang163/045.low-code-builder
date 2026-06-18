package com.lowcode.flow.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
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
        wrapper.eq(WorkflowDefinition::getProcessKey, workflow.getProcessKey());
        wrapper.eq(WorkflowDefinition::getAppId, workflow.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException("工作流编码已存在");
        }

        if (workflow.getVersion() == null) workflow.setVersion(1);
        if (workflow.getStatus() == null) workflow.setStatus(0);
        save(workflow);
        return workflow;
    }

    @Transactional(rollbackFor = Exception.class)
    public WorkflowDefinition updateWorkflow(WorkflowDefinition workflow) {
        WorkflowDefinition existing = getById(workflow.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工作流不存在");
        }
        workflow.setStatus(0);
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

        String standardBpmnXml = generateBpmnXml(id);
        if (standardBpmnXml == null || standardBpmnXml.isEmpty()) {
            throw new BusinessException("BPMN XML不能为空");
        }

        try {
            if (workflow.getFlowableDeploymentId() != null) {
                repositoryService.deleteDeployment(workflow.getFlowableDeploymentId(), true);
            }

            String resourceName = workflow.getProcessKey() + ".bpmn20.xml";
            Deployment deployment = repositoryService.createDeployment()
                    .name(workflow.getProcessName())
                    .addString(resourceName, standardBpmnXml)
                    .deploy();

            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult();

            workflow.setFlowableDeploymentId(deployment.getId());
            workflow.setFlowableProcessDefId(processDefinition.getId());
            workflow.setStatus(1);
            workflow.setVersion(workflow.getVersion() == null ? 1 : workflow.getVersion() + 1);
            workflow.setDeployTime(java.time.LocalDateTime.now());
            updateById(workflow);

            log.info("工作流部署成功: {} -> {}", workflow.getProcessName(), deployment.getId());
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

        if (workflow.getStatus() == null || workflow.getStatus() != 1) {
            throw new BusinessException("工作流未部署");
        }

        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceById(
                    workflow.getFlowableProcessDefId(), variables);

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

        String storedData = workflow.getBpmnXml();

        if (storedData == null || storedData.isEmpty()) {
            return generateDefaultBpmn(workflow);
        }

        if (storedData.trim().startsWith("<?xml") || storedData.trim().startsWith("<bpmn")) {
            return storedData;
        }

        try {
            JSONObject json = JSON.parseObject(storedData);
            JSONArray nodes = json.getJSONArray("nodes");
            JSONArray edges = json.getJSONArray("edges");

            if (nodes == null || nodes.isEmpty()) {
                return generateDefaultBpmn(workflow);
            }

            return convertDesignerToBpmn(workflow, nodes, edges);
        } catch (Exception e) {
            log.warn("解析设计器数据失败，使用默认BPMN: {}", e.getMessage());
            return generateDefaultBpmn(workflow);
        }
    }

    private String convertDesignerToBpmn(WorkflowDefinition workflow, JSONArray nodes, JSONArray edges) {
        BpmnModel model = new BpmnModel();

        Process process = new Process();
        process.setId(workflow.getProcessKey());
        process.setName(workflow.getProcessName());
        if (workflow.getProcessDesc() != null) {
            process.setDocumentation(workflow.getProcessDesc());
        }
        model.addProcess(process);

        Map<String, FlowElement> elementMap = new HashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            String nodeId = node.getString("nodeId");
            String nodeName = node.getString("nodeName");
            String nodeType = node.getString("nodeType");
            String configStr = node.getString("nodeConfig");

            JSONObject config = configStr != null && !configStr.isEmpty()
                    ? JSON.parseObject(configStr) : new JSONObject();

            FlowElement element = createFlowElement(nodeType, nodeId, nodeName, config);
            if (element != null) {
                process.addFlowElement(element);
                elementMap.put(nodeId, element);
            }
        }

        boolean hasStart = nodes.stream()
                .map(n -> ((JSONObject) n).getString("nodeType"))
                .anyMatch(t -> t != null && (t.equals("START_EVENT") || t.equals("START")));
        boolean hasEnd = nodes.stream()
                .map(n -> ((JSONObject) n).getString("nodeType"))
                .anyMatch(t -> t != null && (t.equals("END_EVENT") || t.equals("END")));

        if (!hasStart) {
            StartEvent startEvent = new StartEvent();
            startEvent.setId("auto_start_" + System.currentTimeMillis());
            startEvent.setName("开始");
            process.addFlowElement(startEvent);
        }
        if (!hasEnd) {
            EndEvent endEvent = new EndEvent();
            endEvent.setId("auto_end_" + System.currentTimeMillis());
            endEvent.setName("结束");
            process.addFlowElement(endEvent);
        }

        if (edges != null) {
            for (int i = 0; i < edges.size(); i++) {
                JSONObject edge = edges.getJSONObject(i);
                String edgeId = edge.getString("edgeId");
                String sourceId = edge.getString("sourceNodeId");
                String targetId = edge.getString("targetNodeId");
                String condition = edge.getString("conditionExpression");

                if (sourceId == null || targetId == null) continue;

                SequenceFlow sequenceFlow = new SequenceFlow(sourceId, targetId);
                sequenceFlow.setId(edgeId != null ? edgeId : "flow_" + i);
                if (condition != null && !condition.trim().isEmpty()) {
                    FormalExpression expression = new FormalExpression();
                    expression.setExpression(condition);
                    sequenceFlow.setConditionExpression(expression);
                }
                process.addFlowElement(sequenceFlow);
            }
        }

        BpmnXMLConverter converter = new BpmnXMLConverter();
        byte[] xmlBytes = converter.convertToXML(model);
        String bpmnXml = new String(xmlBytes, StandardCharsets.UTF_8);

        workflow.setBpmnXml(bpmnXml);
        updateById(workflow);

        return bpmnXml;
    }

    private FlowElement createFlowElement(String nodeType, String nodeId, String nodeName, JSONObject config) {
        if (nodeType == null) return null;

        switch (nodeType) {
            case "START_EVENT":
            case "START":
            case "TIMER_START_EVENT": {
                StartEvent start = new StartEvent();
                start.setId(nodeId);
                start.setName(nodeName);
                if ("TIMER_START_EVENT".equals(nodeType)) {
                    TimerEventDefinition timer = new TimerEventDefinition();
                    String cycle = config.getString("timerCycle");
                    String date = config.getString("timerDate");
                    String duration = config.getString("timerDuration");
                    if (cycle != null) timer.setTimeCycle(cycle);
                    else if (date != null) timer.setTimeDate(date);
                    else if (duration != null) timer.setTimeDuration(duration);
                    else timer.setTimeCycle("R3/PT10M");
                    start.addEventDefinition(timer);
                }
                return start;
            }
            case "END_EVENT":
            case "END":
            case "TERMINATE_END_EVENT": {
                EndEvent end = new EndEvent();
                end.setId(nodeId);
                end.setName(nodeName);
                if ("TERMINATE_END_EVENT".equals(nodeType)) {
                    TerminateEventDefinition terminate = new TerminateEventDefinition();
                    end.addEventDefinition(terminate);
                }
                return end;
            }
            case "USER_TASK": {
                UserTask userTask = new UserTask();
                userTask.setId(nodeId);
                userTask.setName(nodeName);
                String assignee = config.getString("assignee");
                String candidateUsers = config.getString("candidateUsers");
                String candidateGroups = config.getString("candidateGroups");
                if (assignee != null && !assignee.isEmpty()) {
                    userTask.setAssignee(assignee);
                } else {
                    userTask.setAssignee("${assignee}");
                }
                if (candidateUsers != null && !candidateUsers.isEmpty()) {
                    userTask.setCandidateUsers(Arrays.asList(candidateUsers.split(",")));
                }
                if (candidateGroups != null && !candidateGroups.isEmpty()) {
                    userTask.setCandidateGroups(Arrays.asList(candidateGroups.split(",")));
                }
                return userTask;
            }
            case "SERVICE_TASK": {
                ServiceTask serviceTask = new ServiceTask();
                serviceTask.setId(nodeId);
                serviceTask.setName(nodeName);
                String expression = config.getString("expression");
                String delegateExpression = config.getString("delegateExpression");
                String className = config.getString("className");
                if (expression != null && !expression.isEmpty()) {
                    serviceTask.setExpression(expression);
                } else if (delegateExpression != null && !delegateExpression.isEmpty()) {
                    serviceTask.setDelegateExpression(delegateExpression);
                } else if (className != null && !className.isEmpty()) {
                    serviceTask.setImplementation(className);
                } else {
                    serviceTask.setExpression("${serviceTask.execute()}");
                }
                return serviceTask;
            }
            case "SCRIPT_TASK": {
                ScriptTask scriptTask = new ScriptTask();
                scriptTask.setId(nodeId);
                scriptTask.setName(nodeName);
                String scriptFormat = config.getString("scriptFormat");
                String script = config.getString("script");
                scriptTask.setScriptFormat(scriptFormat != null && !scriptFormat.isEmpty() ? scriptFormat : "groovy");
                scriptTask.setScript(script != null && !script.isEmpty() ? script : "return null;");
                return scriptTask;
            }
            case "BUSINESS_RULE_TASK": {
                BusinessRuleTask ruleTask = new BusinessRuleTask();
                ruleTask.setId(nodeId);
                ruleTask.setName(nodeName);
                String ruleKeys = config.getString("ruleKeys");
                if (ruleKeys != null) {
                    ruleTask.setRuleKeys(Arrays.asList(ruleKeys.split(",")));
                }
                return ruleTask;
            }
            case "SEND_TASK": {
                SendTask sendTask = new SendTask();
                sendTask.setId(nodeId);
                sendTask.setName(nodeName);
                return sendTask;
            }
            case "RECEIVE_TASK": {
                ReceiveTask receiveTask = new ReceiveTask();
                receiveTask.setId(nodeId);
                receiveTask.setName(nodeName);
                return receiveTask;
            }
            case "MANUAL_TASK": {
                ManualTask manualTask = new ManualTask();
                manualTask.setId(nodeId);
                manualTask.setName(nodeName);
                return manualTask;
            }
            case "CALL_ACTIVITY": {
                CallActivity callActivity = new CallActivity();
                callActivity.setId(nodeId);
                callActivity.setName(nodeName);
                String calledElement = config.getString("calledElement");
                callActivity.setCalledElement(calledElement != null && !calledElement.isEmpty() ? calledElement : "subProcess");
                return callActivity;
            }
            case "EXCLUSIVE_GATEWAY":
            case "GATEWAY": {
                ExclusiveGateway gateway = new ExclusiveGateway();
                gateway.setId(nodeId);
                gateway.setName(nodeName);
                return gateway;
            }
            case "PARALLEL_GATEWAY": {
                ParallelGateway gateway = new ParallelGateway();
                gateway.setId(nodeId);
                gateway.setName(nodeName);
                return gateway;
            }
            case "INCLUSIVE_GATEWAY": {
                InclusiveGateway gateway = new InclusiveGateway();
                gateway.setId(nodeId);
                gateway.setName(nodeName);
                return gateway;
            }
            case "EVENT_BASED_GATEWAY": {
                EventGateway gateway = new EventGateway();
                gateway.setId(nodeId);
                gateway.setName(nodeName);
                return gateway;
            }
            case "SUB_PROCESS": {
                SubProcess subProcess = new SubProcess();
                subProcess.setId(nodeId);
                subProcess.setName(nodeName);
                return subProcess;
            }
            case "TIMER_INTERMEDIATE_EVENT":
            case "MESSAGE_INTERMEDIATE_EVENT":
            case "SIGNAL_INTERMEDIATE_EVENT":
            case "BOUNDARY_TIMER_EVENT":
            case "BOUNDARY_ERROR_EVENT": {
                ThrowEvent intermediateEvent = new ThrowEvent();
                intermediateEvent.setId(nodeId);
                intermediateEvent.setName(nodeName);
                return intermediateEvent;
            }
            default: {
                log.warn("未知的节点类型: {}，使用默认UserTask代替", nodeType);
                UserTask userTask = new UserTask();
                userTask.setId(nodeId);
                userTask.setName(nodeName);
                userTask.setAssignee("${assignee}");
                return userTask;
            }
        }
    }

    private String generateDefaultBpmn(WorkflowDefinition workflow) {
        BpmnModel model = new BpmnModel();

        Process process = new Process();
        process.setId(workflow.getProcessKey());
        process.setName(workflow.getProcessName());
        if (workflow.getProcessDesc() != null) {
            process.setDocumentation(workflow.getProcessDesc());
        }
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
                        node.put("nodeId", element.getId());
                        node.put("nodeName", element.getName());
                        node.put("nodeType", getNodeType(element));
                        node.put("id", element.getId());
                        nodes.add(node);
                    }
                    if (element instanceof SequenceFlow) {
                        SequenceFlow flow = (SequenceFlow) element;
                        Map<String, Object> edge = new LinkedHashMap<>();
                        edge.put("edgeId", flow.getId());
                        edge.put("sourceNodeId", flow.getSourceRef());
                        edge.put("targetNodeId", flow.getTargetRef());
                        edge.put("id", flow.getId());
                        edge.put("conditionExpression", flow.getConditionExpression() != null ?
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
        if (element instanceof StartEvent) {
            StartEvent se = (StartEvent) element;
            if (!se.getEventDefinitions().isEmpty()) {
                if (se.getEventDefinitions().get(0) instanceof TimerEventDefinition) {
                    return "TIMER_START_EVENT";
                }
            }
            return "START_EVENT";
        }
        if (element instanceof EndEvent) return "END_EVENT";
        if (element instanceof UserTask) return "USER_TASK";
        if (element instanceof ServiceTask) return "SERVICE_TASK";
        if (element instanceof ScriptTask) return "SCRIPT_TASK";
        if (element instanceof BusinessRuleTask) return "BUSINESS_RULE_TASK";
        if (element instanceof SendTask) return "SEND_TASK";
        if (element instanceof ReceiveTask) return "RECEIVE_TASK";
        if (element instanceof ManualTask) return "MANUAL_TASK";
        if (element instanceof CallActivity) return "CALL_ACTIVITY";
        if (element instanceof ExclusiveGateway) return "EXCLUSIVE_GATEWAY";
        if (element instanceof ParallelGateway) return "PARALLEL_GATEWAY";
        if (element instanceof InclusiveGateway) return "INCLUSIVE_GATEWAY";
        if (element instanceof EventGateway) return "EVENT_BASED_GATEWAY";
        if (element instanceof SubProcess) return "SUB_PROCESS";
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
                createNodeType("START_EVENT", "开始事件", "EVENT", "流程开始"),
                createNodeType("END_EVENT", "结束事件", "EVENT", "流程结束"),
                createNodeType("TIMER_START_EVENT", "定时启动", "EVENT", "定时触发"),
                createNodeType("TIMER_INTERMEDIATE_EVENT", "定时事件", "EVENT", "中间定时"),
                createNodeType("MESSAGE_INTERMEDIATE_EVENT", "消息事件", "EVENT", "中间消息")
        );
        result.put("events", events);

        List<Map<String, Object>> tasks = Arrays.asList(
                createNodeType("USER_TASK", "用户任务", "TASK", "人工审批节点"),
                createNodeType("SERVICE_TASK", "服务任务", "TASK", "自动调用服务"),
                createNodeType("SCRIPT_TASK", "脚本任务", "TASK", "执行脚本"),
                createNodeType("BUSINESS_RULE_TASK", "规则任务", "TASK", "执行业务规则"),
                createNodeType("SEND_TASK", "发送任务", "TASK", "发送消息"),
                createNodeType("RECEIVE_TASK", "接收任务", "TASK", "接收消息"),
                createNodeType("MANUAL_TASK", "手动任务", "TASK", "无需系统参与"),
                createNodeType("CALL_ACTIVITY", "调用活动", "TASK", "调用子流程")
        );
        result.put("tasks", tasks);

        List<Map<String, Object>> gateways = Arrays.asList(
                createNodeType("EXCLUSIVE_GATEWAY", "排他网关", "GATEWAY", "条件分支"),
                createNodeType("PARALLEL_GATEWAY", "并行网关", "GATEWAY", "并行执行"),
                createNodeType("INCLUSIVE_GATEWAY", "包容网关", "GATEWAY", "多条件分支"),
                createNodeType("EVENT_BASED_GATEWAY", "事件网关", "GATEWAY", "事件选择")
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
