package com.lowcode.flow.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.entity.LogicDebug;
import com.lowcode.flow.entity.LogicEdge;
import com.lowcode.flow.entity.LogicNode;
import com.lowcode.flow.mapper.LogicDebugMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
public class LogicDebugService extends ServiceImpl<LogicDebugMapper, LogicDebug> {

    @Autowired
    private BusinessLogicService logicService;

    public LogicDebug startDebug(Long logicId, Map<String, Object> inputParams) {
        BusinessLogic logic = logicService.getLogicDetail(logicId);
        if (logic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "业务逻辑不存在");
        }

        LogicNode startNode = findStartNode(logic.getNodes());
        if (startNode == null) {
            throw new BusinessException("流程没有开始节点");
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");

        LogicDebug debug = new LogicDebug();
        debug.setLogicId(logicId);
        debug.setDebugSessionId(sessionId);
        debug.setCurrentNodeId(startNode.getNodeId());
        debug.setStepIndex(0);
        debug.setStatus("RUNNING");

        Map<String, Object> variables = new HashMap<>();
        variables.put("input", inputParams);
        variables.put("output", new HashMap<String, Object>());
        debug.setVariableSnapshot(JSON.toJSONString(variables));

        StringBuilder executionLog = new StringBuilder();
        executionLog.append("[").append(new Date()).append("] 开始调试会话: ").append(sessionId).append("\n");
        executionLog.append("[").append(new Date()).append("] 输入参数: ").append(JSON.toJSONString(inputParams)).append("\n");
        executionLog.append("[").append(new Date()).append("] 当前节点: ").append(startNode.getNodeName()).append(" (").append(startNode.getNodeId()).append(")\n");
        debug.setExecutionLog(executionLog.toString());

        save(debug);
        return debug;
    }

    public LogicDebug stepForward(String sessionId) {
        LogicDebug debug = getBySessionId(sessionId);
        if (debug == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "调试会话不存在");
        }

        if (!"RUNNING".equals(debug.getStatus())) {
            throw new BusinessException("调试会话已结束");
        }

        BusinessLogic logic = logicService.getLogicDetail(debug.getLogicId());
        LogicNode currentNode = findNodeById(logic.getNodes(), debug.getCurrentNodeId());
        if (currentNode == null) {
            throw new BusinessException("当前节点不存在");
        }

        Map<String, Object> variables = JSON.parseObject(debug.getVariableSnapshot(), Map.class);
        StringBuilder executionLog = new StringBuilder(debug.getExecutionLog());

        executionLog.append("[").append(new Date()).append("] 执行节点: ").append(currentNode.getNodeName()).append("\n");

        executeNode(currentNode, variables, executionLog);

        List<LogicEdge> outEdges = findOutEdges(logic.getEdges(), currentNode.getNodeId());
        if (outEdges.isEmpty() || "END".equals(currentNode.getNodeCategory())) {
            debug.setStatus("COMPLETED");
            executionLog.append("[").append(new Date()).append("] 流程执行完成\n");
        } else {
            LogicEdge nextEdge = selectNextEdge(outEdges, variables);
            LogicNode nextNode = findNodeById(logic.getNodes(), nextEdge.getTargetNodeId());
            if (nextNode != null) {
                debug.setCurrentNodeId(nextNode.getNodeId());
                debug.setStepIndex(debug.getStepIndex() + 1);
                executionLog.append("[").append(new Date()).append("] 下一节点: ").append(nextNode.getNodeName()).append("\n");
            } else {
                debug.setStatus("ERROR");
                executionLog.append("[").append(new Date()).append("] 错误: 找不到下一节点\n");
            }
        }

        debug.setVariableSnapshot(JSON.toJSONString(variables));
        debug.setExecutionLog(executionLog.toString());
        updateById(debug);

        return debug;
    }

    public LogicDebug getDebugStatus(String sessionId) {
        return getBySessionId(sessionId);
    }

    public LogicDebug stopDebug(String sessionId) {
        LogicDebug debug = getBySessionId(sessionId);
        if (debug == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "调试会话不存在");
        }

        debug.setStatus("STOPPED");
        StringBuilder executionLog = new StringBuilder(debug.getExecutionLog());
        executionLog.append("[").append(new Date()).append("] 调试会话已停止\n");
        debug.setExecutionLog(executionLog.toString());
        updateById(debug);

        return debug;
    }

    @Transactional(rollbackFor = Exception.class)
    public LogicDebug setBreakpoint(String sessionId, String nodeId) {
        LogicDebug debug = getBySessionId(sessionId);
        if (debug == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "调试会话不存在");
        }

        Set<String> breakpoints;
        if (debug.getBreakpoints() != null && !debug.getBreakpoints().isEmpty()) {
            breakpoints = new HashSet<>(Arrays.asList(debug.getBreakpoints().split(",")));
        } else {
            breakpoints = new HashSet<>();
        }
        breakpoints.add(nodeId);
        debug.setBreakpoints(String.join(",", breakpoints));
        updateById(debug);

        return debug;
    }

    @Transactional(rollbackFor = Exception.class)
    public LogicDebug removeBreakpoint(String sessionId, String nodeId) {
        LogicDebug debug = getBySessionId(sessionId);
        if (debug == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "调试会话不存在");
        }

        if (debug.getBreakpoints() != null && !debug.getBreakpoints().isEmpty()) {
            Set<String> breakpoints = new HashSet<>(Arrays.asList(debug.getBreakpoints().split(",")));
            breakpoints.remove(nodeId);
            debug.setBreakpoints(breakpoints.isEmpty() ? null : String.join(",", breakpoints));
            updateById(debug);
        }

        return debug;
    }

    private LogicDebug getBySessionId(String sessionId) {
        LambdaQueryWrapper<LogicDebug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogicDebug::getDebugSessionId, sessionId);
        wrapper.orderByDesc(LogicDebug::getCreatedTime);
        List<LogicDebug> list = list(wrapper);
        return list.isEmpty() ? null : list.get(0);
    }

    private LogicNode findStartNode(List<LogicNode> nodes) {
        if (nodes == null) return null;
        for (LogicNode node : nodes) {
            if ("START".equals(node.getNodeCategory())) {
                return node;
            }
        }
        return nodes.isEmpty() ? null : nodes.get(0);
    }

    private LogicNode findNodeById(List<LogicNode> nodes, String nodeId) {
        if (nodes == null) return null;
        for (LogicNode node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                return node;
            }
        }
        return null;
    }

    private List<LogicEdge> findOutEdges(List<LogicEdge> edges, String nodeId) {
        List<LogicEdge> result = new ArrayList<>();
        if (edges == null) return result;
        for (LogicEdge edge : edges) {
            if (edge.getSourceNodeId().equals(nodeId)) {
                result.add(edge);
            }
        }
        return result;
    }

    private LogicEdge selectNextEdge(List<LogicEdge> edges, Map<String, Object> variables) {
        for (LogicEdge edge : edges) {
            if (edge.getConditionExpression() == null || edge.getConditionExpression().isEmpty()) {
                return edge;
            }
            if (evaluateCondition(edge.getConditionExpression(), variables)) {
                return edge;
            }
        }
        return edges.isEmpty() ? null : edges.get(0);
    }

    private boolean evaluateCondition(String expression, Map<String, Object> variables) {
        try {
            return true;
        } catch (Exception e) {
            log.warn("条件表达式计算失败: {}", expression, e);
            return false;
        }
    }

    private void executeNode(LogicNode node, Map<String, Object> variables, StringBuilder executionLog) {
        String nodeType = node.getNodeCategory();
        executionLog.append("[").append(new Date()).append("] 执行节点类型: ").append(nodeType).append("\n");

        switch (nodeType) {
            case "START":
                executionLog.append("[").append(new Date()).append("] 流程开始\n");
                break;
            case "VARIABLE":
                variables.put(node.getNodeId(), "executed");
                executionLog.append("[").append(new Date()).append("] 变量赋值完成\n");
                break;
            case "DATA_OPERATION":
                executionLog.append("[").append(new Date()).append("] 数据操作执行\n");
                break;
            case "API_CALL":
                executionLog.append("[").append(new Date()).append("] API调用执行\n");
                break;
            case "NOTIFICATION":
                executionLog.append("[").append(new Date()).append("] 通知已发送\n");
                break;
            case "END":
                executionLog.append("[").append(new Date()).append("] 流程结束\n");
                break;
            default:
                executionLog.append("[").append(new Date()).append("] 节点执行完成\n");
        }
    }

    public List<LogicDebug> getDebugHistory(Long logicId) {
        LambdaQueryWrapper<LogicDebug> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LogicDebug::getLogicId, logicId);
        wrapper.orderByDesc(LogicDebug::getCreatedTime);
        wrapper.last("LIMIT 20");
        return list(wrapper);
    }
}
