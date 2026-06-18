package com.lowcode.flow.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.flow.entity.BusinessLogic;
import com.lowcode.flow.entity.LogicEdge;
import com.lowcode.flow.entity.LogicNode;
import com.lowcode.flow.mapper.BusinessLogicMapper;
import com.lowcode.generator.service.CodeGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class BusinessLogicService extends ServiceImpl<BusinessLogicMapper, BusinessLogic> {

    @Autowired
    private LogicNodeService nodeService;

    @Autowired
    private LogicEdgeService edgeService;

    @Autowired
    private CodeGeneratorService codeGeneratorService;

    public BusinessLogic getLogicDetail(Long id) {
        BusinessLogic logic = getById(id);
        if (logic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "业务逻辑不存在");
        }

        LambdaQueryWrapper<LogicNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(LogicNode::getLogicId, id);
        List<LogicNode> nodes = nodeService.list(nodeWrapper);
        logic.setNodes(nodes);

        LambdaQueryWrapper<LogicEdge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(LogicEdge::getLogicId, id);
        List<LogicEdge> edges = edgeService.list(edgeWrapper);
        logic.setEdges(edges);

        return logic;
    }

    public List<BusinessLogic> getLogicList(Long appId) {
        LambdaQueryWrapper<BusinessLogic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessLogic::getAppId, appId);
        wrapper.orderByDesc(BusinessLogic::getCreatedTime);
        return list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessLogic saveLogic(BusinessLogic logic) {
        LambdaQueryWrapper<BusinessLogic> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BusinessLogic::getLogicCode, logic.getLogicCode());
        wrapper.eq(BusinessLogic::getAppId, logic.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.LOGIC_EXISTS);
        }

        logic.setVersion("1.0.0");
        logic.setStatus("DRAFT");
        save(logic);

        if (logic.getNodes() != null) {
            for (LogicNode node : logic.getNodes()) {
                node.setLogicId(logic.getId());
                nodeService.save(node);
            }
        }

        if (logic.getEdges() != null) {
            for (LogicEdge edge : logic.getEdges()) {
                edge.setLogicId(logic.getId());
                edgeService.save(edge);
            }
        }

        return getLogicDetail(logic.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessLogic updateLogic(BusinessLogic logic) {
        BusinessLogic existing = getById(logic.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "业务逻辑不存在");
        }

        updateById(logic);

        LambdaQueryWrapper<LogicNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(LogicNode::getLogicId, logic.getId());
        nodeService.remove(nodeWrapper);

        LambdaQueryWrapper<LogicEdge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(LogicEdge::getLogicId, logic.getId());
        edgeService.remove(edgeWrapper);

        if (logic.getNodes() != null) {
            for (LogicNode node : logic.getNodes()) {
                node.setLogicId(logic.getId());
                nodeService.save(node);
            }
        }

        if (logic.getEdges() != null) {
            for (LogicEdge edge : logic.getEdges()) {
                edge.setLogicId(logic.getId());
                edgeService.save(edge);
            }
        }

        return getLogicDetail(logic.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteLogic(Long id) {
        BusinessLogic logic = getById(id);
        if (logic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "业务逻辑不存在");
        }

        LambdaQueryWrapper<LogicNode> nodeWrapper = new LambdaQueryWrapper<>();
        nodeWrapper.eq(LogicNode::getLogicId, id);
        nodeService.remove(nodeWrapper);

        LambdaQueryWrapper<LogicEdge> edgeWrapper = new LambdaQueryWrapper<>();
        edgeWrapper.eq(LogicEdge::getLogicId, id);
        edgeService.remove(edgeWrapper);

        removeById(id);
    }

    public String generateLogicCode(Long id) {
        BusinessLogic logic = getLogicDetail(id);
        StringBuilder sb = new StringBuilder();

        String className = toCamelCase(logic.getLogicCode()) + "Logic";
        String packageName = "com.lowcode.generated.logic";

        sb.append("package ").append(packageName).append(";\n\n");
        sb.append("import org.springframework.stereotype.Component;\n");
        sb.append("import java.util.*;\n");
        sb.append("import lombok.extern.slf4j.Slf4j;\n\n");
        sb.append("@Slf4j\n");
        sb.append("@Component\n");
        sb.append("public class ").append(className).append(" {\n\n");

        sb.append("    public Object execute(Map<String, Object> params) {\n");
        sb.append("        log.info(\"开始执行业务逻辑: ").append(logic.getLogicName()).append("\");\n");
        sb.append("        Map<String, Object> variables = new HashMap<>(params);\n\n");

        Map<String, LogicNode> nodeMap = new HashMap<>();
        if (logic.getNodes() != null) {
            for (LogicNode node : logic.getNodes()) {
                nodeMap.put(node.getNodeId(), node);
            }
        }

        Map<String, List<LogicEdge>> edgeMap = new HashMap<>();
        if (logic.getEdges() != null) {
            for (LogicEdge edge : logic.getEdges()) {
                edgeMap.computeIfAbsent(edge.getSourceNodeId(), k -> new ArrayList<>()).add(edge);
            }
        }

        LogicNode startNode = findStartNode(logic.getNodes());
        if (startNode != null) {
            generateNodeCode(sb, startNode, nodeMap, edgeMap, new HashSet<>(), 2);
        }

        sb.append("        return variables;\n");
        sb.append("    }\n");
        sb.append("}\n");

        String code = sb.toString();
        logic.setGeneratedCode(code);
        updateById(logic);

        return code;
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

    private void generateNodeCode(StringBuilder sb, LogicNode node, Map<String, LogicNode> nodeMap,
                                  Map<String, List<LogicEdge>> edgeMap, Set<String> visited, int indent) {
        if (node == null || visited.contains(node.getNodeId())) return;
        visited.add(node.getNodeId());

        String indentStr = getIndent(indent);

        sb.append(indentStr).append("// 节点: ").append(node.getNodeName()).append(" (").append(node.getNodeType()).append(")\n");

        switch (node.getNodeCategory()) {
            case "START":
                sb.append(indentStr).append("log.info(\"流程开始\");\n\n");
                break;
            case "TRIGGER":
                generateTriggerCode(sb, node, indentStr);
                break;
            case "CONDITION":
                generateConditionCode(sb, node, indentStr);
                break;
            case "LOOP":
                generateLoopCode(sb, node, indentStr);
                break;
            case "DATA_OPERATION":
                generateDataOperationCode(sb, node, indentStr);
                break;
            case "VARIABLE":
                generateVariableCode(sb, node, indentStr);
                break;
            case "NOTIFICATION":
                generateNotificationCode(sb, node, indentStr);
                break;
            case "API_CALL":
                generateApiCallCode(sb, node, indentStr);
                break;
            case "END":
                sb.append(indentStr).append("log.info(\"流程结束\");\n");
                sb.append(indentStr).append("return variables;\n");
                return;
            default:
                sb.append(indentStr).append("// 未知节点类型\n");
        }

        List<LogicEdge> outEdges = edgeMap.get(node.getNodeId());
        if (outEdges != null) {
            for (LogicEdge edge : outEdges) {
                LogicNode targetNode = nodeMap.get(edge.getTargetNodeId());
                if (targetNode != null) {
                    if (edge.getConditionExpression() != null && !edge.getConditionExpression().isEmpty()) {
                        sb.append(indentStr).append("if (").append(edge.getConditionExpression()).append(") {\n");
                        generateNodeCode(sb, targetNode, nodeMap, edgeMap, visited, indent + 1);
                        sb.append(indentStr).append("}\n");
                    } else {
                        generateNodeCode(sb, targetNode, nodeMap, edgeMap, visited, indent);
                    }
                }
            }
        }
    }

    private void generateTriggerCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 触发器: ").append(node.getNodeName()).append("\n");
        sb.append(indent).append("log.info(\"触发条件满足\");\n\n");
    }

    private void generateConditionCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 条件判断\n");
        sb.append(indent).append("boolean conditionResult = true;\n\n");
    }

    private void generateLoopCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 循环开始\n");
        sb.append(indent).append("for (int i = 0; i < 10; i++) {\n");
        sb.append(indent).append("    // 循环体\n");
        sb.append(indent).append("}\n\n");
    }

    private void generateDataOperationCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 数据操作\n");
        sb.append(indent).append("// TODO: 实现数据增删改查逻辑\n\n");
    }

    private void generateVariableCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 变量赋值\n");
        sb.append(indent).append("variables.put(\"key\", \"value\");\n\n");
    }

    private void generateNotificationCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// 发送通知\n");
        sb.append(indent).append("log.info(\"发送通知: ").append(node.getNodeName()).append("\");\n\n");
    }

    private void generateApiCallCode(StringBuilder sb, LogicNode node, String indent) {
        sb.append(indent).append("// API调用\n");
        sb.append(indent).append("// TODO: 实现API调用逻辑\n\n");
    }

    private String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append("    ");
        }
        return sb.toString();
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] parts = str.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
            }
        }
        return result.toString();
    }

    @Transactional(rollbackFor = Exception.class)
    public BusinessLogic publishLogic(Long id) {
        BusinessLogic logic = getLogicDetail(id);
        if (logic == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "业务逻辑不存在");
        }

        String code = generateLogicCode(id);

        String deployPath = deployCode(logic, code);

        logic.setStatus("PUBLISHED");
        logic.setDeployedPath(deployPath);
        logic.setVersion(incrementVersion(logic.getVersion()));
        updateById(logic);

        return logic;
    }

    private String deployCode(BusinessLogic logic, String code) {
        try {
            String basePath = System.getProperty("user.dir") + "/generated-code/";
            String packagePath = "com/lowcode/generated/logic/";
            String className = toCamelCase(logic.getLogicCode()) + "Logic.java";
            String fullPath = basePath + packagePath + className;

            File dir = new File(basePath + packagePath);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(fullPath);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(code);
            }

            log.info("代码已部署到: {}", fullPath);
            return fullPath;
        } catch (Exception e) {
            log.error("代码部署失败", e);
            throw new BusinessException("代码部署失败: " + e.getMessage());
        }
    }

    private String incrementVersion(String version) {
        if (version == null || version.isEmpty()) {
            return "1.0.0";
        }
        String[] parts = version.split("\\.");
        int patch = Integer.parseInt(parts[2]) + 1;
        return parts[0] + "." + parts[1] + "." + patch;
    }

    public Map<String, Object> getNodeTypes() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<Map<String, Object>> triggers = Arrays.asList(
                createNodeType("TIMER_TRIGGER", "定时触发器", "TRIGGER", "定时触发任务"),
                createNodeType("API_TRIGGER", "API触发器", "TRIGGER", "API调用触发"),
                createNodeType("DATA_TRIGGER", "数据触发器", "TRIGGER", "数据表事件触发")
        );
        result.put("triggers", triggers);

        List<Map<String, Object>> control = Arrays.asList(
                createNodeType("CONDITION", "条件分支", "CONDITION", "条件判断分支"),
                createNodeType("LOOP", "循环", "LOOP", "循环执行"),
                createNodeType("PARALLEL", "并行", "CONTROL", "并行执行")
        );
        result.put("control", control);

        List<Map<String, Object>> operations = Arrays.asList(
                createNodeType("DATA_INSERT", "数据插入", "DATA_OPERATION", "插入数据"),
                createNodeType("DATA_UPDATE", "数据更新", "DATA_OPERATION", "更新数据"),
                createNodeType("DATA_DELETE", "数据删除", "DATA_OPERATION", "删除数据"),
                createNodeType("DATA_QUERY", "数据查询", "DATA_OPERATION", "查询数据")
        );
        result.put("operations", operations);

        List<Map<String, Object>> utilities = Arrays.asList(
                createNodeType("VARIABLE", "变量赋值", "VARIABLE", "赋值变量"),
                createNodeType("API_CALL", "API调用", "API_CALL", "调用外部API"),
                createNodeType("NOTIFICATION", "发送通知", "NOTIFICATION", "发送消息通知"),
                createNodeType("SCRIPT", "脚本执行", "SCRIPT", "执行自定义脚本")
        );
        result.put("utilities", utilities);

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
}
