package com.lowcode.model.datasource;

import com.lowcode.common.enums.DbTypeEnum;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.AesEncryptUtil;
import com.lowcode.model.entity.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class DynamicDataSourceManager {

    private final Map<Long, HikariDataSourceWrapper> dataSourcePool = new ConcurrentHashMap<>();

    public synchronized Connection getConnection(Long dataSourceId, DataSource dataSourceConfig) {
        HikariDataSourceWrapper wrapper = dataSourcePool.get(dataSourceId);
        if (wrapper != null && !wrapper.isClosed()) {
            try {
                return wrapper.getDataSource().getConnection();
            } catch (SQLException e) {
                log.warn("从连接池获取连接失败，尝试重建: {}", e.getMessage());
                destroyDataSource(dataSourceId);
            }
        }
        return createAndGetConnection(dataSourceId, dataSourceConfig);
    }

    private Connection createAndGetConnection(Long dataSourceId, DataSource dataSourceConfig) {
        HikariDataSourceWrapper wrapper = createDataSource(dataSourceConfig);
        dataSourcePool.put(dataSourceId, wrapper);
        try {
            return wrapper.getDataSource().getConnection();
        } catch (SQLException e) {
            log.error("从新建连接池获取连接失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECT_ERROR, e.getMessage());
        }
    }

    private HikariDataSourceWrapper createDataSource(DataSource config) {
        DbTypeEnum typeEnum = DbTypeEnum.getByCode(config.getDbType());
        String url = typeEnum.buildUrl(config.getHost(), config.getPort(), config.getDbName());
        String password = AesEncryptUtil.isEncrypted(config.getPassword())
                ? AesEncryptUtil.decrypt(config.getPassword())
                : config.getPassword();

        com.zaxxer.hikari.HikariConfig hikariConfig = new com.zaxxer.hikari.HikariConfig();
        hikariConfig.setJdbcUrl(url);
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName(typeEnum.getDriverClass());

        hikariConfig.setPoolName("lowcode-ds-" + config.getId());
        hikariConfig.setMinimumIdle(config.getMinIdle() != null ? config.getMinIdle() : 2);
        hikariConfig.setMaximumPoolSize(config.getMaxActive() != null ? config.getMaxActive() : 10);
        hikariConfig.setConnectionTimeout(config.getConnectionTimeout() != null ? config.getConnectionTimeout() : 30000);
        hikariConfig.setIdleTimeout(config.getMinEvictableIdleTimeMillis() != null ? config.getMinEvictableIdleTimeMillis() : 600000);
        hikariConfig.setMaxLifetime(config.getMaxLifetime() != null ? config.getMaxLifetime() : 1800000);
        hikariConfig.setConnectionTestQuery(config.getValidationQuery() != null ? config.getValidationQuery() : "SELECT 1");

        if (config.getInitialSize() != null) {
            hikariConfig.setMinimumIdle(Math.min(config.getInitialSize(), hikariConfig.getMaximumPoolSize()));
        }

        com.zaxxer.hikari.HikariDataSource ds = new com.zaxxer.hikari.HikariDataSource(hikariConfig);
        return new HikariDataSourceWrapper(ds, config.getId());
    }

    public synchronized void destroyDataSource(Long dataSourceId) {
        HikariDataSourceWrapper wrapper = dataSourcePool.remove(dataSourceId);
        if (wrapper != null && !wrapper.isClosed()) {
            wrapper.close();
            log.info("数据源连接池已销毁: dataSourceId={}", dataSourceId);
        }
    }

    public synchronized void refreshDataSource(Long dataSourceId, DataSource dataSourceConfig) {
        destroyDataSource(dataSourceId);
        HikariDataSourceWrapper wrapper = createDataSource(dataSourceConfig);
        dataSourcePool.put(dataSourceId, wrapper);
        log.info("数据源连接池已刷新: dataSourceId={}", dataSourceId);
    }

    public Map<String, Object> getPoolStatus(Long dataSourceId) {
        HikariDataSourceWrapper wrapper = dataSourcePool.get(dataSourceId);
        Map<String, Object> status = new HashMap<>();
        if (wrapper != null && !wrapper.isClosed()) {
            com.zaxxer.hikari.HikariDataSource ds = wrapper.getDataSource();
            status.put("activeConnections", ds.getHikariPoolMXBean().getActiveConnections());
            status.put("idleConnections", ds.getHikariPoolMXBean().getIdleConnections());
            status.put("totalConnections", ds.getHikariPoolMXBean().getTotalConnections());
            status.put("threadsAwaitingConnection", ds.getHikariPoolMXBean().getThreadsAwaitingConnection());
            status.put("poolName", ds.getPoolName());
            status.put("closed", ds.isClosed());
        } else {
            status.put("activeConnections", 0);
            status.put("idleConnections", 0);
            status.put("totalConnections", 0);
            status.put("threadsAwaitingConnection", 0);
            status.put("closed", true);
        }
        return status;
    }

    public boolean testPoolConnection(Long dataSourceId) {
        HikariDataSourceWrapper wrapper = dataSourcePool.get(dataSourceId);
        if (wrapper == null || wrapper.isClosed()) {
            return false;
        }
        try (Connection conn = wrapper.getDataSource().getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            log.error("连接池健康检查失败: dataSourceId={}, error={}", dataSourceId, e.getMessage());
            return false;
        }
    }

    public void destroyAll() {
        dataSourcePool.forEach((id, wrapper) -> {
            if (!wrapper.isClosed()) {
                wrapper.close();
            }
        });
        dataSourcePool.clear();
        log.info("所有数据源连接池已销毁");
    }
}
