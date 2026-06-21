package com.lowcode.flow.rpa;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.lowcode.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
public class RpaExecutorClient {

    @Value("${rpa.executor.url:http://localhost:8000}")
    private String executorUrl;

    @Value("${rpa.executor.timeout:300000}")
    private int timeout;

    public JSONObject executeScript(String scriptContent, Map<String, Object> params, String targetUrl) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("script", scriptContent);
            requestBody.put("params", params);
            requestBody.put("targetUrl", targetUrl);

            String response = sendPostRequest(executorUrl + "/api/execute", requestBody);
            JSONObject result = JSON.parseObject(response);

            if (result.getBooleanValue("success")) {
                return result.getJSONObject("data");
            } else {
                throw new BusinessException("RPA执行失败: " + result.getString("message"));
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用RPA执行器失败", e);
            throw new BusinessException("调用RPA执行器失败: " + e.getMessage());
        }
    }

    public String validateScript(String scriptContent) {
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("script", scriptContent);

            String response = sendPostRequest(executorUrl + "/api/validate", requestBody);
            JSONObject result = JSON.parseObject(response);

            if (!result.getBooleanValue("success")) {
                return result.getString("message");
            }
            return null;
        } catch (Exception e) {
            log.error("验证RPA脚本失败", e);
            return "验证服务不可用: " + e.getMessage();
        }
    }

    public boolean checkHealth() {
        try {
            String response = sendGetRequest(executorUrl + "/health");
            JSONObject result = JSON.parseObject(response);
            return "UP".equals(result.getString("status"));
        } catch (Exception e) {
            log.warn("RPA执行器健康检查失败", e);
            return false;
        }
    }

    private String sendPostRequest(String urlStr, JSONObject body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = body.toJSONString().getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return readResponse(conn);
    }

    private String sendGetRequest(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}
