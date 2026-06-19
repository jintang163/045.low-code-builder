package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.AesEncryptUtil;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.model.datasource.DataSourceHealthChecker;
import com.lowcode.model.datasource.DynamicDataSourceManager;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.mapper.DataSourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Slf4j
@Service
public class DataSourceService extends ServiceImpl<DataSourceMapper, DataSource> {

    @Autowired
    private DynamicDataSourceManager dynamicDataSourceManager;

    @Autowired
    private DataSourceHealthChecker healthChecker;

    @Autowired
    private VirtualViewService virtualViewService;

    public VirtualViewService getVirtualViewService() {
        return virtualViewService;
    }

    public boolean testConnection(DataSource dataSource) {
        if ("rest_api".equals(dataSource.getDbType())) {
            return testRestApiConnection(dataSource);
        }
        return JdbcUtil.testConnection(
                dataSource.getDbType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDbName(),
                dataSource.getUsername(),
                dataSource.getPassword()
        );
    }

    private boolean testRestApiConnection(DataSource dataSource) {
        try {
            java.net.URL url = new java.net.URL(dataSource.getRestApiUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(dataSource.getRestApiMethod() != null ? dataSource.getRestApiMethod() : "GET");
            conn.setConnectTimeout(dataSource.getConnectTimeout() != null ? dataSource.getConnectTimeout() : 5000);
            conn.setReadTimeout(dataSource.getReadTimeout() != null ? dataSource.getReadTimeout() : 5000);
            if (dataSource.getRestApiAuthType() != null && dataSource.getRestApiAuthToken() != null) {
                applyRestApiAuth(conn, dataSource.getRestApiAuthType(), dataSource.getRestApiAuthToken());
            }
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            log.error("REST API测试连接失败: {}", e.getMessage());
            return false;
        }
    }

    private void applyRestApiAuth(java.net.HttpURLConnection conn, String authType, String authToken) {
        switch (authType) {
            case "BEARER":
                conn.setRequestProperty("Authorization", "Bearer " + authToken);
                break;
            case "BASIC":
                conn.setRequestProperty("Authorization", "Basic " + authToken);
                break;
            case "API_KEY":
                conn.setRequestProperty("X-API-Key", authToken);
                break;
            default:
                break;
        }
    }

    public Connection getConnection(Long dataSourceId) {
        DataSource dataSource = getById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        if ("rest_api".equals(dataSource.getDbType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "REST API类型数据源不支持JDBC连接");
        }
        return dynamicDataSourceManager.getConnection(dataSourceId, dataSource);
    }

    public DataSource saveDataSource(DataSource dataSource) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getSourceCode, dataSource.getSourceCode());
        wrapper.eq(DataSource::getAppId, dataSource.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_EXISTS);
        }

        encryptSensitiveFields(dataSource);
        if (dataSource.getSourceType() == null) {
            dataSource.setSourceType("rest_api".equals(dataSource.getDbType()) ? "REST_API" : "DATABASE");
        }
        setPoolDefaults(dataSource);

        save(dataSource);
        return dataSource;
    }

    public DataSource updateDataSource(DataSource dataSource) {
        DataSource existing = getById(dataSource.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        if (dataSource.getPassword() != null && !dataSource.getPassword().isEmpty()) {
            if (!AesEncryptUtil.isEncrypted(dataSource.getPassword())) {
                dataSource.setPassword(AesEncryptUtil.encrypt(dataSource.getPassword()));
            }
        } else {
            dataSource.setPassword(existing.getPassword());
        }

        if (dataSource.getRestApiAuthToken() != null && !dataSource.getRestApiAuthToken().isEmpty()) {
            if (!AesEncryptUtil.isEncrypted(dataSource.getRestApiAuthToken())) {
                dataSource.setRestApiAuthToken(AesEncryptUtil.encrypt(dataSource.getRestApiAuthToken()));
            }
        } else {
            dataSource.setRestApiAuthToken(existing.getRestApiAuthToken());
        }

        updateById(dataSource);

        if (!"rest_api".equals(dataSource.getDbType())) {
            try {
                dynamicDataSourceManager.refreshDataSource(dataSource.getId(), getById(dataSource.getId()));
            } catch (Exception e) {
                log.warn("刷新数据源连接池失败: {}", e.getMessage());
            }
        }

        return getById(dataSource.getId());
    }

    public List<String> getTableNames(Long dataSourceId) {
        Connection conn = null;
        try {
            conn = getConnection(dataSourceId);
            return JdbcUtil.getTableNames(conn);
        } finally {
            JdbcUtil.close(conn, null, null);
        }
    }

    public List<Map<String, Object>> getTableColumns(Long dataSourceId, String tableName) {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getConnection(dataSourceId);
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getColumns(conn.getCatalog(), null, tableName, "%");
            List<Map<String, Object>> columns = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("columnName", rs.getString("COLUMN_NAME"));
                col.put("dataType", rs.getInt("DATA_TYPE"));
                col.put("typeName", rs.getString("TYPE_NAME"));
                col.put("columnSize", rs.getInt("COLUMN_SIZE"));
                col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                col.put("remarks", rs.getString("REMARKS"));
                col.put("defaultValue", rs.getString("COLUMN_DEF"));
                columns.add(col);
            }
            return columns;
        } catch (Exception e) {
            log.error("获取表字段信息失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            JdbcUtil.close(conn, null, null);
        }
    }

    public List<Map<String, Object>> getTablePrimaryKeys(Long dataSourceId, String tableName) {
        Connection conn = null;
        ResultSet rs = null;
        try {
            conn = getConnection(dataSourceId);
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getPrimaryKeys(conn.getCatalog(), null, tableName);
            List<Map<String, Object>> pks = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> pk = new LinkedHashMap<>();
                pk.put("columnName", rs.getString("COLUMN_NAME"));
                pk.put("pkName", rs.getString("PK_NAME"));
                pk.put("keySeq", rs.getInt("KEY_SEQ"));
                pks.add(pk);
            }
            return pks;
        } catch (Exception e) {
            log.error("获取主键信息失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignored) {}
            JdbcUtil.close(conn, null, null);
        }
    }

    public List<Map<String, Object>> callRestApi(Long dataSourceId, Map<String, Object> params) {
        DataSource ds = getById(dataSourceId);
        if (ds == null || !"rest_api".equals(ds.getDbType())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "数据源不存在或非REST API类型");
        }
        try {
            java.net.URL url = new java.net.URL(ds.getRestApiUrl());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod(ds.getRestApiMethod() != null ? ds.getRestApiMethod() : "GET");
            conn.setConnectTimeout(ds.getConnectTimeout() != null ? ds.getConnectTimeout() : 5000);
            conn.setReadTimeout(ds.getReadTimeout() != null ? ds.getReadTimeout() : 10000);
            conn.setRequestProperty("Content-Type", "application/json");

            if (ds.getRestApiHeaders() != null && !ds.getRestApiHeaders().isEmpty()) {
                Map<String, String> headers = com.alibaba.fastjson2.JSON.parseObject(ds.getRestApiHeaders(), Map.class);
                headers.forEach(conn::setRequestProperty);
            }

            String authToken = ds.getRestApiAuthToken();
            if (AesEncryptUtil.isEncrypted(authToken)) {
                authToken = AesEncryptUtil.decrypt(authToken);
            }
            if (ds.getRestApiAuthType() != null && authToken != null) {
                applyRestApiAuth(conn, ds.getRestApiAuthType(), authToken);
            }

            if ("POST".equals(ds.getRestApiMethod()) || "PUT".equals(ds.getRestApiMethod())) {
                conn.setDoOutput(true);
                String body = ds.getRestApiBody();
                if (params != null && !params.isEmpty()) {
                    body = com.alibaba.fastjson2.JSON.toJSONString(params);
                }
                if (body != null && !body.isEmpty()) {
                    conn.getOutputStream().write(body.getBytes("UTF-8"));
                }
            }

            int code = conn.getResponseCode();
            java.io.InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String response = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A").next();
            is.close();
            conn.disconnect();

            List<Map<String, Object>> result = new ArrayList<>();
            try {
                Object parsed = com.alibaba.fastjson2.JSON.parse(response);
                if (parsed instanceof List) {
                    return (List<Map<String, Object>>) parsed;
                } else if (parsed instanceof Map) {
                    result.add((Map<String, Object>) parsed);
                    return result;
                }
            } catch (Exception ignored) {
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("response", response);
            result.add(row);
            return result;
        } catch (Exception e) {
            log.error("REST API调用失败: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REST_API_CALL_ERROR, e.getMessage());
        }
    }

    public Map<String, Object> getPoolStatus(Long dataSourceId) {
        return dynamicDataSourceManager.getPoolStatus(dataSourceId);
    }

    public Map<String, Object> healthCheck(Long dataSourceId) {
        DataSource ds = getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        return healthChecker.checkDataSource(ds);
    }

    public void refreshPool(Long dataSourceId) {
        DataSource ds = getById(dataSourceId);
        if (ds == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        dynamicDataSourceManager.refreshDataSource(dataSourceId, ds);
    }

    public void deleteDataSource(Long id) {
        DataSource dataSource = getById(id);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        dynamicDataSourceManager.destroyDataSource(id);
        removeById(id);
    }

    private void encryptSensitiveFields(DataSource dataSource) {
        if (dataSource.getPassword() != null && !AesEncryptUtil.isEncrypted(dataSource.getPassword())) {
            dataSource.setPassword(AesEncryptUtil.encrypt(dataSource.getPassword()));
        }
        if (dataSource.getRestApiAuthToken() != null && !AesEncryptUtil.isEncrypted(dataSource.getRestApiAuthToken())) {
            dataSource.setRestApiAuthToken(AesEncryptUtil.encrypt(dataSource.getRestApiAuthToken()));
        }
    }

    private void setPoolDefaults(DataSource ds) {
        if (ds.getInitialSize() == null) ds.setInitialSize(2);
        if (ds.getMinIdle() == null) ds.setMinIdle(2);
        if (ds.getMaxActive() == null) ds.setMaxActive(10);
        if (ds.getMaxWait() == null) ds.setMaxWait(60000);
        if (ds.getTimeBetweenEvictionRunsMillis() == null) ds.setTimeBetweenEvictionRunsMillis(60000);
        if (ds.getMinEvictableIdleTimeMillis() == null) ds.setMinEvictableIdleTimeMillis(600000);
        if (ds.getMaxLifetime() == null) ds.setMaxLifetime(1800000);
        if (ds.getConnectionTimeout() == null) ds.setConnectionTimeout(30000);
        if (ds.getTestWhileIdle() == null) ds.setTestWhileIdle(true);
        if (ds.getTestOnBorrow() == null) ds.setTestOnBorrow(false);
        if (ds.getTestOnReturn() == null) ds.setTestOnReturn(false);
        if (ds.getConnectTimeout() == null) ds.setConnectTimeout(5000);
        if (ds.getReadTimeout() == null) ds.setReadTimeout(10000);
    }
}
