package com.lowcode.model.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import com.lowcode.common.util.JdbcUtil;
import com.lowcode.model.entity.DataSource;
import com.lowcode.model.mapper.DataSourceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;

@Slf4j
@Service
public class DataSourceService extends ServiceImpl<DataSourceMapper, DataSource> {

    public boolean testConnection(DataSource dataSource) {
        return JdbcUtil.testConnection(
                dataSource.getDbType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDbName(),
                dataSource.getUsername(),
                dataSource.getPassword()
        );
    }

    public Connection getConnection(Long dataSourceId) {
        DataSource dataSource = getById(dataSourceId);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        return JdbcUtil.getConnection(
                dataSource.getDbType(),
                dataSource.getHost(),
                dataSource.getPort(),
                dataSource.getDbName(),
                dataSource.getUsername(),
                dataSource.getPassword()
        );
    }

    public DataSource saveDataSource(DataSource dataSource) {
        LambdaQueryWrapper<DataSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DataSource::getSourceCode, dataSource.getSourceCode());
        wrapper.eq(DataSource::getAppId, dataSource.getAppId());
        Long count = count(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.DATA_SOURCE_EXISTS);
        }

        if (dataSource.getPassword() != null && !dataSource.getPassword().startsWith("ENC(")) {
            dataSource.setPassword(encryptPassword(dataSource.getPassword()));
        }

        save(dataSource);
        return dataSource;
    }

    public DataSource updateDataSource(DataSource dataSource) {
        DataSource existing = getById(dataSource.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }

        if (dataSource.getPassword() != null && !dataSource.getPassword().startsWith("ENC(")) {
            dataSource.setPassword(encryptPassword(dataSource.getPassword()));
        } else {
            dataSource.setPassword(existing.getPassword());
        }

        updateById(dataSource);
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

    public void deleteDataSource(Long id) {
        DataSource dataSource = getById(id);
        if (dataSource == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "数据源不存在");
        }
        removeById(id);
    }

    private String encryptPassword(String password) {
        String salt = "lowcode-platform-2024";
        String encrypted = DigestUtils.md5DigestAsHex((password + salt).getBytes(StandardCharsets.UTF_8));
        return "ENC(" + encrypted + ")";
    }
}
