package com.lowcode.common.monitor.interceptor;

import com.lowcode.common.monitor.entity.SlowSqlLog;
import com.lowcode.common.monitor.report.MonitorReportClient;
import com.lowcode.common.monitor.store.MonitorDataStore;
import com.lowcode.common.monitor.util.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.time.LocalDateTime;
import java.util.Properties;

@Slf4j
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "queryCursor", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private long threshold = MonitorDataStore.getSlowSqlThreshold();

    @Autowired(required = false)
    @Lazy
    private MonitorReportClient reportClient;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        BoundSql boundSql = ms.getBoundSql(parameter);

        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            checkSlowSql(ms, parameter, boundSql, cost);
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    private void checkSlowSql(MappedStatement ms, Object parameter, BoundSql boundSql, long cost) {
        String sql = boundSql != null ? boundSql.getSql() : "";
        String mapperId = ms.getId();

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
            int lastDotIdx = mapperId.lastIndexOf('.');
            slowSqlLog.setMapperName(lastDotIdx > 0 ? mapperId.substring(0, lastDotIdx) : mapperId);
            slowSqlLog.setMethodName(lastDotIdx > 0 ? mapperId.substring(lastDotIdx + 1) : mapperId);
            slowSqlLog.setHappenTime(LocalDateTime.now());
            slowSqlLog.setDataSource(getDataSourceName(ms));

            MonitorDataStore.addSlowSqlLog(slowSqlLog);

            if (reportClient != null) {
                reportClient.reportSlowSql(slowSqlLog);
            }

            org.slf4j.Logger slowSqlLogger = org.slf4j.LoggerFactory.getLogger("SLOW_SQL");
            slowSqlLogger.info("{}ms | {} | {}", cost, mapperId, slowSqlLog.getSql());
        }
    }

    private String getDataSourceName(MappedStatement ms) {
        try {
            return ms.getConfiguration().getEnvironment().getId();
        } catch (Exception e) {
            return "default";
        }
    }

    private String formatSql(String sql) {
        if (sql == null) return "";
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Override
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
