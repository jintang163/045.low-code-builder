package com.lowcode.model.datasource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.mapper.DataSourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DataSourceHealthChecker {

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Scheduled(fixedDelay = 60000)
    public void checkAllDataSources() {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getStatus, 1);
        List<DataSource> dataSources = dataSourceMapper.selectList(wrapper);

        for (DataSource ds : dataSources) {
            try {
                checkDataSource(ds);
            } catch (Exception e) {
                log.error("数据源健康检查异常: id={}, name={}, error={}", ds.getId(), ds.getSourceName(), e.getMessage());
            }
        }
    }

    public Map<String, Object> checkDataSource(DataSource ds) {
        boolean healthy = false;
        String checkStatus;
        if ("rest_api".equals(ds.getDbType())) {
            healthy = checkRestApiDataSource(ds);
            checkStatus = healthy ? "HEALTHY" : "UNHEALTHY";
        } else {
            healthy = dynamicDataSourceManager.testPoolConnection(ds.getId());
            if (!healthy) {
                try {
                    healthy = dynamicDataSourceManager.getConnection(ds.getId(), ds) != null;
                    if (healthy) {
                        checkStatus = "HEALTHY";
                    } else {
                        checkStatus = "UNHEALTHY";
                    }
                } catch (Exception e) {
                    healthy = false;
                    checkStatus = "UNHEALTHY";
                }
            } else {
                checkStatus = "HEALTHY";
            }
        }

        ds.setLastHealthCheckTime(LocalDateTime.now());
        ds.setHealthCheckStatus(checkStatus);
        if (!healthy) {
            ds.setStatus(0);
        }
        dataSourceMapper.updateById(ds);

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("dataSourceId", ds.getId());
        result.put("healthy", healthy);
        result.put("checkStatus", checkStatus);
        result.put("checkTime", LocalDateTime.now());
        return result;
    }

    private boolean checkRestApiDataSource(DataSource ds) {
        if (ds.getRestApiUrl() == null || ds.getRestApiUrl().isEmpty()) {
            return false;
        }
        try {
            java.net.URL url = new java.net.URL(ds.getRestApiUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(ds.getRestApiMethod() != null ? ds.getRestApiMethod() : "GET");
            conn.setConnectTimeout(ds.getConnectTimeout() != null ? ds.getConnectTimeout() : 5000);
            conn.setReadTimeout(ds.getReadTimeout() != null ? ds.getReadTimeout() : 5000);

            if (ds.getRestApiAuthType() != null && ds.getRestApiAuthToken() != null) {
                switch (ds.getRestApiAuthType()) {
                    case "BEARER":
                        conn.setRequestProperty("Authorization", "Bearer " + ds.getRestApiAuthToken());
                        break;
                    case "BASIC":
                        conn.setRequestProperty("Authorization", "Basic " + ds.getRestApiAuthToken());
                        break;
                    case "API_KEY":
                        conn.setRequestProperty("X-API-Key", ds.getRestApiAuthToken());
                        break;
                    default:
                        break;
                }
            }

            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode >= 200 && responseCode < 500;
        } catch (Exception e) {
            log.warn("REST API数据源健康检查失败: id={}, url={}, error={}", ds.getId(), ds.getRestApiUrl(), e.getMessage());
            return false;
        }
    }
}
