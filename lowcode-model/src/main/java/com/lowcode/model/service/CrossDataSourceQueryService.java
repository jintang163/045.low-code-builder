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
import java.util.stream.Collectors;

@Slf4j
@Service
public class CrossDataSourceQueryService {

    private static final int DEFAULT_LIMIT = 1000;
    private static final int MAX_LIMIT = 10000;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    public List<Map<String, Object>> queryVirtualView(Long viewId) {
        return queryVirtualView(viewId, DEFAULT_LIMIT);
    }

    public List<Map<String, Object>> queryVirtualView(Long viewId, Integer limit) {
        VirtualView virtualView = dataSourceService.getVirtualViewService().getById(viewId);
        if (virtualView == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "虚拟视图不存在");
        }

        List<VirtualViewJoin> joins = dataSourceService.getVirtualViewService()
                .parseJoinConfig(virtualView.getJoinConfig());

        int queryLimit = normalizeLimit(limit);

        if (joins == null || joins.isEmpty()) {
            if (virtualView.getViewSql() != null && !virtualView.getViewSql().isEmpty()) {
                Long dsId = extractFirstDataSourceId(virtualView.getViewConfig());
                return executeQueryOnDataSource(dsId, buildLimitedSql(virtualView.getViewSql(), queryLimit));
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
            List<Map<String, Object>> data = fetchFromDataSource(sourceId, joins, queryLimit);
            dataBySource.put(sourceId, data);
        }

        return performJoin(joins, dataBySource, queryLimit);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String buildLimitedSql(String baseSql, int limit) {
        String trimmed = baseSql.trim();
        if (trimmed.toUpperCase().contains("LIMIT")) {
            return trimmed;
        }
        return trimmed + " LIMIT " + limit;
    }

    private List<Map<String, Object>> fetchFromDataSource(Long dataSourceId, List<VirtualViewJoin> joins, int limit) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在: " + dataSourceId);
        }

        Set<String> selectColumns = new LinkedHashSet<>();
        Set<String> fromTables = new LinkedHashSet<>();

        for (VirtualViewJoin join : joins) {
            if (join.getLeftDataSourceId().equals(dataSourceId)) {
                String alias = join.getLeftAlias() != null ? join.getLeftAlias() : join.getLeftTable();
                fromTables.add(join.getLeftTable() + " AS " + alias);
                selectColumns.add(alias + "." + join.getLeftColumn() + " AS "
                        + buildColumnAlias(alias, join.getLeftColumn()));
            }
            if (join.getRightDataSourceId().equals(dataSourceId)) {
                String alias = join.getRightAlias() != null ? join.getRightAlias() : join.getRightTable();
                fromTables.add(join.getRightTable() + " AS " + alias);
                selectColumns.add(alias + "." + join.getRightColumn() + " AS "
                        + buildColumnAlias(alias, join.getRightColumn()));
            }
        }

        String sql = buildSelectSqlWithAliases(fromTables, selectColumns, limit);
        return executeQueryOnDataSource(dataSourceId, sql);
    }

    private String buildColumnAlias(String alias, String column) {
        return alias + "_" + column;
    }

    private String buildSelectSqlWithAliases(Set<String> fromTables, Set<String> selectColumns, int limit) {
        StringBuilder sb = new StringBuilder("SELECT ");
        sb.append(String.join(", ", selectColumns));
        sb.append(" FROM ");
        sb.append(String.join(", ", fromTables));
        sb.append(" LIMIT ").append(limit);
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
            log.debug("执行跨源查询SQL: dataSourceId={}, sql={}", dataSourceId, sql);
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
                                                   Map<Long, List<Map<String, Object>>> dataBySource,
                                                   int limit) {
        if (joins.isEmpty()) {
            return Collections.emptyList();
        }

        VirtualViewJoin firstJoin = joins.get(0);
        List<Map<String, Object>> result = new ArrayList<>(
                dataBySource.getOrDefault(firstJoin.getLeftDataSourceId(), Collections.emptyList()));

        for (VirtualViewJoin join : joins) {
            String leftAlias = join.getLeftAlias() != null ? join.getLeftAlias() : join.getLeftTable();
            String rightAlias = join.getRightAlias() != null ? join.getRightAlias() : join.getRightTable();
            String leftColAlias = buildColumnAlias(leftAlias, join.getLeftColumn());
            String rightColAlias = buildColumnAlias(rightAlias, join.getRightColumn());

            List<Map<String, Object>> leftData = result;
            List<Map<String, Object>> rightData = dataBySource.getOrDefault(join.getRightDataSourceId(), Collections.emptyList());

            boolean isLeftJoin = "LEFT".equalsIgnoreCase(join.getJoinType());

            Map<Object, List<Map<String, Object>>> rightIndex = new HashMap<>();
            for (Map<String, Object> rightRow : rightData) {
                Object rightVal = rightRow.get(rightColAlias);
                if (rightVal == null) {
                    rightVal = rightRow.get(join.getRightColumn());
                }
                if (rightVal != null) {
                    rightIndex.computeIfAbsent(rightVal, k -> new ArrayList<>()).add(rightRow);
                }
            }

            List<Map<String, Object>> newResult = new ArrayList<>();
            for (Map<String, Object> leftRow : leftData) {
                Object leftVal = leftRow.get(leftColAlias);
                if (leftVal == null) {
                    leftVal = leftRow.get(join.getLeftColumn());
                }

                List<Map<String, Object>> matchingRights = rightIndex.getOrDefault(leftVal, Collections.emptyList());

                if (!matchingRights.isEmpty()) {
                    for (Map<String, Object> rightRow : matchingRights) {
                        Map<String, Object> merged = new LinkedHashMap<>(leftRow);
                        merged.putAll(prefixColumns(rightRow, rightAlias));
                        newResult.add(merged);
                        if (newResult.size() >= limit) {
                            break;
                        }
                    }
                } else if (isLeftJoin) {
                    Map<String, Object> merged = new LinkedHashMap<>(leftRow);
                    for (Map<String, Object> rightRow : rightData) {
                        for (String key : rightRow.keySet()) {
                            String prefixedKey = rightAlias + "_" + key;
                            if (!merged.containsKey(prefixedKey)) {
                                merged.put(prefixedKey, null);
                            }
                        }
                        break;
                    }
                    newResult.add(merged);
                }

                if (newResult.size() >= limit) {
                    break;
                }
            }

            result = newResult;
            if (result.size() >= limit) {
                break;
            }
        }

        return result.stream().limit(limit).collect(Collectors.toList());
    }

    private Map<String, Object> prefixColumns(Map<String, Object> row, String alias) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            result.put(alias + "_" + entry.getKey(), entry.getValue());
        }
        return result;
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
                row.put(metaData.getColumnLabel(i), rs.getObject(i));
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
