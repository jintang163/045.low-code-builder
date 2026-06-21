package com.lowcode.flow.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class RpaDatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化RPA数据库表...");

        try {
            ClassPathResource resource = new ClassPathResource("db/rpa.sql");
            String sqlScript = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);

            String[] statements = sqlScript.split(";");
            int executedCount = 0;

            for (String statement : statements) {
                String trimmedStmt = statement.trim();
                if (!trimmedStmt.isEmpty() && !trimmedStmt.startsWith("--") && !trimmedStmt.startsWith("SET")) {
                    try {
                        jdbcTemplate.execute(trimmedStmt);
                        executedCount++;
                    } catch (Exception e) {
                        log.debug("SQL执行跳过（可能已存在）: {}", e.getMessage());
                    }
                } else if (trimmedStmt.startsWith("SET")) {
                    try {
                        jdbcTemplate.execute(trimmedStmt);
                    } catch (Exception e) {
                        log.debug("SET语句执行跳过: {}", e.getMessage());
                    }
                } else if (!trimmedStmt.startsWith("--") && !trimmedStmt.isEmpty()) {
                    try {
                        jdbcTemplate.execute(trimmedStmt);
                        executedCount++;
                    } catch (Exception e) {
                        log.debug("预处理SQL执行跳过: {}", e.getMessage());
                    }
                }
            }

            log.info("RPA数据库表初始化完成，执行了{}条语句", executedCount);

            try {
                Integer scriptCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_rpa_script", Integer.class);
                Integer executionCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM sys_rpa_execution", Integer.class);
                log.info("RPA表数据统计 - 脚本数: {}, 执行记录数: {}", scriptCount, executionCount);
            } catch (Exception e) {
                log.warn("查询RPA表统计信息失败", e);
            }

        } catch (Exception e) {
            log.error("RPA数据库表初始化失败", e);
        }
    }
}
