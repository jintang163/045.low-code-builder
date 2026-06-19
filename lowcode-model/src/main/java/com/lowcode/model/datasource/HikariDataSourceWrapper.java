package com.lowcode.model.datasource;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;

@Getter
public class HikariDataSourceWrapper implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final Long dataSourceId;

    public HikariDataSourceWrapper(HikariDataSource dataSource, Long dataSourceId) {
        this.dataSource = dataSource;
        this.dataSourceId = dataSourceId;
    }

    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}
