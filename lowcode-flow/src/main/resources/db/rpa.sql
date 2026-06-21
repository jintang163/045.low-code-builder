-- RPA自动化模块数据库表结构
-- 集成到低代码平台数据库初始化流程中

-- 检查并创建RPA脚本表
CREATE TABLE IF NOT EXISTS `sys_rpa_script` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL COMMENT '应用ID',
  `script_name` varchar(100) NOT NULL COMMENT '脚本名称',
  `script_code` varchar(100) NOT NULL COMMENT '脚本编码',
  `description` varchar(500) DEFAULT NULL COMMENT '脚本描述',
  `script_content` longtext COMMENT '脚本内容(JSON格式)',
  `script_type` varchar(50) DEFAULT 'BROWSER' COMMENT '脚本类型:BROWSER-浏览器自动化,DATA_EXTRACT-数据抓取,FORM_FILL-表单填写,CUSTOM-自定义',
  `target_url` varchar(500) DEFAULT NULL COMMENT '目标网站URL',
  `timeout` int DEFAULT 300 COMMENT '超时时间(秒)',
  `status` varchar(20) DEFAULT 'DRAFT' COMMENT '状态:DRAFT-草稿,PUBLISHED-已发布',
  `version` varchar(20) DEFAULT '1.0.0' COMMENT '版本号',
  `schedule_enabled` tinyint DEFAULT 0 COMMENT '是否启用定时调度:0-否,1-是',
  `cron_expression` varchar(100) DEFAULT NULL COMMENT 'Cron表达式',
  `schedule_params` text COMMENT '定时执行参数(JSON)',
  `last_execute_time` datetime DEFAULT NULL COMMENT '上次执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `execute_count` bigint DEFAULT 0 COMMENT '总执行次数',
  `success_count` bigint DEFAULT 0 COMMENT '成功次数',
  `fail_count` bigint DEFAULT 0 COMMENT '失败次数',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_code` (`app_id`, `script_code`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_schedule_enabled` (`schedule_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA脚本表';

-- 检查并创建RPA执行记录表
CREATE TABLE IF NOT EXISTS `sys_rpa_execution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `script_id` bigint NOT NULL COMMENT '脚本ID',
  `execution_no` varchar(50) NOT NULL COMMENT '执行编号',
  `trigger_type` varchar(20) DEFAULT 'MANUAL' COMMENT '触发方式:MANUAL-手动,SCHEDULE-定时,API-API调用,LOGIC-逻辑流程',
  `trigger_logic_id` bigint DEFAULT NULL COMMENT '触发的逻辑流程ID',
  `trigger_node_id` varchar(100) DEFAULT NULL COMMENT '触发的节点ID',
  `input_params` longtext COMMENT '输入参数(JSON)',
  `output_result` longtext COMMENT '输出结果(JSON)',
  `status` varchar(20) DEFAULT 'PENDING' COMMENT '执行状态:PENDING-等待,RUNNING-执行中,SUCCESS-成功,FAILED-失败',
  `error_message` text COMMENT '错误信息',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration` bigint DEFAULT NULL COMMENT '执行耗时(毫秒)',
  `execution_log` longtext COMMENT '执行日志',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '逻辑删除:0-未删除,1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_execution_no` (`execution_no`),
  KEY `idx_script_id` (`script_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_time` (`start_time`),
  KEY `idx_trigger_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RPA执行记录表';

-- 为已存在的表添加新增字段（兼容升级）
SET @dbname = DATABASE();
SET @tablename = 'sys_rpa_script';
SET @columnname = 'schedule_enabled';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' tinyint DEFAULT 0 COMMENT ''是否启用定时调度:0-否,1-是'' AFTER version')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'cron_expression';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' varchar(100) DEFAULT NULL COMMENT ''Cron表达式'' AFTER schedule_enabled')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'schedule_params';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' text COMMENT ''定时执行参数(JSON)'' AFTER cron_expression')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'last_execute_time';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' datetime DEFAULT NULL COMMENT ''上次执行时间'' AFTER schedule_params')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'next_execute_time';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' datetime DEFAULT NULL COMMENT ''下次执行时间'' AFTER last_execute_time')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'execute_count';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' bigint DEFAULT 0 COMMENT ''总执行次数'' AFTER next_execute_time')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'success_count';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' bigint DEFAULT 0 COMMENT ''成功次数'' AFTER execute_count')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @columnname = 'fail_count';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (column_name = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' bigint DEFAULT 0 COMMENT ''失败次数'' AFTER success_count')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 添加索引
SET @indexname = 'idx_schedule_enabled';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (index_name = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (schedule_enabled)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @indexname = 'idx_trigger_type';
SET @tablename = 'sys_rpa_execution';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE
      (table_schema = @dbname)
      AND (table_name = @tablename)
      AND (index_name = @indexname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD INDEX ', @indexname, ' (trigger_type)')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
