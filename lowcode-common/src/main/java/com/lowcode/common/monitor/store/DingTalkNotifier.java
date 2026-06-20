package com.lowcode.common.monitor.store;

import com.alibaba.fastjson2.JSON;
import com.lowcode.common.monitor.entity.AlertRule;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DingTalkNotifier {

    public static void sendAlert(AlertRule rule, String message) {
        String webhookUrl = rule.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("钉钉Webhook未配置，跳过告警通知: {}", rule.getRuleName());
            return;
        }
        try {
            Map<String, Object> content = new HashMap<>();
            content.put("content", "【低代码平台监控告警】\n" +
                    "规则名称: " + rule.getRuleName() + "\n" +
                    "告警内容: " + message + "\n" +
                    "触发时间: " + java.time.LocalDateTime.now());

            Map<String, Object> payload = new HashMap<>();
            payload.put("msgtype", "text");
            payload.put("text", content);

            if (rule.getNotifyTargets() != null && !rule.getNotifyTargets().isEmpty()) {
                Map<String, Object> at = new HashMap<>();
                at.put("atMobiles", rule.getNotifyTargets());
                at.put("isAtAll", false);
                payload.put("at", at);
            }

            sendPostRequest(webhookUrl, JSON.toJSONString(payload));
        } catch (Exception e) {
            log.error("发送钉钉告警失败", e);
        }
    }

    private static void sendPostRequest(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        log.info("钉钉告警发送响应码: {}", code);
        conn.disconnect();
    }
}
