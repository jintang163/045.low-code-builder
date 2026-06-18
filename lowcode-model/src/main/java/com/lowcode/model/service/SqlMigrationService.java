package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.model.entity.SqlMigration;
import com.lowcode.model.mapper.SqlMigrationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class SqlMigrationService extends ServiceImpl<SqlMigrationMapper, SqlMigration> {

    @Autowired
    private DataSourceService dataSourceService;

    public List<SqlMigration> getMigrations(Long appId, Long dataSourceId) {
        LambdaQueryWrapper<SqlMigration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SqlMigration::getAppId, appId);
        if (dataSourceId != null) {
            wrapper.eq(SqlMigration::getDataSourceId, dataSourceId);
        }
        wrapper.orderByDesc(SqlMigration::getCreatedTime);
        return list(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public SqlMigration executeMigration(Long migrationId) {
        SqlMigration migration = getById(migrationId);
        if (migration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "迁移记录不存在");
        }

        if (migration.getStatus() == 1) {
            throw new BusinessException("该迁移已执行，请勿重复执行");
        }

        Connection conn = null;
        try {
            conn = dataSourceService.getConnection(migration.getDataSourceId());
            conn.setAutoCommit(false);

            String[] sqlStatements = migration.getSqlContent().split(";");
            for (String sql : sqlStatements) {
                String trimmedSql = sql.trim();
                if (!trimmedSql.isEmpty()) {
                    JdbcUtil.executeSql(conn, trimmedSql);
                }
            }

            conn.commit();

            migration.setStatus(1);
            migration.setExecuteTime(LocalDateTime.now());
            migration.setExecuteResult("执行成功");
            updateById(migration);

            return migration;
        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (Exception rollbackEx) {
                log.error("回滚失败: {}", rollbackEx.getMessage());
            }
            log.error("执行迁移失败: {}", e.getMessage());
            migration.setStatus(2);
            migration.setExecuteTime(LocalDateTime.now());
            migration.setExecuteResult("执行失败: " + e.getMessage());
            updateById(migration);
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            JdbcUtil.close(conn, null, null);
        }
    }

    public SqlMigration getMigrationDetail(Long id) {
        SqlMigration migration = getById(id);
        if (migration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "迁移记录不存在");
        }
        return migration;
    }
}
