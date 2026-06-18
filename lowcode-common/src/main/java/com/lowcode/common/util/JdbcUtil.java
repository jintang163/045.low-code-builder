package com.lowcode.common.util;

import com.lowcode.common.enums.DbTypeEnum;
import com.lowcode.common.exception.BusinessException;
import com.lowcode.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

@Slf4j
public class JdbcUtil {

    public static Connection getConnection(String dbType, String host, Integer port, String dbName,
                                           String username, String password) {
        DbTypeEnum typeEnum = DbTypeEnum.getByCode(dbType);
        String url = typeEnum.buildUrl(host, port, dbName);
        try {
            Class.forName(typeEnum.getDriverClass());
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            log.error("获取数据库连接失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.DATA_SOURCE_CONNECT_ERROR, e.getMessage());
        }
    }

    public static void close(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (stmt != null) stmt.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            log.error("关闭数据库连接失败: {}", e.getMessage());
        }
    }

    public static boolean testConnection(String dbType, String host, Integer port, String dbName,
                                         String username, String password) {
        Connection conn = null;
        try {
            conn = getConnection(dbType, host, port, dbName, username, password);
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            log.error("测试连接失败: {}", e.getMessage());
            return false;
        } finally {
            close(conn, null, null);
        }
    }

    public static List<String> getTableNames(Connection conn) {
        List<String> tables = new ArrayList<>();
        ResultSet rs = null;
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"});
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        } catch (SQLException e) {
            log.error("获取表名失败: {}", e.getMessage());
        } finally {
            close(null, null, rs);
        }
        return tables;
    }

    public static void executeSql(Connection conn, String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            stmt.execute(sql);
        } catch (SQLException e) {
            log.error("执行SQL失败: {}, SQL: {}", e.getMessage(), sql);
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            close(null, stmt, null);
        }
    }

    public static int executeUpdate(Connection conn, String sql) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            return stmt.executeUpdate(sql);
        } catch (SQLException e) {
            log.error("执行SQL更新失败: {}, SQL: {}", e.getMessage(), sql);
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            close(null, stmt, null);
        }
    }

    public static List<Map<String, Object>> executeQuery(Connection conn, String sql) {
        List<Map<String, Object>> result = new ArrayList<>();
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
        } catch (SQLException e) {
            log.error("执行SQL查询失败: {}, SQL: {}", e.getMessage(), sql);
            throw new BusinessException(ErrorCode.SQL_EXECUTE_ERROR, e.getMessage());
        } finally {
            close(null, stmt, rs);
        }
        return result;
    }
}
