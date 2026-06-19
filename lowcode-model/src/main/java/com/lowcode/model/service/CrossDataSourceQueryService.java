package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.model.datasource.DynamicDataSourceManager;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.VirtualView;
import com.lowcode.model.entity.VirtualViewJoin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class CrossDataSourceQueryService {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    public List<Map<String, Object>> queryVirtualView(Long viewId) {
        VirtualView virtualView = dataSourceService.getVirtualViewService().getById(viewId);
        if (virtualView == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "虚拟视图不存在");
        }

        List<VirtualViewJoin> joins = dataSourceService.getVirtualViewService()
                .parseJoinConfig(virtualView.getJoinConfig());

        if (joins == null || joins.isEmpty()) {
            if (virtualView.getViewSql() != null && !virtualView.getViewSql().isEmpty()) {
                Long dsId = extractFirstDataSourceId(virtualView.getViewConfig());
                return executeQueryOnDataSource(dsId, virtualView.getViewSql());
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "虚拟视图未配置关联关系");
        }

        Map<Long, List<Map<String, Object>>> dataBySource = new LinkedHashMap<>();
        Set<Long> allSourceIds = new LinkedHashSet<>();
        for (VirtualViewJoin join : joins) {
            allSourceIds.add(join.getLeftDataSourceId());
            allSourceIds.add(join.getRightDataSourceId());
        }

        for (Long sourceId : allSourceIds) {
            List<Map<String, Object>> data = fetchAllFromDataSource(sourceId, joins);
            dataBySource.put(sourceId, data);
        }

        return performJoin(joins, dataBySource);
    }

    private List<Map<String, Object>> fetchAllFromDataSource(Long dataSourceId, List<VirtualViewJoin> joins) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在: " + dataSourceId);
        }

        Set<String> tables = new LinkedHashSet<>();
        Set<String> columns = new LinkedHashSet<>();
        for (VirtualViewJoin join : joins) {
            if (join.getLeftDataSourceId().equals(dataSourceId)) {
                tables.add(join.getLeftTable());
                columns.add(join.getLeftAlias() + "." + join.getLeftColumn());
            }
            if (join.getRightDataSourceId().equals(dataSourceId)) {
                tables.add(join.getRightTable());
                columns.add(join.getRightAlias() + "." + join.getRightColumn());
            }
        }

        String sql = buildSelectSql(tables, columns);
        return executeQueryOnDataSource(dataSourceId, sql);
    }

    private String buildSelectSql(Set<String> tables, Set<String> columns) {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(String.join(", ", columns));
        sb.append(" FROM ");
        boolean first = true;
        for (String table : tables) {
            if (!first) sb.append(", ");
            sb.append(table);
            first = false;
        }
        return sb.toString();
    }

    private List<Map<String, Object>> executeQueryOnDataSource(Long dataSourceId, String sql) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            return resultSetToList(rs);
        } catch (SQLException e) {
            log.error("跨数据源查询执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
            closeQuietly(conn);
        }
    }

    private List<Map<String, Object>> performJoin(List<VirtualViewJoin> joins,
                                                   Map<Long, List<Map<String, Object>>> dataBySource) {
        if (joins.isEmpty()) {
            return Collections.emptyList();
        }

        VirtualViewJoin firstJoin = joins.get(0);
        List<Map<String, Object>> result = new ArrayList<>(dataBySource.getOrDefault(firstJoin.getLeftDataSourceId(), Collections.emptyList()));

        for (VirtualViewJoin join : joins) {
            List<Map<String, Object>> leftData = result;
            List<Map<String, Object>> rightData = dataBySource.getOrDefault(join.getRightDataSourceId(), Collections.emptyList());

            result = new ArrayList<>();
            for (Map<String, Object> leftRow : leftData) {
                for (Map<String, Object> rightRow : rightData) {
                    Object leftVal = getNestedValue(leftRow, join.getLeftAlias(), join.getLeftColumn());
                    Object rightVal = getNestedValue(rightRow, join.getRightAlias(), join.getRightColumn());

                    boolean match = leftVal != null && leftVal.equals(rightVal);
                    boolean isLeftJoin = "LEFT".equalsIgnoreCase(join.getJoinType());

                    if (match) {
                        Map<String, Object> merged = new LinkedHashMap<>(leftRow);
                        merged.putAll(rightRow);
                        result.add(merged);
                    } else if (isLeftJoin) {
                        result.add(new LinkedHashMap<>(leftRow));
                    }
                }
            }
        }

        return result;
    }

    private Object getNestedValue(Map<String, Object> row, String alias, String column) {
        Object val = row.get(alias + "." + column);
        if (val == null) {
            val = row.get(column);
        }
        return val;
    }

    private Long extractFirstDataSourceId(String viewConfig) {
        if (viewConfig == null || viewConfig.isEmpty()) return null;
        try {
            Map<String, Object> config = JSON.parseObject(viewConfig, new TypeReference<Map<String, Object>>() {});
            Object dsId = config.get("dataSourceId");
            if (dsId != null) return Long.valueOf(dsId.toString());
        } catch (Exception ignored) {
        }
        return null;
    }

    private List<Map<String, Object>> resultSetToList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(metaData.getColumnName(i), rs.getObject(i));
            }
            result.add(row);
        }
        return result;
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}
