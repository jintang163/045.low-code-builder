package com.lowcode.page.service.dashboard;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.dashboard.Dashboard;
import com.lowcode.page.entity.dashboard.DashboardComponent;
import com.lowcode.page.feign.DataQueryFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class DashboardDataService {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardComponentService componentService;

    @Autowired
    private DataQueryFeignClient dataQueryFeignClient;

    public Map<String, Object> getDashboardData(Long dashboardId) {
        return getDashboardData(dashboardId, null);
    }

    public Map<String, Object> getDashboardData(Long dashboardId, Map<String, Object> linkageFilters) {
        log.info("获取大屏数据, dashboardId: {}, linkageFilters: {}", dashboardId, linkageFilters);

        Dashboard dashboard = dashboardService.getDashboardDetail(dashboardId);
        if (dashboard == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "大屏不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dashboardId", dashboardId);
        result.put("dashboardName", dashboard.getDashboardName());
        result.put("autoRefresh", dashboard.getAutoRefresh());
        result.put("refreshInterval", dashboard.getRefreshInterval());

        Map<String, Object> componentDataMap = new LinkedHashMap<>();

        if (dashboard.getComponents() != null) {
            for (DashboardComponent component : dashboard.getComponents()) {
                try {
                    Map<String, Object> componentResult = getComponentDataWithConfig(component, linkageFilters);
                    componentDataMap.put(component.getComponentId(), componentResult);
                } catch (Exception e) {
                    log.error("获取组件数据失败, componentId: {}, error: {}", component.getComponentId(), e.getMessage());
                    Map<String, Object> errorResult = new LinkedHashMap<>();
                    errorResult.put("data", Collections.emptyList());
                    errorResult.put("refreshInterval", null);
                    componentDataMap.put(component.getComponentId(), errorResult);
                }
            }
        }

        result.put("components", componentDataMap);
        return result;
    }

    public Map<String, Object> getComponentDataByComponentId(Long dashboardId, String componentId) {
        return getComponentDataByComponentId(dashboardId, componentId, null);
    }

    public Map<String, Object> getComponentDataByComponentId(Long dashboardId, String componentId,
                                                              Map<String, Object> linkageFilters) {
        log.info("获取大屏组件数据, dashboardId: {}, componentId: {}", dashboardId, componentId);

        LambdaQueryWrapper<DashboardComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DashboardComponent::getDashboardId, dashboardId);
        wrapper.eq(DashboardComponent::getComponentId, componentId);
        DashboardComponent component = componentService.getOne(wrapper);

        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }

        return getComponentDataWithConfig(component, linkageFilters);
    }

    private Map<String, Object> getComponentDataWithConfig(DashboardComponent component, Map<String, Object> linkageFilters) {
        Map<String, Object> result = new LinkedHashMap<>();

        Integer refreshInterval = getComponentRefreshInterval(component);
        result.put("refreshInterval", refreshInterval);

        List<Map<String, Object>> data = getComponentData(component, linkageFilters);
        result.put("data", data);

        return result;
    }

    private Integer getComponentRefreshInterval(DashboardComponent component) {
        String refreshConfigStr = component.getRefreshConfig();
        if (refreshConfigStr == null || refreshConfigStr.isEmpty()) {
            return null;
        }

        try {
            Map<String, Object> refreshConfig = JSON.parseObject(refreshConfigStr, new TypeReference<Map<String, Object>>() {});
            Object autoRefresh = refreshConfig.get("autoRefresh");
            Object interval = refreshConfig.get("refreshInterval");

            if (autoRefresh != null && "1".equals(autoRefresh.toString()) && interval != null) {
                return Integer.valueOf(interval.toString());
            }
        } catch (Exception e) {
            log.warn("解析组件刷新配置失败, componentId: {}, error: {}", component.getComponentId(), e.getMessage());
        }

        return null;
    }

    private List<Map<String, Object>> getComponentData(DashboardComponent component, Map<String, Object> linkageFilters) {
        String dataSourceConfigStr = component.getDataSourceConfig();
        if (dataSourceConfigStr == null || dataSourceConfigStr.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> dataSourceConfig;
        try {
            dataSourceConfig = JSON.parseObject(dataSourceConfigStr, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析数据源配置失败, componentId: {}, error: {}", component.getComponentId(), e.getMessage());
            return Collections.emptyList();
        }

        String dataSourceType = (String) dataSourceConfig.get("dataSourceType");
        if (dataSourceType == null) {
            return Collections.emptyList();
        }

        Map<String, Object> mergedFilters = mergeFilters(dataSourceConfig, linkageFilters);

        switch (dataSourceType) {
            case "dataModel":
                return queryByDataModel(dataSourceConfig, mergedFilters);
            case "dataSource":
                return queryByDataSource(dataSourceConfig, mergedFilters);
            case "sql":
                return queryBySql(dataSourceConfig, mergedFilters);
            default:
                log.warn("不支持的数据源类型: {}", dataSourceType);
                return Collections.emptyList();
        }
    }

    private Map<String, Object> mergeFilters(Map<String, Object> dataSourceConfig, Map<String, Object> linkageFilters) {
        Map<String, Object> result = new LinkedHashMap<>();

        Object filtersObj = dataSourceConfig.get("filters");
        if (filtersObj instanceof List) {
            List<Map<String, Object>> filters = (List<Map<String, Object>>) filtersObj;
            for (Map<String, Object> filter : filters) {
                String field = (String) filter.get("field");
                Object value = filter.get("value");
                if (field != null) {
                    result.put(field, value);
                }
            }
        }

        if (linkageFilters != null && !linkageFilters.isEmpty()) {
            result.putAll(linkageFilters);
        }

        return result;
    }

    private List<Map<String, Object>> queryByDataModel(Map<String, Object> config, Map<String, Object> filters) {
        Object modelIdObj = config.get("modelId");
        if (modelIdObj == null) {
            return Collections.emptyList();
        }

        Long modelId = Long.valueOf(modelIdObj.toString());
        String orderBy = (String) config.get("sortBy");
        String orderDir = (String) config.get("sortOrder");

        Result<List<Map<String, Object>>> result = dataQueryFeignClient.queryList(modelId, filters, orderBy, orderDir);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return result.getData();
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> queryByDataSource(Map<String, Object> config, Map<String, Object> filters) {
        Object dataSourceIdObj = config.get("dataSourceId");
        String tableName = (String) config.get("tableName");
        if (dataSourceIdObj == null || tableName == null) {
            return Collections.emptyList();
        }

        Long dataSourceId = Long.valueOf(dataSourceIdObj.toString());
        String orderBy = (String) config.get("sortBy");
        String orderDir = (String) config.get("sortOrder");
        Integer limit = config.get("limit") != null ? Integer.valueOf(config.get("limit").toString()) : null;

        Result<List<Map<String, Object>>> result = dataQueryFeignClient.queryByTable(
                dataSourceId, tableName, filters, orderBy, orderDir, limit);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return result.getData();
        }
        return Collections.emptyList();
    }

    private List<Map<String, Object>> queryBySql(Map<String, Object> config, Map<String, Object> filters) {
        Object dataSourceIdObj = config.get("dataSourceId");
        String sql = (String) config.get("sql");
        if (dataSourceIdObj == null || sql == null) {
            return Collections.emptyList();
        }

        Long dataSourceId = Long.valueOf(dataSourceIdObj.toString());

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("sql", sql);
        if (filters != null && !filters.isEmpty()) {
            params.put("filters", filters);
        }

        Result<List<Map<String, Object>>> result = dataQueryFeignClient.executeSql(dataSourceId, params);
        if (result != null && result.getCode() == 0 && result.getData() != null) {
            return result.getData();
        }
        return Collections.emptyList();
    }
}
