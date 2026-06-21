package com.lowcode.page.service.report;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.result.Result;
import com.lowcode.page.entity.report.Report;
import com.lowcode.page.entity.report.ReportComponent;
import com.lowcode.page.feign.DataQueryFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class ReportDataService {

    @Autowired
    private ReportService reportService;

    @Autowired
    private ReportComponentService componentService;

    @Autowired
    private DataQueryFeignClient dataQueryFeignClient;

    public Map<String, Object> getReportData(Long reportId) {
        return getReportData(reportId, null);
    }

    public Map<String, Object> getReportData(Long reportId, Map<String, Object> linkageFilters) {
        log.info("获取报表数据, reportId: {}, linkageFilters: {}", reportId, linkageFilters);

        Report report = reportService.getReportDetail(reportId);
        if (report == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "报表不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reportId", reportId);
        result.put("reportName", report.getReportName());

        Map<String, Object> componentDataMap = new LinkedHashMap<>();

        if (report.getComponents() != null) {
            for (ReportComponent component : report.getComponents()) {
                try {
                    List<Map<String, Object>> data = getComponentData(component, linkageFilters);
                    componentDataMap.put(component.getComponentId(), data);
                } catch (Exception e) {
                    log.error("获取组件数据失败, componentId: {}, error: {}", component.getComponentId(), e.getMessage());
                    componentDataMap.put(component.getComponentId(), Collections.emptyList());
                }
            }
        }

        result.put("components", componentDataMap);
        return result;
    }

    public List<Map<String, Object>> getComponentDataByComponentId(Long reportId, String componentId) {
        return getComponentDataByComponentId(reportId, componentId, null);
    }

    public List<Map<String, Object>> getComponentDataByComponentId(Long reportId, String componentId,
                                                                   Map<String, Object> linkageFilters) {
        log.info("获取报表组件数据, reportId: {}, componentId: {}", reportId, componentId);

        LambdaQueryWrapper<ReportComponent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReportComponent::getReportId, reportId);
        wrapper.eq(ReportComponent::getComponentId, componentId);
        ReportComponent component = componentService.getOne(wrapper);

        if (component == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "组件不存在");
        }

        return getComponentData(component, linkageFilters);
    }

    private List<Map<String, Object>> getComponentData(ReportComponent component, Map<String, Object> linkageFilters) {
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
