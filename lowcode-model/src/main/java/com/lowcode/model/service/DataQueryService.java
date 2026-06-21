package com.lowcode.model.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.result.Result;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.common.util.UserContext;
import com.lowcode.model.datasource.DynamicDataSourceManager;
import com.lowcode.model.entity.DataModel;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.entity.ModelField;
import com.lowcode.model.feign.AuthPermissionFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class DataQueryService {

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private DataModelService dataModelService;

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired(required = false)
    private AuthPermissionFeignClient authPermissionFeignClient;

    public List<Map<String, Object>> queryList(Long modelId, Map<String, Object> conditions) {
        return queryList(modelId, conditions, null, null);
    }

    public List<Map<String, Object>> queryList(Long modelId, Map<String, Object> conditions,
                                               String orderBy, String orderDir) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(model.getTableName());
        List<Object> params = new ArrayList<>();

        sql.append(buildWhereClause(conditions, params, model));
        appendRowLevelFilter(sql, model);

        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(orderBy);
            if (orderDir != null && !orderDir.isEmpty()) {
                sql.append(" ").append(orderDir);
            }
        }

        return executeQuery(model.getDataSourceId(), sql.toString(), params);
    }

    public IPage<Map<String, Object>> queryPage(Long modelId, Map<String, Object> conditions,
                                                int current, int size) {
        return queryPage(modelId, conditions, current, size, null, null);
    }

    public IPage<Map<String, Object>> queryPage(Long modelId, Map<String, Object> conditions,
                                                int current, int size,
                                                String orderBy, String orderDir) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(model.getTableName());
        StringBuilder dataSql = new StringBuilder("SELECT * FROM ").append(model.getTableName());
        List<Object> params = new ArrayList<>();

        String whereClause = buildWhereClause(conditions, params, model);
        countSql.append(whereClause);
        dataSql.append(whereClause);

        appendRowLevelFilter(countSql, model);
        appendRowLevelFilter(dataSql, model);

        if (orderBy != null && !orderBy.isEmpty()) {
            dataSql.append(" ORDER BY ").append(orderBy);
            if (orderDir != null && !orderDir.isEmpty()) {
                dataSql.append(" ").append(orderDir);
            }
        }
        dataSql.append(" LIMIT ").append((current - 1) * size).append(", ").append(size);

        long total = executeCount(model.getDataSourceId(), countSql.toString(), params);
        List<Map<String, Object>> records = executeQuery(model.getDataSourceId(), dataSql.toString(), params);

        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    public Map<String, Object> queryById(Long modelId, Object id) {
        DataModel model = dataModelService.getModelDetail(modelId);
        if (model == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据模型不存在");
        }

        ModelField pkField = model.getFields().stream()
                .filter(f -> f.getIsPrimary() != null && f.getIsPrimary() == 1)
                .findFirst()
                .orElse(model.getFields().get(0));

        StringBuilder sql = new StringBuilder("SELECT * FROM ")
                .append(model.getTableName())
                .append(" WHERE ").append(pkField.getColumnName()).append(" = ?");

        appendRowLevelFilter(sql, model);

        List<Map<String, Object>> result = executeQuery(model.getDataSourceId(), sql.toString(), Collections.singletonList(id));
        return result.isEmpty() ? null : result.get(0);
    }

    private String buildWhereClause(Map<String, Object> conditions, List<Object> params, DataModel model) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }

        Set<String> validColumns = new HashSet<>();
        for (ModelField field : model.getFields()) {
            validColumns.add(field.getColumnName());
            validColumns.add(field.getFieldName());
        }

        StringBuilder where = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> entry : conditions.entrySet()) {
            String column = entry.getKey();
            if (!validColumns.contains(column)) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (first) {
                where.append(" WHERE ");
                first = false;
            } else {
                where.append(" AND ");
            }
            where.append(column).append(" = ?");
            params.add(value);
        }
        return where.toString();
    }

    private void appendRowLevelFilter(StringBuilder sql, DataModel model) {
        if (authPermissionFeignClient == null) {
            return;
        }
        try {
            Long userId = UserContext.getCurrentUserId();
            Result<String> result = authPermissionFeignClient.getRowLevelFilter(model.getAppId(), model.getId());
            if (result != null && result.getCode() == 0 && result.getData() != null && !result.getData().isEmpty()) {
                String filter = result.getData();
                if (sql.toString().toUpperCase().contains(" WHERE ")) {
                    sql.insert(sql.toString().toUpperCase().indexOf(" WHERE ") + " WHERE ".length(), "(").append(") AND ");
                    int whereIdx = sql.toString().toUpperCase().indexOf(" WHERE ");
                    String before = sql.substring(0, whereIdx + " WHERE ".length());
                    String after = sql.substring(whereIdx + " WHERE ".length());
                    sql.setLength(0);
                    sql.append(before).append("(").append(filter).append(") AND ").append(after);
                } else {
                    sql.append(" WHERE (").append(filter).append(")");
                }
            }
        } catch (Exception e) {
            log.warn("获取行级权限过滤条件失败，跳过过滤: modelId={}, error={}", model.getId(), e.getMessage());
        }
    }

    private List<Map<String, Object>> executeQuery(Long dataSourceId, String sql, List<Object> params) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行数据查询SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            rs = stmt.executeQuery();
            return resultSetToList(rs);
        } catch (SQLException e) {
            log.error("数据查询执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, rs);
        }
    }

    private long executeCount(Long dataSourceId, String sql, List<Object> params) {
        DataSource ds = dataSourceService.getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = dynamicDataSourceManager.getConnection(dataSourceId, ds);
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            log.debug("执行计数查询SQL: dataSourceId={}, sql={}, params={}", dataSourceId, sql, params);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0;
        } catch (SQLException e) {
            log.error("计数查询执行失败: dataSourceId={}, sql={}, error={}", dataSourceId, sql, e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, stmt, rs);
        }
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

    public List<Map<String, Object>> executeSql(Long dataSourceId, String sql, List<Object> params) {
        validateSelectSql(sql);
        return executeQuery(dataSourceId, sql, params);
    }

    public IPage<Map<String, Object>> executeSqlPage(Long dataSourceId, String sql, List<Object> params,
                                                     int current, int size) {
        validateSelectSql(sql);

        String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
        String dataSql = sql + " LIMIT " + (current - 1) * size + ", " + size;

        long total = executeCount(dataSourceId, countSql, params);
        List<Map<String, Object>> records = executeQuery(dataSourceId, dataSql, params);

        Page<Map<String, Object>> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    public List<Map<String, Object>> testSql(Long dataSourceId, String sql, List<Object> params) {
        validateSelectSql(sql);
        String testSql = sql + " LIMIT 10";
        return executeQuery(dataSourceId, testSql, params);
    }

    private void validateSelectSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "SQL语句不能为空");
        }
        String trimmedSql = sql.trim().toUpperCase();
        if (!trimmedSql.startsWith("SELECT")) {
            throw new BusinessException(ErrorCode.SQL_NOT_ALLOWED);
        }
        String[] dangerousKeywords = {"INSERT ", "UPDATE ", "DELETE ", "DROP ", "CREATE ", "ALTER ",
                "TRUNCATE ", "REPLACE ", "MERGE ", "GRANT ", "REVOKE ", "EXEC ", "EXECUTE ",
                "CALL ", "LOAD_FILE ", "INTO OUTFILE ", "INTO DUMPFILE "};
        for (String keyword : dangerousKeywords) {
            if (trimmedSql.contains(keyword)) {
                throw new BusinessException(ErrorCode.SQL_NOT_ALLOWED);
            }
        }
    }
}
