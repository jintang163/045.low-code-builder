-- =============================================
-- A/B 测试功能 数据库表结构
-- =============================================

-- A/B测试主表
DROP TABLE IF EXISTS `sys_ab_test`;
CREATE TABLE `sys_ab_test` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `test_name` varchar(100) NOT NULL COMMENT '测试名称',
  `test_code` varchar(100) NOT NULL COMMENT '测试编码',
  `description` varchar(500) DEFAULT NULL COMMENT '测试描述',
  `resource_type` varchar(50) DEFAULT 'PAGE' COMMENT '资源类型 PAGE/COMPONENT',
  `resource_id` bigint DEFAULT NULL COMMENT '资源ID',
  `control_snapshot_id` bigint DEFAULT NULL COMMENT '对照组快照ID',
  `control_version` varchar(50) DEFAULT NULL COMMENT '对照组版本号',
  `status` tinyint DEFAULT 0 COMMENT '状态 0草稿 1运行中 2已暂停 3已结束',
  `traffic_allocation_type` varchar(50) DEFAULT 'PERCENTAGE' COMMENT '流量分配类型 PERCENTAGE/USER_GROUP/CUSTOM',
  `sample_size` int DEFAULT 1000 COMMENT '样本量预估',
  `confidence_level` decimal(5,4) DEFAULT 0.9500 COMMENT '置信度',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `winner_variant_id` bigint DEFAULT NULL COMMENT '优胜版本ID',
  `conclusion` text COMMENT '实验结论',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_test_code` (`app_id`, `test_code`, `deleted`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_status` (`status`),
  KEY `idx_resource` (`resource_type`, `resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B测试表';

-- A/B测试变体表
DROP TABLE IF EXISTS `sys_ab_test_variant`;
CREATE TABLE `sys_ab_test_variant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `test_id` bigint NOT NULL COMMENT '测试ID',
  `variant_name` varchar(100) NOT NULL COMMENT '变体名称',
  `variant_type` varchar(20) DEFAULT 'EXPERIMENT' COMMENT '变体类型 CONTROL/EXPERIMENT',
  `snapshot_id` bigint DEFAULT NULL COMMENT '版本快照ID',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号',
  `traffic_weight` int DEFAULT 50 COMMENT '流量权重(百分比)',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `page_views` bigint DEFAULT 0 COMMENT '页面浏览量',
  `unique_visitors` bigint DEFAULT 0 COMMENT '独立访客数',
  `conversions` bigint DEFAULT 0 COMMENT '转化数',
  `conversion_rate` decimal(10,4) DEFAULT 0.0000 COMMENT '转化率',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_test_id` (`test_id`),
  KEY `idx_variant_type` (`variant_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B测试变体表';

-- A/B测试指标表
DROP TABLE IF EXISTS `sys_ab_test_metric`;
CREATE TABLE `sys_ab_test_metric` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `test_id` bigint NOT NULL COMMENT '测试ID',
  `variant_id` bigint NOT NULL COMMENT '变体ID',
  `metric_name` varchar(100) NOT NULL COMMENT '指标名称',
  `metric_type` varchar(50) DEFAULT 'CONVERSION' COMMENT '指标类型 CONVERSION/CLICK/VIEW/DURATION',
  `metric_key` varchar(100) NOT NULL COMMENT '指标键',
  `total_value` decimal(20,4) DEFAULT 0.0000 COMMENT '总值',
  `count` bigint DEFAULT 0 COMMENT '计数',
  `avg_value` decimal(20,4) DEFAULT 0.0000 COMMENT '平均值',
  `unique_count` bigint DEFAULT 0 COMMENT '独立计数',
  `confidence_interval` varchar(100) DEFAULT NULL COMMENT '置信区间',
  `statistical_significance` decimal(10,6) DEFAULT NULL COMMENT '统计显著性(p值)',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint DEFAULT 0 COMMENT '是否删除 0否 1是',
  PRIMARY KEY (`id`),
  KEY `idx_test_id` (`test_id`),
  KEY `idx_variant_id` (`variant_id`),
  KEY `idx_metric_key` (`metric_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B测试指标表';

-- A/B测试事件表
DROP TABLE IF EXISTS `sys_ab_test_event`;
CREATE TABLE `sys_ab_test_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `test_id` bigint NOT NULL COMMENT '测试ID',
  `variant_id` bigint NOT NULL COMMENT '变体ID',
  `event_type` varchar(50) NOT NULL COMMENT '事件类型 VIEW/CLICK/CONVERSION',
  `event_key` varchar(200) DEFAULT NULL COMMENT '事件键',
  `user_id` bigint DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(100) DEFAULT NULL COMMENT '会话ID',
  `page_url` varchar(500) DEFAULT NULL COMMENT '页面URL',
  `component_id` varchar(100) DEFAULT NULL COMMENT '组件ID',
  `event_value` decimal(20,4) DEFAULT NULL COMMENT '事件值',
  `timestamp` bigint DEFAULT NULL COMMENT '时间戳',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  PRIMARY KEY (`id`),
  KEY `idx_test_id` (`test_id`),
  KEY `idx_variant_id` (`variant_id`),
  KEY `idx_event_type` (`event_type`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_timestamp` (`timestamp`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='A/B测试事件表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入示例A/B测试
INSERT INTO `sys_ab_test` (`app_id`, `test_name`, `test_code`, `description`, `resource_type`, `resource_id`, `status`, `traffic_allocation_type`, `sample_size`, `confidence_level`, `created_by`, `created_time`) VALUES
(1, '首页按钮颜色测试', 'home_button_color_test', '测试不同按钮颜色对点击率的影响', 'PAGE', 1, 1, 'PERCENTAGE', 1000, 0.95, 1, NOW()),
(1, '注册流程优化测试', 'register_flow_test', '测试注册流程优化对转化率的影响', 'PAGE', 2, 0, 'PERCENTAGE', 500, 0.95, 1, NOW());

-- 插入示例变体
INSERT INTO `sys_ab_test_variant` (`test_id`, `variant_name`, `variant_type`, `version`, `traffic_weight`, `description`, `page_views`, `unique_visitors`, `conversions`, `conversion_rate`, `created_by`, `created_time`) VALUES
(1, '对照组(蓝色按钮)', 'CONTROL', '1.0.0', 50, '原始蓝色按钮样式', 1250, 1020, 85, 0.0680, 1, NOW()),
(1, '实验组(绿色按钮)', 'EXPERIMENT', '1.1.0', 50, '绿色按钮测试版本', 1280, 1050, 126, 0.0984, 1, NOW()),
(2, '对照组', 'CONTROL', '1.0.0', 50, '原始注册流程', 0, 0, 0, 0.0000, 1, NOW()),
(2, '实验组(简化流程)', 'EXPERIMENT', '1.1.0', 50, '简化注册流程', 0, 0, 0, 0.0000, 1, NOW());

-- 插入示例指标
INSERT INTO `sys_ab_test_metric` (`test_id`, `variant_id`, `metric_name`, `metric_type`, `metric_key`, `total_value`, `count`, `avg_value`, `unique_count`, `statistical_significance`, `created_by`, `created_time`) VALUES
(1, 1, '按钮点击率', 'CLICK', 'submit_button_click', 85, 1250, 0.0680, 85, NULL, 1, NOW()),
(1, 2, '按钮点击率', 'CLICK', 'submit_button_click', 126, 1280, 0.0984, 126, 0.0125, 1, NOW());
