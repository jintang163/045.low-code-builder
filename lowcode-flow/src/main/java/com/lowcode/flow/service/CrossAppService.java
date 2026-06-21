package com.lowcode.flow.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.model.Result;
import com.lowcode.flow.dto.CrossAppCallDTO;
import com.lowcode.flow.entity.AppExposedApi;
import com.lowcode.flow.entity.AppExposedEvent;
import com.lowcode.flow.mapper.AppExposedApiMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CrossAppService extends ServiceImpl<AppExposedApiMapper, AppExposedApi> {

    @Autowired
    private AppExposedEventService appExposedEventService;

    @Value("${lowcode.api.gateway-url:http://localhost:8080}")
    private String gatewayUrl;

    @Value("${lowcode.api.internal-token:lowcode-internal-token}")
    private String internalToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public AppExposedApi registerApi(AppExposedApi api) {
        if (api.getApiCode() == null || api.getApiCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "接口编码不能为空");
        }
        if (api.getAppId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属应用ID不能为空");
        }

        AppExposedApi exist = getByCode(api.getApiCode());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "接口编码已存在：" + api.getApiCode());
        }

        if (api.getCreatedTime() == null) {
            api.setCreatedTime(LocalDateTime.now());
        }
        if (api.getUpdatedTime() == null) {
            api.setUpdatedTime(LocalDateTime.now());
        }
        if (api.getStatus() == null) {
            api.setStatus(1);
        }
        if (api.getTimeoutMs() == null) {
            api.setTimeoutMs(5000);
        }
        if (api.getAuthType() == null) {
            api.setAuthType("TOKEN");
        }

        save(api);
        log.info("注册应用接口成功: appId={}, apiCode={}", api.getAppId(), api.getApiCode());
        return getById(api.getId());
    }

    public AppExposedApi updateApi(AppExposedApi api) {
        if (api.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "接口ID不能为空");
        }
        api.setUpdatedTime(LocalDateTime.now());
        updateById(api);
        return getById(api.getId());
    }

    public void deleteApi(Long id) {
        removeById(id);
    }

    public AppExposedApi getApiById(Long id) {
        return getById(id);
    }

    public AppExposedApi getByCode(String apiCode) {
        LambdaQueryWrapper<AppExposedApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedApi::getApiCode, apiCode);
        return getOne(wrapper, false);
    }

    public List<AppExposedApi> listApisByApp(Long appId) {
        LambdaQueryWrapper<AppExposedApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedApi::getAppId, appId)
                .orderByDesc(AppExposedApi::getCreatedTime);
        return list(wrapper);
    }

    public List<AppExposedApi> listApisByAppCode(String appCode) {
        LambdaQueryWrapper<AppExposedApi> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AppExposedApi::getAppCode, appCode)
                .eq(AppExposedApi::getStatus, 1)
                .orderByDesc(AppExposedApi::getCreatedTime);
        return list(wrapper);
    }

    public Page<AppExposedApi> pageApis(Integer current, Integer size, Long appId, String keyword) {
        LambdaQueryWrapper<AppExposedApi> wrapper = new LambdaQueryWrapper<>();
        if (appId != null) {
            wrapper.eq(AppExposedApi::getAppId, appId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(AppExposedApi::getApiName, keyword)
                    .or().like(AppExposedApi::getApiCode, keyword);
        }
        wrapper.orderByDesc(AppExposedApi::getCreatedTime);
        return page(new Page<>(current, size), wrapper);
    }

    public AppExposedEvent registerEvent(AppExposedEvent event) {
        if (event.getEventCode() == null || event.getEventCode().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "事件编码不能为空");
        }
        if (event.getAppId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "所属应用ID不能为空");
        }

        AppExposedEvent exist = appExposedEventService.getByCode(event.getEventCode());
        if (exist != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "事件编码已存在：" + event.getEventCode());
        }

        if (event.getCreatedTime() == null) {
            event.setCreatedTime(LocalDateTime.now());
        }
        if (event.getUpdatedTime() == null) {
            event.setUpdatedTime(LocalDateTime.now());
        }
        if (event.getStatus() == null) {
            event.setStatus(1);
        }

        appExposedEventService.save(event);
        log.info("注册应用事件成功: appId={}, eventCode={}", event.getAppId(), event.getEventCode());
        return appExposedEventService.getById(event.getId());
    }

    public List<AppExposedEvent> listEventsByAppCode(String appCode) {
        return appExposedEventService.listByAppCode(appCode);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> executeCrossAppCall(CrossAppCallDTO dto) {
        String callType = dto.getCallType() != null ? dto.getCallType() : "API";
        Integer timeout = dto.getTimeoutMs() != null ? dto.getTimeoutMs() : 5000;

        log.info("执行跨应用调用: targetApp={}, targetCode={}, callType={}",
                dto.getTargetAppCode(), dto.getTargetCode(), callType);

        if ("API".equalsIgnoreCase(callType)) {
            return executeApiCall(dto, timeout);
        } else if ("EVENT".equalsIgnoreCase(callType)) {
            return executeEventPublish(dto, timeout);
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的调用类型：" + callType);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeApiCall(CrossAppCallDTO dto, Integer timeout) {
        AppExposedApi api = getByCode(dto.getTargetCode());
        if (api == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标接口不存在：" + dto.getTargetCode());
        }
        if (api.getStatus() != null && api.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标接口已禁用：" + dto.getTargetCode());
        }

        String url = buildApiUrl(api, dto);
        String method = api.getHttpMethod() != null ? api.getHttpMethod() : "POST";
        Map<String, Object> params = dto.getParams() != null ? dto.getParams() : new HashMap<>();

        log.info("调用HTTP接口: method={}, url={}, params={}", method, url, params);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);
            if (dto.getCallerAppId() != null) {
                headers.set("X-Caller-App-Id", String.valueOf(dto.getCallerAppId()));
            }
            if (dto.getCallerLogicId() != null) {
                headers.set("X-Caller-Logic-Id", String.valueOf(dto.getCallerLogicId()));
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(params, headers);
            ResponseEntity<String> response;

            if ("GET".equalsIgnoreCase(method)) {
                response = restTemplate.exchange(
                        buildGetUrl(url, params), HttpMethod.GET, new HttpEntity<>(headers), String.class);
            } else if ("PUT".equalsIgnoreCase(method)) {
                response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                response = restTemplate.exchange(
                        buildGetUrl(url, params), HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            } else {
                response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "接口调用失败，HTTP状态码：" + response.getStatusCode());
            }

            String body = response.getBody();
            log.info("接口调用成功: targetCode={}, response={}", dto.getTargetCode(), body);

            if (body == null || body.trim().isEmpty()) {
                return new HashMap<>();
            }

            Result<Map<String, Object>> result = JSON.parseObject(body, Result.class);
            if (result != null && result.getCode() != null && result.getCode() != 0) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        result.getMessage() != null ? result.getMessage() : "接口调用返回错误");
            }
            return (result != null && result.getData() != null) ? result.getData() : new HashMap<>();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用跨应用接口异常: targetCode={}", dto.getTargetCode(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "调用跨应用接口异常：" + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> executeEventPublish(CrossAppCallDTO dto, Integer timeout) {
        AppExposedEvent event = appExposedEventService.getByCode(dto.getTargetCode());
        if (event == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标事件不存在：" + dto.getTargetCode());
        }
        if (event.getStatus() != null && event.getStatus() == 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "目标事件已禁用：" + dto.getTargetCode());
        }

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("eventCode", dto.getTargetCode());
        eventPayload.put("eventName", event.getEventName());
        eventPayload.put("sourceAppCode", event.getAppCode());
        eventPayload.put("callerAppId", dto.getCallerAppId());
        eventPayload.put("callerLogicId", dto.getCallerLogicId());
        eventPayload.put("timestamp", System.currentTimeMillis());
        eventPayload.put("data", dto.getParams() != null ? dto.getParams() : new HashMap<>());

        String url = gatewayUrl + "/api/cross-app/event/publish";
        log.info("发布事件: eventCode={}, url={}, payload={}", dto.getTargetCode(), url, eventPayload);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Internal-Token", internalToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(eventPayload, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                        "事件发布失败，HTTP状态码：" + response.getStatusCode());
            }

            String body = response.getBody();
            log.info("事件发布成功: eventCode={}, response={}", dto.getTargetCode(), body);

            Map<String, Object> result = new HashMap<>();
            result.put("eventCode", dto.getTargetCode());
            result.put("published", true);
            result.put("eventId", "evt_" + System.currentTimeMillis());
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("发布事件异常: eventCode={}", dto.getTargetCode(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发布事件异常：" + e.getMessage());
        }
    }

    private String buildApiUrl(AppExposedApi api, CrossAppCallDTO dto) {
        String appCode = api.getAppCode() != null ? api.getAppCode() : dto.getTargetAppCode();
        String path = api.getApiPath() != null ? api.getApiPath() : ("/api/cross-app/" + appCode + "/" + api.getApiCode());
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return gatewayUrl + (path.startsWith("/") ? path : "/" + path);
    }

    private String buildGetUrl(String baseUrl, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean first = !baseUrl.contains("?");
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (first) {
                sb.append("?");
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue() != null ? entry.getValue() : "");
        }
        return sb.toString();
    }
}
