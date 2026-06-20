package com.lowcode.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.cloud.CloudClient;
import com.lowcode.deploy.entity.CloudConfig;
import com.lowcode.deploy.entity.CloudPlatform;
import com.lowcode.deploy.mapper.CloudConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CloudConfigService extends ServiceImpl<CloudConfigMapper, CloudConfig> {

    @Autowired
    private CloudClientFactory cloudClientFactory;

    public List<CloudConfig> listByPlatform(CloudPlatform platform) {
        try {
            LambdaQueryWrapper<CloudConfig> wrapper = new LambdaQueryWrapper<>();
            if (platform != null) {
                wrapper.eq(CloudConfig::getPlatform, platform);
            }
            wrapper.orderByDesc(CloudConfig::getCreatedTime);
            return list(wrapper);
        } catch (Exception e) {
            log.error("查询云平台配置列表失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询云平台配置列表失败: " + e.getMessage());
        }
    }

    public boolean testConnection(Long configId) {
        try {
            CloudConfig config = getById(configId);
            if (config == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "云配置不存在");
            }
            CloudClient client = cloudClientFactory.getClient(config);
            com.lowcode.deploy.cloud.CloudConfig clientConfig = cloudClientFactory.toCloudClientConfig(config);
            return client.testConnection(clientConfig);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("测试云平台连接失败, configId: {}", configId, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "测试连接失败: " + e.getMessage());
        }
    }
}
