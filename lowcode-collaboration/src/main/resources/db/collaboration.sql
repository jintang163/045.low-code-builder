-- =====================================================
-- 团队协作与评论模块数据库表结构
-- =====================================================

-- -----------------------------------------------------
-- 评论表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_comment`;
CREATE TABLE `sys_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT NOT NULL COMMENT '应用ID',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: DATA_MODEL-数据模型, PAGE-页面, PAGE_COMPONENT-页面组件, LOGIC_NODE-逻辑节点, WORKFLOW-工作流',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `target_name` VARCHAR(255) DEFAULT NULL COMMENT '目标名称（冗余）',
    `parent_id` BIGINT DEFAULT NULL COMMENT '父评论ID（用于回复）',
    `reply_to_user_id` BIGINT DEFAULT NULL COMMENT '回复目标用户ID',
    `reply_to_user_name` VARCHAR(64) DEFAULT NULL COMMENT '回复目标用户名称',
    `content` TEXT NOT NULL COMMENT '评论内容',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 1-未解决, 2-已解决',
    `like_count` INT DEFAULT 0 COMMENT '点赞数',
    `resolved_by` BIGINT DEFAULT NULL COMMENT '解决人ID',
    `resolved_time` DATETIME DEFAULT NULL COMMENT '解决时间',
    `comment_tag` VARCHAR(64) DEFAULT NULL COMMENT '评论标签: TODO, QUESTION, IDEA, BUG, SUGGESTION',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_app_target` (`app_id`, `target_type`, `target_id`),
    KEY `idx_parent_id` (`parent_id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- -----------------------------------------------------
-- 评论附件表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_comment_attachment`;
CREATE TABLE `sys_comment_attachment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
    `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
    `file_type` VARCHAR(64) DEFAULT NULL COMMENT '文件类型: image/png, image/jpeg, application/pdf等',
    `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    `width` INT DEFAULT NULL COMMENT '图片宽度（像素）',
    `height` INT DEFAULT NULL COMMENT '图片高度（像素）',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_comment_id` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论附件表';

-- -----------------------------------------------------
-- 评论@提及记录表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_comment_mention`;
CREATE TABLE `sys_comment_mention` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `comment_id` BIGINT NOT NULL COMMENT '评论ID',
    `user_id` BIGINT NOT NULL COMMENT '被@的用户ID',
    `username` VARCHAR(64) DEFAULT NULL COMMENT '用户名（冗余）',
    `nickname` VARCHAR(64) DEFAULT NULL COMMENT '用户昵称（冗余）',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
    `read_time` DATETIME DEFAULT NULL COMMENT '阅读时间',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_comment_id` (`comment_id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论@提及记录表';

-- -----------------------------------------------------
-- 设计历史记录表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_design_history`;
CREATE TABLE `sys_design_history` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT NOT NULL COMMENT '应用ID',
    `target_type` VARCHAR(32) NOT NULL COMMENT '目标类型: DATA_MODEL-数据模型, PAGE-页面, PAGE_COMPONENT-页面组件, LOGIC_NODE-逻辑节点, WORKFLOW-工作流',
    `target_id` BIGINT NOT NULL COMMENT '目标ID',
    `target_name` VARCHAR(255) DEFAULT NULL COMMENT '目标名称（冗余）',
    `operation_type` VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE-创建, UPDATE-更新, DELETE-删除, MOVE-移动, RESIZE-调整大小, PUBLISH-发布, ROLLBACK-回滚',
    `operation_desc` VARCHAR(512) DEFAULT NULL COMMENT '操作描述',
    `before_snapshot` LONGTEXT DEFAULT NULL COMMENT '操作前快照（JSON）',
    `after_snapshot` LONGTEXT DEFAULT NULL COMMENT '操作后快照（JSON）',
    `diff_json` LONGTEXT DEFAULT NULL COMMENT '差异对比（JSON）',
    `ip_address` VARCHAR(64) DEFAULT NULL COMMENT '操作IP地址',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_app_target` (`app_id`, `target_type`, `target_id`),
    KEY `idx_created_by` (`created_by`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_created_time` (`created_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设计历史记录表';

-- -----------------------------------------------------
-- 任务分配表
-- -----------------------------------------------------
DROP TABLE IF EXISTS `sys_task_assignment`;
CREATE TABLE `sys_task_assignment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `app_id` BIGINT NOT NULL COMMENT '应用ID',
    `task_title` VARCHAR(255) NOT NULL COMMENT '任务标题',
    `task_desc` TEXT DEFAULT NULL COMMENT '任务描述',
    `task_priority` VARCHAR(16) DEFAULT 'MEDIUM' COMMENT '优先级: URGENT-紧急, HIGH-高, MEDIUM-中, LOW-低',
    `task_status` VARCHAR(16) DEFAULT 'TODO' COMMENT '状态: TODO-待办, IN_PROGRESS-进行中, DONE-已完成, CANCELLED-已取消',
    `target_type` VARCHAR(32) DEFAULT NULL COMMENT '关联目标类型',
    `target_id` BIGINT DEFAULT NULL COMMENT '关联目标ID',
    `target_name` VARCHAR(255) DEFAULT NULL COMMENT '关联目标名称',
    `assignee_id` BIGINT NOT NULL COMMENT '指派人ID',
    `assignee_name` VARCHAR(64) DEFAULT NULL COMMENT '指派人名称（冗余）',
    `comment_id` BIGINT DEFAULT NULL COMMENT '关联评论ID',
    `due_date` DATETIME DEFAULT NULL COMMENT '截止日期',
    `completed_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `completed_note` TEXT DEFAULT NULL COMMENT '完成说明',
    `created_by` BIGINT DEFAULT NULL COMMENT '创建人',
    `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_by` BIGINT DEFAULT NULL COMMENT '更新人',
    `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '删除标记: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_app_target` (`app_id`, `target_type`, `target_id`),
    KEY `idx_assignee_status` (`assignee_id`, `task_status`),
    KEY `idx_due_date` (`due_date`),
    KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务分配表';
