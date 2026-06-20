package com.lowcode.common.monitor.interceptor;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.lowcode.common.monitor.entity.SlowSqlLog;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.monitor.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
public class SlowSqlInterceptor implements InnerInterceptor {

    private long threshold = MonitorDataStore.getSlowSqlThreshold();

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        String sql = boundSql.getSql();
        String mapperId = ms.getId();
        long startTime = System.currentTimeMillis();

        try {
            InnerInterceptor.super.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            checkSlowSql(sql, mapperId, parameter, cost);
        }
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        String sql = boundSql.getSql();
        String mapperId = ms.getId();
        long startTime = System.currentTimeMillis();

        try {
            InnerInterceptor.super.beforeUpdate(executor, ms, parameter);
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            checkSlowSql(sql, mapperId, parameter, cost);
        }
    }

    private void checkSlowSql(String sql, String mapperId, Object parameter, long cost) {
        if (cost >= threshold) {
            SlowSqlLog slowSqlLog = new SlowSqlLog();
            slowSqlLog.setId(String.valueOf(System.currentTimeMillis()));
            slowSqlLog.setTraceId(TraceIdUtil.getTraceId());
            slowSqlLog.setSql(formatSql(sql));
            try {
                slowSqlLog.setParams(parameter != null ? parameter.toString() : null);
            } catch (Exception e) {
                slowSqlLog.setParams("unparseable");
            }
            slowSqlLog.setExecuteTime(cost);
            slowSqlLog.setThreshold(threshold);
            slowSqlLog.setMapperName(mapperId.substring(0, mapperId.lastIndexOf('.')));
            slowSqlLog.setMethodName(mapperId.substring(mapperId.lastIndexOf('.') + 1));
            slowSqlLog.setHappenTime(LocalDateTime.now());

            MonitorDataStore.addSlowSqlLog(slowSqlLog);
            log.warn("[SlowSQL] {}ms {} - {}", cost, mapperId, slowSqlLog.getSql());
        }
    }

    private String formatSql(String sql) {
        if (sql == null) return "";
        return sql.replaceAll("\\s+", " ").trim();
    }

    public void setProperties(Properties properties) {
        String thresholdStr = properties.getProperty("threshold");
        if (thresholdStr != null && !thresholdStr.isEmpty()) {
            try {
                this.threshold = Long.parseLong(thresholdStr);
            } catch (NumberFormatException ignored) {
            }
        }
    }
}
