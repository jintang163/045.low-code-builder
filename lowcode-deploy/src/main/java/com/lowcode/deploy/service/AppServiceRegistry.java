package com.lowcode.deploy.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.deploy.entity.AppService;
import com.lowcode.deploy.mapper.AppServiceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AppServiceRegistry extends ServiceImpl<AppServiceMapper, AppService> {

    @Transactional(rollbackFor = Exception.class)
    public AppService registerIfAbsent(String serviceName, String displayName, String modulePath) {
        try {
            AppService existService = getByServiceName(serviceName);
            if (existService != null) {
                log.info("服务已存在，直接返回: {}", serviceName);
                return existService;
            }

            AppService appService = AppService.builder()
                    .serviceName(serviceName)
                    .displayName(displayName != null ? displayName : serviceName)
                    .modulePath(modulePath)
                    .imageName(serviceName)
                    .imageTag("latest")
                    .dockerfilePath("docker/" + serviceName + "/Dockerfile")
                    .jarPath(modulePath + "/target/" + serviceName + ".jar")
                    .build();

            save(appService);
            log.info("注册新服务成功: {} -> id={}", serviceName, appService.getId());
            return appService;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("注册服务失败: {}", serviceName, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册服务失败: " + e.getMessage());
        }
    }

    public List<AppService> listAll() {
        try {
            LambdaQueryWrapper<AppService> wrapper = new LambdaQueryWrapper<>();
            wrapper.orderByDesc(AppService::getCreatedTime);
            return list(wrapper);
        } catch (Exception e) {
            log.error("查询所有服务列表失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询服务列表失败: " + e.getMessage());
        }
    }

    public AppService getByServiceName(String name) {
        try {
            if (name == null || name.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "服务名不能为空");
            }
            LambdaQueryWrapper<AppService> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AppService::getServiceName, name);
            return getOne(wrapper, false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("按服务名查询失败: {}", name, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询服务失败: " + e.getMessage());
        }
    }
}
