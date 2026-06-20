package com.lowcode.deploy.service;

import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.CloudClient;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.CloudPlatform;
import com.lowcode.deploy.mapper.CloudConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CloudClientFactory {

    @Autowired
    private List<CloudClient> cloudClients;

    @Autowired
    private CloudConfigMapper cloudConfigMapper;

    private final Map<CloudPlatform, CloudClient> clientMap = new EnumMap<>(CloudPlatform.class);

    @PostConstruct
    public void init() {
        if (cloudClients == null || cloudClients.isEmpty()) {
            log.warn("未发现任何 CloudClient 实现");
            return;
        }
        for (CloudClient client : cloudClients) {
            String className = client.getClass().getSimpleName().toLowerCase();
            CloudPlatform platform = matchPlatform(className);
            if (platform != null) {
                clientMap.put(platform, client);
                log.info("注册 CloudClient: {} -> {}", platform, client.getClass().getName());
            }
        }
    }

    private CloudPlatform matchPlatform(String className) {
        if (className.contains("aliyun")) {
            return CloudPlatform.ALIYUN;
        }
        if (className.contains("tencent")) {
            return CloudPlatform.TENCENT;
        }
        if (className.contains("custom")) {
            return CloudPlatform.CUSTOM;
        }
        return null;
    }

    public CloudClient getClient(CloudConfig config) {
        if (config == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "云配置不能为空");
        }
        CloudPlatform platform = config.getPlatform();
        if (platform == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "云平台类型不能为空");
        }
        CloudClient client = clientMap.get(platform);
        if (client == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "未找到对应云平台的客户端: " + platform.getName());
        }
        return client;
    }

    public CloudClient getClient(Long cloudConfigId) {
        if (cloudConfigId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "云配置ID不能为空");
        }
        CloudConfig config = cloudConfigMapper.selectById(cloudConfigId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "云配置不存在");
        }
        return getClient(config);
    }

    public com.lowcode.deploy.cloud.CloudConfig toCloudClientConfig(CloudConfig entityConfig) {
        return com.lowcode.deploy.cloud.CloudConfig.builder()
                .provider(entityConfig.getPlatform() != null ? entityConfig.getPlatform().getCode() : null)
                .accessKeyId(entityConfig.getAccessKey())
                .accessKeySecret(entityConfig.getAccessSecret())
                .regionId(entityConfig.getRegion())
                .clusterId(entityConfig.getClusterId())
                .build();
    }
}
