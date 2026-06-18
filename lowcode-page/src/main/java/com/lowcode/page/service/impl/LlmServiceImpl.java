package com.lowcode.page.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lowcode.page.config.AiConfig;
import com.lowcode.page.dto.AiChatMessage;
import com.lowcode.page.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    @Autowired
    private AiConfig aiConfig;

    private OkHttpClient httpClient;

    public LlmServiceImpl() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public String chat(List<AiChatMessage> messages) {
        return chatWithJson(messages, aiConfig.getTemperature(), aiConfig.getMaxTokens());
    }

    @Override
    public String chatWithJson(List<AiChatMessage> messages, double temperature, int maxTokens) {
        if (!Boolean.TRUE.equals(aiConfig.getEnabled())) {
            log.warn("AI LLM service is disabled, returning mock response");
            return getMockResponse(messages);
        }

        try {
            String endpoint = aiConfig.getEndpoint();
            if (!endpoint.endsWith("/")) {
                endpoint = endpoint.substring(0, endpoint.length() - 1);
            }
            String url = endpoint + "/chat/completions";

            JSONArray messagesJson = new JSONArray();
            for (AiChatMessage msg : messages) {
                JSONObject jsonMsg = new JSONObject();
                jsonMsg.put("role", msg.getRole());
                jsonMsg.put("content", msg.getContent());
                messagesJson.add(jsonMsg);
            }

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", aiConfig.getModel());
            requestBody.put("messages", messagesJson);
            requestBody.put("temperature", temperature);
            requestBody.put("max_tokens", maxTokens);

            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(requestBody.toJSONString(), mediaType);

            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            log.debug("Calling LLM API: {}", url);
            log.debug("Request body: {}", requestBody.toJSONString());

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No response body";
                    log.error("LLM API call failed: HTTP {} - {}", response.code(), errorBody);
                    throw new RuntimeException("LLM API调用失败: HTTP " + response.code());
                }

                String responseBody = response.body() != null ? response.body().string() : "";
                log.debug("LLM API response: {}", responseBody);

                JSONObject jsonResponse = JSON.parseObject(responseBody);
                JSONArray choices = jsonResponse.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject messageObj = choice.getJSONObject("message");
                    if (messageObj != null) {
                        return messageObj.getString("content");
                    }
                }

                throw new RuntimeException("无法解析LLM响应失败");
            }
        } catch (IOException e) {
            log.error("LLM API call exception", e);
            throw new RuntimeException("LLM API调用异常: " + e.getMessage(), e);
        }
    }

    private String getMockResponse(List<AiChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"pageName\": \"AI生成页面\",\n");
        sb.append("  \"components\": [\n");
        sb.append("    {\n");
        sb.append("      \"componentId\": \"comp_1\",\n");
        sb.append("      \"componentName\": \"标题\",\n");
        sb.append("      \"componentType\": \"TITLE\",\n");
        sb.append("      \"sortOrder\": 1,\n");
        sb.append("      \"propsConfig\": \"{\\\"text\\\": \\\"商品管理列表\\\"}\",\n");
        sb.append("      \"styleConfig\": \"{}\",\n");
        sb.append("      \"eventConfig\": \"[]\",\n");
        sb.append("      \"dataSourceConfig\": \"{}\",\n");
        sb.append("      \"validationConfig\": \"[]\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"componentId\": \"comp_2\",\n");
        sb.append("      \"componentName\": \"输入框\",\n");
        sb.append("      \"componentType\": \"INPUT\",\n");
        sb.append("      \"sortOrder\": 2,\n");
        sb.append("      \"propsConfig\": \"{\\\"placeholder\\\": \\\"请输入搜索关键词\\\"}\",\n");
        sb.append("      \"styleConfig\": \"{}\",\n");
        sb.append("      \"eventConfig\": \"[]\",\n");
        sb.append("      \"dataSourceConfig\": \"{}\",\n");
        sb.append("      \"validationConfig\": \"[]\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"componentId\": \"comp_3\",\n");
        sb.append("      \"componentName\": \"按钮\",\n");
        sb.append("      \"componentType\": \"BUTTON\",\n");
        sb.append("      \"sortOrder\": 3,\n");
        sb.append("      \"propsConfig\": \"{\\\"text\\\": \\\"搜索\\\"}\",\n");
        sb.append("      \"styleConfig\": \"{}\",\n");
        sb.append("      \"eventConfig\": \"[]\",\n");
        sb.append("      \"dataSourceConfig\": \"{}\",\n");
        sb.append("      \"validationConfig\": \"[]\"\n");
        sb.append("    },\n");
        sb.append("    {\n");
        sb.append("      \"componentId\": \"comp_4\",\n");
        sb.append("      \"componentName\": \"表格\",\n");
        sb.append("      \"componentType\": \"TABLE\",\n");
        sb.append("      \"sortOrder\": 4,\n");
        sb.append("      \"propsConfig\": \"{}\",\n");
        sb.append("      \"styleConfig\": \"{}\",\n");
        sb.append("      \"eventConfig\": \"[]\",\n");
        sb.append("      \"dataSourceConfig\": \"{}\",\n");
        sb.append("      \"validationConfig\": \"[]\"\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }
}
