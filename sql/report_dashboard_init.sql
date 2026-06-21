-- =============================================
-- 报表与大屏设计器 数据库表结构
-- =============================================

-- 报表主表
DROP TABLE IF EXISTS `sys_report`;
CREATE TABLE `sys_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `report_name` varchar(100) NOT NULL COMMENT '报表名称',
  `report_code` varchar(100) NOT NULL COMMENT '报表编码',
  `report_type` varchar(50) DEFAULT NULL COMMENT '报表类型',
  `description` varchar(500) DEFAULT NULL COMMENT '报表描述',
  `layout_type` varchar(50) DEFAULT 'flow' COMMENT '布局类型',
  `page_config` text COMMENT '页面配置',
  `report_config` text COMMENT '报表配置',
  `status` tinyint DEFAULT 0 COMMENT '状态 0草稿 1已发布',
  `version` varchar(20) DEFAULT '1.0.0' COMMENT '版本号',
  `schedule_config` text COMMENT '定时任务配置',
  `email_config` text COMMENT '邮件配置',
  `auto_refresh` tinyint DEFAULT 0 COMMENT '是否自动刷新 0否 1是',
  `refresh_interval` int DEFAULT 30 COMMENT '刷新间隔(秒)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_code` (`app_id`, `report_code`, `deleted`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表表';

-- 报表组件表
DROP TABLE IF EXISTS `sys_report_component`;
CREATE TABLE `sys_report_component` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `component_id` varchar(64) NOT NULL COMMENT '组件ID',
  `component_type` varchar(50) NOT NULL COMMENT '组件类型',
  `component_name` varchar(100) NOT NULL COMMENT '组件名称',
  `parent_id` varchar(64) DEFAULT NULL COMMENT '父组件ID',
  `slot_name` varchar(50) DEFAULT NULL COMMENT '插槽名称',
  `props_config` text COMMENT '属性配置',
  `style_config` text COMMENT '样式配置',
  `event_config` text COMMENT '事件配置',
  `data_source_config` text COMMENT '数据源配置',
  `validation_config` text COMMENT '校验配置',
  `linkage_config` text COMMENT '联动配置',
  `position_x` int DEFAULT 0 COMMENT 'X坐标',
  `position_y` int DEFAULT 0 COMMENT 'Y坐标',
  `width` int DEFAULT NULL COMMENT '宽度',
  `height` int DEFAULT NULL COMMENT '高度',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `z_index` int DEFAULT 1 COMMENT '层级',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_component_id` (`component_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表组件表';

-- 大屏主表
DROP TABLE IF EXISTS `sys_dashboard`;
CREATE TABLE `sys_dashboard` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `dashboard_name` varchar(100) NOT NULL COMMENT '大屏名称',
  `dashboard_code` varchar(100) NOT NULL COMMENT '大屏编码',
  `description` varchar(500) DEFAULT NULL COMMENT '大屏描述',
  `layout_type` varchar(50) DEFAULT 'free' COMMENT '布局类型',
  `width` int DEFAULT 1920 COMMENT '宽度',
  `height` int DEFAULT 1080 COMMENT '高度',
  `background_color` varchar(20) DEFAULT '#0a0e27' COMMENT '背景颜色',
  `background_image` varchar(500) DEFAULT NULL COMMENT '背景图片',
  `theme` varchar(50) DEFAULT 'tech-blue' COMMENT '主题',
  `status` tinyint DEFAULT 0 COMMENT '状态 0草稿 1已发布',
  `version` varchar(20) DEFAULT '1.0.0' COMMENT '版本号',
  `auto_refresh` tinyint DEFAULT 0 COMMENT '是否自动刷新 0否 1是',
  `refresh_interval` int DEFAULT 30 COMMENT '刷新间隔(秒)',
  `carousel_config` text COMMENT '轮播配置',
  `display_config` text COMMENT '展示配置',
  `share_config` text COMMENT '分享配置',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dashboard_code` (`app_id`, `dashboard_code`, `deleted`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大屏表';

-- 大屏组件表
DROP TABLE IF EXISTS `sys_dashboard_component`;
CREATE TABLE `sys_dashboard_component` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `dashboard_id` bigint NOT NULL COMMENT '大屏ID',
  `component_id` varchar(64) NOT NULL COMMENT '组件ID',
  `component_type` varchar(50) NOT NULL COMMENT '组件类型',
  `component_name` varchar(100) NOT NULL COMMENT '组件名称',
  `props_config` text COMMENT '属性配置',
  `style_config` text COMMENT '样式配置',
  `data_source_config` text COMMENT '数据源配置',
  `refresh_config` text COMMENT '刷新配置',
  `linkage_config` text COMMENT '联动配置',
  `position_x` int DEFAULT 0 COMMENT 'X坐标',
  `position_y` int DEFAULT 0 COMMENT 'Y坐标',
  `width` int DEFAULT 300 COMMENT '宽度',
  `height` int DEFAULT 200 COMMENT '高度',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `z_index` int DEFAULT 1 COMMENT '层级',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_dashboard_id` (`dashboard_id`),
  KEY `idx_component_id` (`component_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大屏组件表';

-- 报表定时任务表
DROP TABLE IF EXISTS `sys_report_schedule`;
CREATE TABLE `sys_report_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `report_id` bigint NOT NULL COMMENT '报表ID',
  `schedule_name` varchar(100) NOT NULL COMMENT '任务名称',
  `cron_expression` varchar(100) NOT NULL COMMENT 'Cron表达式',
  `email_subject` varchar(200) DEFAULT NULL COMMENT '邮件主题',
  `email_recipients` varchar(1000) DEFAULT NULL COMMENT '收件人(多个逗号分隔)',
  `email_cc` varchar(1000) DEFAULT NULL COMMENT '抄送人(多个逗号分隔)',
  `email_content` text COMMENT '邮件内容',
  `attach_type` varchar(20) DEFAULT 'html' COMMENT '附件类型 pdf/excel/html',
  `status` tinyint DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `last_execute_time` datetime DEFAULT NULL COMMENT '最后执行时间',
  `next_execute_time` datetime DEFAULT NULL COMMENT '下次执行时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_report_id` (`report_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时任务表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入示例报表数据
INSERT INTO `sys_report` (`app_id`, `report_name`, `report_code`, `report_type`, `description`, `layout_type`, `status`, `version`, `auto_refresh`, `refresh_interval`, `created_by`, `created_time`) VALUES
(1, '销售数据报表', 'sales_report', 'comprehensive', '销售数据综合报表', 'flow', 1, '1.0.0', 1, 60, 1, NOW()),
(1, '运营分析报表', 'operation_report', 'analysis', '运营数据分析报表', 'flow', 1, '1.0.0', 0, 300, 1, NOW());

-- 插入示例大屏数据
INSERT INTO `sys_dashboard` (`app_id`, `dashboard_name`, `dashboard_code`, `description`, `layout_type`, `width`, `height`, `background_color`, `theme`, `status`, `version`, `auto_refresh`, `refresh_interval`, `created_by`, `created_time`) VALUES
(1, '运营数据大屏', 'operation_dashboard', '运营核心指标综合展示', 'free', 1920, 1080, '#0a0e27', 'tech-blue', 1, '1.0.0', 1, 30, 1, NOW()),
(1, '销售监控大屏', 'sales_dashboard', '销售数据实时监控', 'free', 1920, 1080, '#0a0e27', 'deep-black', 1, '1.0.0', 1, 15, 1, NOW());
