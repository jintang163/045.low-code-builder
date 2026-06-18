package com.lowcode.page.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lowcode.common.util.RedisUtil;
import com.lowcode.page.dto.AiChatMessage;
import com.lowcode.page.dto.AiPageGenerateDTO;
import com.lowcode.page.service.AiPageService;
import com.lowcode.page.service.LlmService;
import com.lowcode.page.vo.AiPageGenerateVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiPageServiceImpl implements AiPageService {

    private static final String REDIS_KEY_PREFIX = "ai_page_session:";
    private static final long SESSION_EXPIRE_HOURS = 24;

    private static final Set<String> VALID_COMPONENT_TYPES = new HashSet<>(Arrays.asList(
            "INPUT", "TEXTAREA", "NUMBER", "SELECT", "DATE", "DATETIME", "TIME",
            "SWITCH", "CHECKBOX", "RADIO", "UPLOAD", "RICHTEXT", "TABLE", "BUTTON",
            "LINK", "IMAGE", "TEXT", "TITLE", "ICON", "DIVIDER", "TABS", "CARD",
            "GRID", "FLEX", "MODAL", "FORM", "STEPS", "TIMELINE", "PROGRESS",
            "RATE", "SLIDER", "LINECHART", "BARCHART", "PIECHART", "AREACHART",
            "SCATTERCHART", "RADARCHART"
    ));

    @Autowired
    private LlmService llmService;

    @Autowired(required = false)
    private RedisUtil redisUtil;

    @Override
    public AiPageGenerateVO generatePage(AiPageGenerateDTO dto) {
        AiPageGenerateVO result = new AiPageGenerateVO();
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = generateSessionId();
        }
        result.setSessionId(sessionId);

        try {
            List<AiChatMessage> history = loadHistory(sessionId);
            if (dto.getHistory() != null && !dto.getHistory().isEmpty()) {
                history = dto.getHistory();
            }

            String systemPrompt = buildSystemPrompt();

            String userPrompt = buildUserPrompt(dto);

            List<AiChatMessage> messages = new ArrayList<>();
            messages.add(AiChatMessage.system(systemPrompt));

            int historySize = history.size();
            int maxHistory = 10;
            int start = Math.max(0, historySize - maxHistory);
            messages.addAll(history.subList(start, historySize));

            messages.add(AiChatMessage.user(userPrompt));

            log.info("Generating AI page for user: {}", dto.getUserMessage());

            String aiResponse = llmService.chatWithJson(messages, 0.3, 4096);

            log.debug("AI raw response: {}", aiResponse);

            JSONObject parsedResult = parseAiResponse(aiResponse);

            if (parsedResult == null) {
                result.setSuccess(false);
                result.setErrorMessage("无法解析AI生成的页面配置，请重试");
                result.setReplyMessage("抱歉，我生成的页面配置格式有误，请重试或描述更详细一些。");
                result.setHistory(history);
                return result;
            }

            validateAndFixPageJson(parsedResult);

            String pageJson = parsedResult.toJSONString();
            String pageName = parsedResult.getString("pageName");
            String replyMessage = parsedResult.getString("replyMessage");
            if (replyMessage == null || replyMessage.isEmpty()) {
                replyMessage = "已根据您的描述生成页面配置，您可以在画布上继续调整。";
            }

            history.add(AiChatMessage.user(dto.getUserMessage()));
            history.add(AiChatMessage.assistant(aiResponse));
            saveHistory(sessionId, history);

            result.setSuccess(true);
            result.setPageName(pageName);
            result.setPageJson(pageJson);
            result.setReplyMessage(replyMessage);
            result.setHistory(history);

            log.info("AI page generated successfully: {}", pageName);

        } catch (Exception e) {
            log.error("AI page generation failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setReplyMessage("抱歉，生成页面时出现错误：" + e.getMessage());
            List<AiChatMessage> history = loadHistory(sessionId);
            result.setHistory(history);
        }

        return result;
    }

    @Override
    public String generateSessionId() {
        return "ai_page_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void clearSession(String sessionId) {
        if (redisUtil != null) {
            redisUtil.delete(REDIS_KEY_PREFIX + sessionId);
        }
    }

    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个专业的低代码平台页面生成助手。你的任务是根据用户的自然语言描述，生成符合低代码平台规范的页面配置JSON。\n\n");
        prompt.append("## 页面JSON格式规范\n\n");
        prompt.append("你必须返回一个合法的JSON对象，包含以下字段：\n\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"pageName\": \"页面名称\",\n");
        prompt.append("  \"replyMessage\": \"给用户的友好回复\",\n");
        prompt.append("  \"components\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"componentId\": \"唯一ID，格式如comp_数字\",\n");
        prompt.append("      \"componentName\": \"组件显示名称\",\n");
        prompt.append("      \"componentType\": \"组件类型\",\n");
        prompt.append("      \"parentId\": null,\n");
        prompt.append("      \"sortOrder\": 排序数字从1开始递增,\n");
        prompt.append("      \"propsConfig\": \"JSON字符串，组件属性配置\",\n");
        prompt.append("      \"styleConfig\": \"JSON字符串，样式配置\",\n");
        prompt.append("      \"eventConfig\": \"JSON数组字符串，事件配置\",\n");
        prompt.append("      \"dataSourceConfig\": \"JSON字符串，数据源配置\",\n");
        prompt.append("      \"validationConfig\": \"JSON数组字符串，校验配置\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("## 支持的组件类型\n\n");
        prompt.append("你只能使用以下组件类型（componentType）：\n\n");
        prompt.append("**基础组件：** TEXT（文本）、TITLE（标题）、ICON（图标）、DIVIDER（分割线）、BUTTON（按钮）、LINK（链接）、IMAGE（图片）\n\n");
        prompt.append("**表单组件：** INPUT（输入框）、TEXTAREA（多行文本）、NUMBER（数字输入）、SELECT（下拉选择）、DATE（日期）、DATETIME（日期时间）、TIME（时间）、SWITCH（开关）、CHECKBOX（复选框）、RADIO（单选框）、UPLOAD（文件上传）、RICHTEXT（富文本）、FORM（表单）\n\n");
        prompt.append("**布局组件：** CARD（卡片）、TABS（标签页）、GRID（栅格布局）、FLEX（弹性布局）\n\n");
        prompt.append("**展示组件：** TABLE（表格）、PROGRESS（进度条）、TIMELINE（时间线）、STEPS（步骤条）、RATE（评分）、SLIDER（滑块）\n\n");
        prompt.append("**图表组件：** LINECHART（折线图）、BARCHART（柱状图）、PIECHART（饼图）、AREACHART（面积图）、SCATTERCHART（散点图）、RADARCHART（雷达图）\n\n");
        prompt.append("**其他：** MODAL（弹窗）\n\n");
        prompt.append("## 组件propsConfig示例\n\n");
        prompt.append("不同组件的propsConfig（JSON字符串）：\n\n");
        prompt.append("- INPUT: `{\"placeholder\": \"请输入\", \"defaultValue\": \"\"}`\n");
        prompt.append("- BUTTON: `{\"text\": \"提交\", \"type\": \"primary\"}`\n");
        prompt.append("- TITLE: `{\"text\": \"标题\", \"level\": 2}`\n");
        prompt.append("- TEXT: `{\"text\": \"文本内容\"}`\n");
        prompt.append("- TABLE: `{\"columns\": [{\"title\": \"名称\", \"dataIndex\": \"name\"}]}`\n");
        prompt.append("- SELECT: `{\"placeholder\": \"请选择\", \"options\": [{\"label\": \"选项1\", \"value\": \"1\"}]}`\n\n");
        prompt.append("## 重要规则\n\n");
        prompt.append("1. **只返回JSON，不要返回任何其他文字、解释或markdown代码块标记**\n");
        prompt.append("2. componentId必须唯一，使用comp_前缀加数字\n");
        prompt.append("3. sortOrder从1开始，每个组件递增\n");
        prompt.append("4. 所有的propsConfig、styleConfig、eventConfig、dataSourceConfig、validationConfig必须是JSON字符串（用\"转义双引号）\n");
        prompt.append("5. 如果是优化现有页面，components数组需要包含修改后的完整组件列表\n");
        prompt.append("6. 生成的页面要美观实用，组件排列合理\n");
        prompt.append("7. 如果用户描述模糊，请根据最佳实践生成合理的页面\n");
        prompt.append("8. replyMessage要用友好的中文告诉用户你做了什么\n");

        return prompt.toString();
    }

    private String buildUserPrompt(AiPageGenerateDTO dto) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("## 用户需求\n\n");
        prompt.append(dto.getUserMessage()).append("\n\n");

        if (dto.getCurrentPageJson() != null && !dto.getCurrentPageJson().isEmpty()) {
            prompt.append("## 当前页面配置（需要在此基础上优化）\n\n");
            prompt.append("```json\n");
            prompt.append(dto.getCurrentPageJson()).append("\n");
            prompt.append("```\n\n");
            prompt.append("请根据用户需求修改以上页面配置，返回修改后的完整页面JSON。\n");
        } else {
            prompt.append("请根据用户需求生成一个新的页面配置JSON。\n");
        }

        return prompt.toString();
    }

    private JSONObject parseAiResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return null;
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();
        }

        int jsonStart = cleaned.indexOf('{');
        int jsonEnd = cleaned.lastIndexOf('}');
        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
        }

        try {
            return JSON.parseObject(cleaned);
        } catch (Exception e) {
            log.warn("Failed to parse AI response as JSON: {}", cleaned);
            return null;
        }
    }

    private void validateAndFixPageJson(JSONObject pageJson) {
        if (!pageJson.containsKey("pageName") || pageJson.getString("pageName") == null) {
            pageJson.put("pageName", "AI生成页面");
        }

        if (!pageJson.containsKey("components")) {
            pageJson.put("components", new JSONArray());
            return;
        }

        JSONArray components = pageJson.getJSONArray("components");
        if (components == null) {
            pageJson.put("components", new JSONArray());
            return;
        }

        int sortOrder = 1;
        Set<String> usedIds = new HashSet<>();

        for (int i = 0; i < components.size(); i++) {
            JSONObject comp = components.getJSONObject(i);
            if (comp == null) continue;

            String componentId = comp.getString("componentId");
            if (componentId == null || componentId.isEmpty() || usedIds.contains(componentId)) {
                componentId = "comp_" + (sortOrder + 1000);
                comp.put("componentId", componentId);
            }
            usedIds.add(componentId);

            if (!comp.containsKey("componentName") || comp.getString("componentName") == null) {
                comp.put("componentName", "组件" + sortOrder);
            }

            String componentType = comp.getString("componentType");
            if (componentType == null || !VALID_COMPONENT_TYPES.contains(componentType.toUpperCase())) {
                componentType = "CARD";
                comp.put("componentType", componentType);
            } else {
                comp.put("componentType", componentType.toUpperCase());
            }

            if (!comp.containsKey("parentId")) {
                comp.put("parentId", null);
            }

            if (!comp.containsKey("sortOrder") || comp.getInteger("sortOrder") == null) {
                comp.put("sortOrder", sortOrder);
            }
            sortOrder = Math.max(sortOrder, comp.getInteger("sortOrder")) + 1;

            ensureJsonString(comp, "propsConfig", "{}");
            ensureJsonString(comp, "styleConfig", "{}");
            ensureJsonString(comp, "eventConfig", "[]");
            ensureJsonString(comp, "dataSourceConfig", "{}");
            ensureJsonString(comp, "validationConfig", "[]");
        }
    }

    private void ensureJsonString(JSONObject comp, String key, String defaultValue) {
        if (!comp.containsKey(key)) {
            comp.put(key, defaultValue);
            return;
        }

        Object value = comp.get(key);
        if (value == null) {
            comp.put(key, defaultValue);
            return;
        }

        if (value instanceof String) {
            String strValue = (String) value;
            if (strValue.trim().isEmpty()) {
                comp.put(key, defaultValue);
                return;
            }
            try {
                JSON.parse(strValue);
            } catch (Exception e) {
                log.warn("Invalid JSON string for {}: {}, using default", key, strValue);
                comp.put(key, defaultValue);
            }
        } else {
            comp.put(key, JSON.toJSONString(value));
        }
    }

    @SuppressWarnings("unchecked")
    private List<AiChatMessage> loadHistory(String sessionId) {
        if (redisUtil == null) {
            return new ArrayList<>();
        }
        try {
            Object cached = redisUtil.get(REDIS_KEY_PREFIX + sessionId);
            if (cached != null) {
                return (List<AiChatMessage>) cached;
            }
        } catch (Exception e) {
            log.warn("Failed to load AI session history from Redis", e);
        }
        return new ArrayList<>();
    }

    private void saveHistory(String sessionId, List<AiChatMessage> history) {
        if (redisUtil == null) {
            return;
        }
        try {
            redisUtil.set(REDIS_KEY_PREFIX + sessionId, history, SESSION_EXPIRE_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("Failed to save AI session history to Redis", e);
        }
    }
}
