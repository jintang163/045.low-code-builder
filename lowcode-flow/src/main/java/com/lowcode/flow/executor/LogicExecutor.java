package com.lowcode.flow.executor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.flow.dto.CrossAppCallDTO;
import com.lowcode.flow.dto.RpaExecuteDTO;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.entity.LogicEdge;
import com.lowcode.flow.entity.LogicNode;
import com.lowcode.flow.entity.RpaExecution;
import com.lowcode.flow.service.BusinessLogicService;
import com.lowcode.flow.service.CrossAppService;
import com.lowcode.flow.service.RpaExecutionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class LogicExecutor {

    @Autowired
    private BusinessLogicService businessLogicService;

    @Autowired
    private RpaExecutionService rpaExecutionService;

    @Autowired
    private CrossAppService crossAppService;

    private final Map<String, Object> nodeHandlers = new ConcurrentHashMap<>();

    public Map<String, Object> executeLogic(Long logicId, Map<String, Object> inputParams) {
        BusinessLogic logic = businessLogicService.getLogicDetail(logicId);
        if (logic == null) {
            throw new BusinessException("业务逻辑不存在");
        }

        Map<String, Object> variables = new HashMap<>(inputParams != null ? inputParams : new HashMap<>());
        variables.put("_logicId", logicId);
        variables.put("_logicName", logic.getLogicName());
        variables.put("_startTime", LocalDateTime.now());

        List<LogicNode> nodes = logic.getNodes();
        List<LogicEdge> edges = logic.getEdges();

        if (nodes == null || nodes.isEmpty()) {
            throw new BusinessException("业务逻辑没有节点");
        }

        Map<String, LogicNode> nodeMap = new HashMap<>();
        for (LogicNode node : nodes) {
            nodeMap.put(node.getNodeId(), node);
        }

        Map<String, List<LogicEdge>> outgoingEdges = new HashMap<>();
        if (edges != null) {
            for (LogicEdge edge : edges) {
                outgoingEdges.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            }
        }

        LogicNode currentNode = findStartNode(nodes);
        Set<String> executedNodes = new HashSet<>();

        LocalDateTime startTime = LocalDateTime.now();
        log.info("开始执行业务逻辑: {} (ID: {})", logic.getLogicName(), logicId);

        try {
            while (currentNode != null) {
                if (executedNodes.contains(currentNode.getNodeId())) {
                    log.warn("检测到循环，跳过节点: {}", currentNode.getNodeId());
                    break;
                }

                executedNodes.add(currentNode.getNodeId());
                variables.put("_currentNodeId", currentNode.getNodeId());

                log.info("执行节点: {} [{}]", currentNode.getNodeName(), currentNode.getNodeType());

                NodeExecutionResult result = executeNode(currentNode, variables, logicId);

                if (!result.isSuccess()) {
                    throw new BusinessException("节点执行失败: " + result.getErrorMessage());
                }

                if (result.getOutput() != null) {
                    variables.putAll(result.getOutput());
                }

                List<LogicEdge> nextEdges = outgoingEdges.get(currentNode.getNodeId());
                if (nextEdges == null || nextEdges.isEmpty()) {
                    log.info("没有后续节点，流程结束");
                    break;
                }

                if (result.getNextNodeId() != null) {
                    currentNode = nodeMap.get(result.getNextNodeId());
                } else {
                    LogicEdge nextEdge = selectNextEdge(nextEdges, result);
                    if (nextEdge != null) {
                        currentNode = nodeMap.get(nextEdge.getTargetNodeId());
                    } else {
                        currentNode = null;
                    }
                }
            }

            Duration duration = Duration.between(startTime, LocalDateTime.now());
            variables.put("_duration", duration.toMillis());
            variables.put("_endTime", LocalDateTime.now());
            variables.put("_status", "SUCCESS");

            log.info("业务逻辑执行成功，耗时: {}ms", duration.toMillis());

            return variables;

        } catch (Exception e) {
            Duration duration = Duration.between(startTime, LocalDateTime.now());
            variables.put("_duration", duration.toMillis());
            variables.put("_endTime", LocalDateTime.now());
            variables.put("_status", "FAILED");
            variables.put("_error", e.getMessage());

            log.error("业务逻辑执行失败，耗时: {}ms", duration.toMillis(), e);
            throw new BusinessException("业务逻辑执行失败: " + e.getMessage(), e);
        }
    }

    private LogicNode findStartNode(List<LogicNode> nodes) {
        for (LogicNode node : nodes) {
            if ("START".equals(node.getNodeCategory()) || "SCHEDULE".equals(node.getNodeType())
                    || "API".equals(node.getNodeType()) || "MANUAL".equals(node.getNodeType())) {
                return node;
            }
        }
        return nodes.get(0);
    }

    private LogicEdge selectNextEdge(List<LogicEdge> edges, NodeExecutionResult result) {
        if (edges.size() == 1) {
            return edges.get(0);
        }

        for (LogicEdge edge : edges) {
            String condition = edge.getConditionExpression();
            if (condition == null || condition.isEmpty() || "true".equals(condition)) {
                return edge;
            }

            if (evaluateCondition(condition, result)) {
                return edge;
            }
        }

        return null;
    }

    private boolean evaluateCondition(String condition, NodeExecutionResult result) {
        try {
            if (result.isConditionResult() != null) {
                return result.isConditionResult();
            }
            return true;
        } catch (Exception e) {
            log.warn("条件评估失败: {}", condition, e);
            return false;
        }
    }

    private NodeExecutionResult executeNode(LogicNode node, Map<String, Object> variables, Long logicId) {
        NodeExecutionResult result = new NodeExecutionResult();
        result.setSuccess(true);

        String nodeType = node.getNodeType();
        if (nodeType == null) {
            nodeType = node.getNodeCategory();
        }

        try {
            switch (nodeType) {
                case "START":
                case "END":
                    result.setOutput(new HashMap<>());
                    break;

                case "SCHEDULE":
                case "API":
                case "TABLE_EVENT":
                case "MANUAL":
                    executeTriggerNode(node, variables, result);
                    break;

                case "CONDITION":
                    executeConditionNode(node, variables, result);
                    break;

                case "LOOP":
                    executeLoopNode(node, variables, result);
                    break;

                case "DELAY":
                    executeDelayNode(node, variables, result);
                    break;

                case "RPA_EXECUTE":
                case "RPA_EXTRACT":
                    executeRpaNode(node, variables, result, logicId);
                    break;

                case "ASSIGN":
                case "INCREMENT":
                case "DECREMENT":
                    executeVariableNode(node, variables, result);
                    break;

                case "QUERY":
                case "INSERT":
                case "UPDATE":
                case "DELETE":
                    executeDataOperationNode(node, variables, result);
                    break;

                case "EMAIL":
                case "SMS":
                case "IN_APP":
                case "WEBHOOK":
                case "DINGTALK":
                case "WECHAT":
                    executeNotificationNode(node, variables, result);
                    break;

                case "API_CALL":
                    executeApiCallNode(node, variables, result);
                    break;

                case "CROSS_APP_CALL":
                case "CROSS_APP_API":
                case "CROSS_APP_EVENT":
                    executeCrossAppNode(node, variables, result, logicId);
                    break;

                case "PARALLEL":
                case "SUB_LOGIC":
                case "WORKFLOW":
                case "TRANSACTION":
                case "CODE":
                    log.info("节点类型 {} 暂未完全实现，使用模拟执行", nodeType);
                    result.setOutput(new HashMap<>());
                    break;

                default:
                    log.warn("未知节点类型: {}", nodeType);
                    result.setOutput(new HashMap<>());
            }
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            log.error("节点执行失败: {}", node.getNodeName(), e);
        }

        return result;
    }

    private void executeTriggerNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        result.setOutput(new HashMap<>());
        log.info("触发器节点: {} [{}]", node.getNodeName(), node.getNodeType());
    }

    private void executeConditionNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        JSONObject config = parseConfig(node.getNodeConfig());
        String expression = config.getString("expression");

        if (expression != null && !expression.isEmpty()) {
            boolean conditionResult = evaluateExpression(expression, variables);
            result.setConditionResult(conditionResult);
            result.setOutput(Map.of("_conditionResult", conditionResult));
            log.info("条件节点 {} 评估结果: {}", node.getNodeName(), conditionResult);
        } else {
            result.setOutput(new HashMap<>());
        }
    }

    private void executeLoopNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        JSONObject config = parseConfig(node.getNodeConfig());
        String loopType = config.getString("loopType");
        result.setOutput(new HashMap<>());
        log.info("循环节点: {} [{}]", node.getNodeName(), loopType);
    }

    private void executeDelayNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        JSONObject config = parseConfig(node.getNodeConfig());
        int delaySeconds = config.getIntValue("delaySeconds");
        if (delaySeconds <= 0) {
            delaySeconds = 1;
        }

        try {
            log.info("延迟节点: {} 等待 {} 秒", node.getNodeName(), delaySeconds);
            Thread.sleep(delaySeconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        result.setOutput(new HashMap<>());
    }

    private void executeRpaNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result, Long logicId) {
        JSONObject config = parseConfig(node.getNodeConfig());

        Long scriptId = config.getLong("scriptId");
        if (scriptId == null) {
            throw new BusinessException("RPA节点缺少scriptId配置");
        }

        String resultVariable = config.getString("resultVariable");
        if (resultVariable == null || resultVariable.isEmpty()) {
            resultVariable = "rpaResult_" + node.getNodeId().replace("-", "_");
        }

        String inputParamsStr = config.getString("inputParams");
        Map<String, Object> rpaParams = new HashMap<>();
        if (inputParamsStr != null && !inputParamsStr.isEmpty()) {
            try {
                rpaParams = JSON.parseObject(inputParamsStr, Map.class);
            } catch (Exception e) {
                log.warn("解析RPA输入参数失败，使用空参数", e);
            }
        }

        rpaParams.putAll(variables);

        RpaExecuteDTO executeDTO = new RpaExecuteDTO();
        executeDTO.setScriptId(scriptId);
        executeDTO.setTriggerType("LOGIC");
        executeDTO.setTriggerLogicId(logicId);
        executeDTO.setTriggerNodeId(node.getNodeId());
        executeDTO.setInputParams(rpaParams);

        log.info("执行RPA节点: {}, scriptId={}", node.getNodeName(), scriptId);

        RpaExecution execution = rpaExecutionService.executeScript(executeDTO);

        Map<String, Object> rpaResult = new HashMap<>();
        if (execution.getOutputResult() != null) {
            try {
                rpaResult = JSON.parseObject(execution.getOutputResult(), Map.class);
            } catch (Exception e) {
                log.warn("解析RPA输出结果失败", e);
                rpaResult.put("rawOutput", execution.getOutputResult());
            }
        }

        rpaResult.put("_executionNo", execution.getExecutionNo());
        rpaResult.put("_status", execution.getStatus());
        rpaResult.put("_duration", execution.getDuration());

        result.setOutput(Map.of(resultVariable, rpaResult));

        log.info("RPA节点执行完成: {}, executionNo={}, status={}",
                node.getNodeName(), execution.getExecutionNo(), execution.getStatus());
    }

    private void executeVariableNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        JSONObject config = parseConfig(node.getNodeConfig());
        String variableName = config.getString("variableName");
        Object value = config.get("value");

        if (variableName != null) {
            Object resolvedValue = resolveValue(value, variables);
            variables.put(variableName, resolvedValue);
            result.setOutput(Map.of(variableName, resolvedValue));
            log.info("变量赋值: {} = {}", variableName, resolvedValue);
        } else {
            result.setOutput(new HashMap<>());
        }
    }

    private void executeDataOperationNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        result.setOutput(new HashMap<>());
        log.info("数据操作节点: {} [{}]", node.getNodeName(), node.getNodeType());
    }

    private void executeNotificationNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        result.setOutput(new HashMap<>());
        log.info("通知节点: {} [{}]", node.getNodeName(), node.getNodeType());
    }

    private void executeApiCallNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result) {
        result.setOutput(new HashMap<>());
        log.info("API调用节点: {}", node.getNodeName());
    }

    @SuppressWarnings("unchecked")
    private void executeCrossAppNode(LogicNode node, Map<String, Object> variables, NodeExecutionResult result, Long logicId) {
        JSONObject config = parseConfig(node.getNodeConfig());

        String targetAppCode = config.getString("targetAppCode");
        String callType = config.getString("callType");
        String targetCode = config.getString("targetCode");
        String resultVariable = config.getString("resultVariable");
        Integer timeoutMs = config.getInteger("timeoutMs");

        if (targetAppCode == null || targetAppCode.trim().isEmpty()) {
            throw new BusinessException("跨应用节点缺少targetAppCode配置");
        }
        if (targetCode == null || targetCode.trim().isEmpty()) {
            throw new BusinessException("跨应用节点缺少targetCode配置");
        }
        if (callType == null || callType.trim().isEmpty()) {
            callType = "API";
        }
        if (resultVariable == null || resultVariable.trim().isEmpty()) {
            resultVariable = "crossAppResult_" + node.getNodeId().replace("-", "_");
        }

        String paramsStr = config.getString("callParams");
        Map<String, Object> callParams = new HashMap<>();
        if (paramsStr != null && !paramsStr.trim().isEmpty()) {
            try {
                callParams = JSON.parseObject(paramsStr, Map.class);
            } catch (Exception e) {
                log.warn("解析跨应用调用参数失败，使用空参数", e);
            }
        }

        callParams = resolveParams(callParams, variables);

        CrossAppCallDTO dto = new CrossAppCallDTO();
        dto.setTargetAppCode(targetAppCode);
        dto.setCallType(callType);
        dto.setTargetCode(targetCode);
        dto.setParams(callParams);
        dto.setTimeoutMs(timeoutMs != null ? timeoutMs : 5000);
        dto.setCallerLogicId(logicId);
        dto.setCallerAppId(null);

        log.info("执行跨应用节点: {}, targetApp={}, targetCode={}, callType={}",
                node.getNodeName(), targetAppCode, targetCode, callType);

        Map<String, Object> callResult = crossAppService.executeCrossAppCall(dto);

        result.setOutput(Map.of(resultVariable, callResult));

        log.info("跨应用节点执行完成: {}, resultVariable={}", node.getNodeName(), resultVariable);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveParams(Map<String, Object> params, Map<String, Object> variables) {
        if (params == null || params.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Object> resolved = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            Object value = entry.getValue();
            resolved.put(entry.getKey(), resolveValue(value, variables));
        }
        return resolved;
    }

    private JSONObject parseConfig(String configStr) {
        if (configStr == null || configStr.isEmpty()) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(configStr);
        } catch (Exception e) {
            log.warn("解析节点配置失败: {}", configStr, e);
            return new JSONObject();
        }
    }

    private boolean evaluateExpression(String expression, Map<String, Object> variables) {
        try {
            String expr = expression;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (expr.contains(placeholder) && entry.getValue() != null) {
                    expr = expr.replace(placeholder, entry.getValue().toString());
                }
            }

            if (expr.contains("==")) {
                String[] parts = expr.split("==", 2);
                return safeEquals(parts[0].trim(), parts[1].trim());
            } else if (expr.contains("!=")) {
                String[] parts = expr.split("!=", 2);
                return !safeEquals(parts[0].trim(), parts[1].trim());
            } else if (expr.contains(">")) {
                String[] parts = expr.split(">", 2);
                return safeCompare(parts[0].trim(), parts[1].trim()) > 0;
            } else if (expr.contains("<")) {
                String[] parts = expr.split("<", 2);
                return safeCompare(parts[0].trim(), parts[1].trim()) < 0;
            }

            return "true".equalsIgnoreCase(expr);
        } catch (Exception e) {
            log.warn("表达式评估失败: {}", expression, e);
            return false;
        }
    }

    private boolean safeEquals(String a, String b) {
        if (a.equals(b)) return true;
        try {
            double da = Double.parseDouble(a);
            double db = Double.parseDouble(b);
            return Math.abs(da - db) < 0.0001;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int safeCompare(String a, String b) {
        try {
            double da = Double.parseDouble(a);
            double db = Double.parseDouble(b);
            return Double.compare(da, db);
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }

    private Object resolveValue(Object value, Map<String, Object> variables) {
        if (value instanceof String) {
            String strValue = (String) value;
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                if (strValue.equals(placeholder)) {
                    return entry.getValue();
                }
                if (strValue.contains(placeholder) && entry.getValue() != null) {
                    strValue = strValue.replace(placeholder, entry.getValue().toString());
                }
            }
            return strValue;
        }
        return value;
    }

    public static class NodeExecutionResult {
        private boolean success = true;
        private String errorMessage;
        private Map<String, Object> output;
        private String nextNodeId;
        private Boolean conditionResult;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public Map<String, Object> getOutput() { return output; }
        public void setOutput(Map<String, Object> output) { this.output = output; }
        public String getNextNodeId() { return nextNodeId; }
        public void setNextNodeId(String nextNodeId) { this.nextNodeId = nextNodeId; }
        public Boolean isConditionResult() { return conditionResult; }
        public void setConditionResult(Boolean conditionResult) { this.conditionResult = conditionResult; }
    }
}
