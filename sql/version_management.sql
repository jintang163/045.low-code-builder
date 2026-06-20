-- =============================================
-- 版本管理与发布流水线 - 数据库初始化脚本
-- =============================================

-- 版本快照表
CREATE TABLE IF NOT EXISTS `sys_version_snapshot` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型: PAGE, DATA_MODEL, BUSINESS_LOGIC',
  `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
  `resource_name` varchar(200) NOT NULL COMMENT '资源名称',
  `version` varchar(50) NOT NULL COMMENT '版本号 v1.0.0',
  `snapshot_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '快照类型: 1=自动快照, 2=手动快照',
  `snapshot_data` longtext COMMENT '完整快照数据(JSON)',
  `page_snapshot` longtext COMMENT '页面快照(JSON)',
  `data_model_snapshot` longtext COMMENT '数据模型快照(JSON)',
  `logic_snapshot` longtext COMMENT '业务逻辑快照(JSON)',
  `description` varchar(500) DEFAULT NULL COMMENT '快照描述',
  `git_commit_id` varchar(100) DEFAULT NULL COMMENT 'Git提交ID',
  `git_commit_message` varchar(500) DEFAULT NULL COMMENT 'Git提交信息',
  `git_branch` varchar(100) DEFAULT NULL COMMENT 'Git分支',
  `is_published` tinyint(4) DEFAULT 0 COMMENT '是否已发布: 0=未发布, 1=已发布',
  `published_version` varchar(50) DEFAULT NULL COMMENT '发布版本号',
  `tag` varchar(100) DEFAULT NULL COMMENT '标签',
  `created_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_app_resource` (`app_id`, `resource_type`, `resource_id`),
  KEY `idx_version` (`version`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本快照表';

-- 发布记录表
CREATE TABLE IF NOT EXISTS `sys_release_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型: PAGE, DATA_MODEL, BUSINESS_LOGIC',
  `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
  `resource_name` varchar(200) NOT NULL COMMENT '资源名称',
  `snapshot_id` bigint(20) NOT NULL COMMENT '版本快照ID',
  `version` varchar(50) NOT NULL COMMENT '版本号',
  `release_title` varchar(200) NOT NULL COMMENT '发布标题',
  `release_note` text COMMENT '发布说明',
  `release_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '发布类型: 1=正式发布, 2=回滚发布, 3=灰度发布',
  `release_status` tinyint(4) NOT NULL DEFAULT 0 COMMENT '发布状态: 0=待发布, 1=已发布, 2=发布中, 3=发布失败, 4=已回滚, 5=灰度发布中',
  `gray_type` tinyint(4) DEFAULT NULL COMMENT '灰度类型: 1=百分比, 2=用户组, 3=指定用户',
  `gray_percent` int(11) DEFAULT NULL COMMENT '灰度百分比 0-100',
  `gray_user_group` varchar(200) DEFAULT NULL COMMENT '灰度用户组',
  `gray_user_ids` varchar(1000) DEFAULT NULL COMMENT '灰度用户ID列表(逗号分隔)',
  `target_environment` varchar(50) DEFAULT 'prod' COMMENT '目标环境',
  `git_commit_id` varchar(100) DEFAULT NULL COMMENT 'Git提交ID',
  `git_commit_message` varchar(500) DEFAULT NULL COMMENT 'Git提交信息',
  `git_branch` varchar(100) DEFAULT NULL COMMENT 'Git分支',
  `scheduled_time` datetime DEFAULT NULL COMMENT '计划发布时间',
  `release_time` datetime DEFAULT NULL COMMENT '实际发布时间',
  `rollback_time` datetime DEFAULT NULL COMMENT '回滚时间',
  `rollback_from_snapshot_id` bigint(20) DEFAULT NULL COMMENT '回滚来源快照ID',
  `rollback_reason` varchar(500) DEFAULT NULL COMMENT '回滚原因',
  `is_rollback` tinyint(4) DEFAULT 0 COMMENT '是否回滚: 0=否, 1=是',
  `release_config` text COMMENT '发布配置(JSON)',
  `created_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_app_resource` (`app_id`, `resource_type`, `resource_id`),
  KEY `idx_snapshot` (`snapshot_id`),
  KEY `idx_status` (`release_status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布记录表';

-- 灰度发布配置表
CREATE TABLE IF NOT EXISTS `sys_gray_release_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_id` bigint(20) NOT NULL COMMENT '应用ID',
  `resource_type` varchar(50) NOT NULL COMMENT '资源类型: PAGE, DATA_MODEL, BUSINESS_LOGIC',
  `resource_id` bigint(20) NOT NULL COMMENT '资源ID',
  `release_record_id` bigint(20) DEFAULT NULL COMMENT '发布记录ID',
  `new_snapshot_id` bigint(20) NOT NULL COMMENT '新版本快照ID',
  `old_snapshot_id` bigint(20) DEFAULT NULL COMMENT '旧版本快照ID',
  `new_version` varchar(50) NOT NULL COMMENT '新版本号',
  `old_version` varchar(50) DEFAULT NULL COMMENT '旧版本号',
  `gray_type` tinyint(4) NOT NULL DEFAULT 1 COMMENT '灰度类型: 1=百分比, 2=用户组, 3=指定用户',
  `gray_percent` int(11) DEFAULT 0 COMMENT '灰度百分比 0-100',
  `gray_user_group` varchar(200) DEFAULT NULL COMMENT '灰度用户组',
  `gray_user_ids` varchar(1000) DEFAULT NULL COMMENT '灰度用户ID列表(逗号分隔)',
  `white_list_user_ids` varchar(1000) DEFAULT NULL COMMENT '白名单用户ID列表(逗号分隔)',
  `black_list_user_ids` varchar(1000) DEFAULT NULL COMMENT '黑名单用户ID列表(逗号分隔)',
  `hash_field` varchar(50) DEFAULT 'user_id' COMMENT '哈希字段',
  `status` tinyint(4) NOT NULL DEFAULT 1 COMMENT '状态: 0=未生效, 1=生效中, 2=已停止, 3=已取消',
  `start_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `rule_config` text COMMENT '规则配置(JSON)',
  `created_by` bigint(20) DEFAULT NULL COMMENT '创建人',
  `created_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `updated_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
  PRIMARY KEY (`id`),
  KEY `idx_app_resource` (`app_id`, `resource_type`, `resource_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灰度发布配置表';

-- =============================================
-- 索引优化
-- =============================================

-- 版本快照表索引
CREATE INDEX IF NOT EXISTS `idx_snapshot_is_published` ON `sys_version_snapshot` (`is_published`);
CREATE INDEX IF NOT EXISTS `idx_snapshot_git_commit` ON `sys_version_snapshot` (`git_commit_id`);

-- 发布记录表索引
CREATE INDEX IF NOT EXISTS `idx_release_type` ON `sys_release_record` (`release_type`);
CREATE INDEX IF NOT EXISTS `idx_release_rollback` ON `sys_release_record` (`is_rollback`);

-- 灰度发布配置表索引
CREATE INDEX IF NOT EXISTS `idx_gray_release` ON `sys_gray_release_config` (`new_snapshot_id`, `old_snapshot_id`);

-- =============================================
-- 示例数据
-- =============================================

-- 插入一个示例版本快照
-- INSERT INTO `sys_version_snapshot` (`app_id`, `resource_type`, `resource_id`, `resource_name`, `version`, `snapshot_type`, `description`)
-- VALUES (1, 'PAGE', 1, '用户管理页面', 'v1.0.0', 2, '初始版本');
