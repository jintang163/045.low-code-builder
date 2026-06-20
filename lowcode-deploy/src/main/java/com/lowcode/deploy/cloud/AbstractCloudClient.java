package com.lowcode.deploy.cloud;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.dto.ClusterInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
public abstract class AbstractCloudClient implements CloudClient {

    @Override
    public boolean testConnection(CloudConfig config) {
        try {
            List<ClusterInfo> clusters = listClusters(config);
            return clusters != null;
        } catch (Exception e) {
            log.warn("测试云平台连接失败: {}", e.getMessage());
            return false;
        }
    }

    protected String httpGet(String url, Map<String, String> headers, Map<String, Object> queryParams) {
        return executeRequest(Method.GET, url, headers, queryParams, null);
    }

    protected String httpPost(String url, Map<String, String> headers, Object body) {
        return executeRequest(Method.POST, url, headers, null, body);
    }

    protected String httpPut(String url, Map<String, String> headers, Object body) {
        return executeRequest(Method.PUT, url, headers, null, body);
    }

    protected String httpDelete(String url, Map<String, String> headers) {
        return executeRequest(Method.DELETE, url, headers, null, null);
    }

    private String executeRequest(Method method, String url, Map<String, String> headers,
                                   Map<String, Object> queryParams, Object body) {
        try {
            HttpRequest request = HttpRequest.of(url).method(method).timeout(30000);

            if (headers != null && !headers.isEmpty()) {
                headers.forEach(request::header);
            }

            if (queryParams != null && !queryParams.isEmpty()) {
                request.form(queryParams);
            }

            if (body != null) {
                String bodyStr = body instanceof String ? (String) body : JSON.toJSONString(body);
                request.body(bodyStr);
                request.header("Content-Type", "application/json; charset=UTF-8");
            }

            log.debug("发送HTTP请求: {} {}, headers: {}, params: {}", method, url, headers, queryParams);

            HttpResponse response = request.execute();
            String responseBody = response.body();

            if (!response.isOk()) {
                log.error("HTTP请求失败: {} {}, status: {}, body: {}", method, url, response.getStatus(), responseBody);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "HTTP请求失败: " + response.getStatus() + ", " + responseBody);
            }

            log.debug("HTTP响应成功: {}", responseBody);
            return responseBody;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("HTTP请求异常: {} {}, error: {}", method, url, e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTTP请求异常: " + e.getMessage());
        }
    }
}
