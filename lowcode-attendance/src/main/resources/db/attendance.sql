-- ========================================
-- 员工排班与考勤模块
-- ========================================

-- 排班表
CREATE TABLE IF NOT EXISTS `att_shift_schedule` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '员工ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '员工姓名',
  `shift_type` varchar(20) NOT NULL DEFAULT 'MORNING' COMMENT '班次类型: MORNING-早班, EVENING-晚班, DOUBLE-全天, REST-休息',
  `shift_date` date NOT NULL COMMENT '排班日期',
  `start_time` time DEFAULT NULL COMMENT '上班时间',
  `end_time` time DEFAULT NULL COMMENT '下班时间',
  `work_hours` decimal(4,2) DEFAULT 0.00 COMMENT '工时(小时)',
  `hourly_wage` decimal(10,2) DEFAULT 0.00 COMMENT '时薪',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shift_date` (`shift_date`),
  UNIQUE KEY `uk_user_date` (`user_id`, `shift_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排班表';

-- 打卡记录表
CREATE TABLE IF NOT EXISTS `att_attendance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '员工ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '员工姓名',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `clock_in_time` datetime DEFAULT NULL COMMENT '上班打卡时间',
  `clock_out_time` datetime DEFAULT NULL COMMENT '下班打卡时间',
  `clock_in_latitude` decimal(10,7) DEFAULT NULL COMMENT '上班打卡纬度',
  `clock_in_longitude` decimal(10,7) DEFAULT NULL COMMENT '上班打卡经度',
  `clock_out_latitude` decimal(10,7) DEFAULT NULL COMMENT '下班打卡纬度',
  `clock_out_longitude` decimal(10,7) DEFAULT NULL COMMENT '下班打卡经度',
  `clock_in_location` varchar(200) DEFAULT NULL COMMENT '上班打卡地点',
  `clock_out_location` varchar(200) DEFAULT NULL COMMENT '下班打卡地点',
  `work_hours` decimal(4,2) DEFAULT 0.00 COMMENT '实际工时(小时)',
  `status` varchar(20) NOT NULL DEFAULT 'NORMAL' COMMENT '状态: NORMAL-正常, LATE-迟到, EARLY-早退, ABSENT-缺勤, HALF_DAY-半天',
  `late_minutes` int DEFAULT 0 COMMENT '迟到分钟数',
  `early_minutes` int DEFAULT 0 COMMENT '早退分钟数',
  `shift_type` varchar(20) DEFAULT NULL COMMENT '对应班次',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_attendance_date` (`attendance_date`),
  UNIQUE KEY `uk_user_date` (`user_id`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录表';

-- 请假申请表
CREATE TABLE IF NOT EXISTS `att_leave_request` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '申请人ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '申请人姓名',
  `leave_type` varchar(20) NOT NULL DEFAULT 'PERSONAL' COMMENT '请假类型: SICK-病假, PERSONAL-事假, ANNUAL-年假, MARRIAGE-婚假, MATERNITY-产假, OTHER-其他',
  `start_date` date NOT NULL COMMENT '开始日期',
  `end_date` date NOT NULL COMMENT '结束日期',
  `start_time` time DEFAULT NULL COMMENT '开始时间',
  `end_time` time DEFAULT NULL COMMENT '结束时间',
  `leave_days` decimal(4,1) DEFAULT 0.0 COMMENT '请假天数',
  `leave_hours` decimal(4,1) DEFAULT 0.0 COMMENT '请假小时数',
  `reason` varchar(500) NOT NULL COMMENT '请假原因',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件图片(病假条等)',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approver_name` varchar(100) DEFAULT NULL COMMENT '审批人姓名',
  `approval_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approval_remark` varchar(500) DEFAULT NULL COMMENT '审批备注',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待审批, APPROVED-已批准, REJECTED-已拒绝, CANCELLED-已取消',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';

-- 工资记录表
CREATE TABLE IF NOT EXISTS `att_salary_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `user_id` bigint NOT NULL COMMENT '员工ID',
  `user_name` varchar(100) DEFAULT NULL COMMENT '员工姓名',
  `salary_month` varchar(7) NOT NULL COMMENT '工资月份 yyyy-MM',
  `total_work_days` int DEFAULT 0 COMMENT '实际出勤天数',
  `total_work_hours` decimal(6,2) DEFAULT 0.00 COMMENT '总工时',
  `base_salary` decimal(10,2) DEFAULT 0.00 COMMENT '基本工资',
  `hourly_wage` decimal(10,2) DEFAULT 0.00 COMMENT '时薪',
  `overtime_hours` decimal(5,2) DEFAULT 0.00 COMMENT '加班工时',
  `overtime_pay` decimal(10,2) DEFAULT 0.00 COMMENT '加班费',
  `leave_days` decimal(4,1) DEFAULT 0.0 COMMENT '请假天数',
  `leave_deduction` decimal(10,2) DEFAULT 0.00 COMMENT '请假扣款',
  `late_deduction` decimal(10,2) DEFAULT 0.00 COMMENT '迟到扣款',
  `early_deduction` decimal(10,2) DEFAULT 0.00 COMMENT '早退扣款',
  `absent_deduction` decimal(10,2) DEFAULT 0.00 COMMENT '缺勤扣款',
  `bonus` decimal(10,2) DEFAULT 0.00 COMMENT '奖金',
  `subsidy` decimal(10,2) DEFAULT 0.00 COMMENT '补贴',
  `deduction_total` decimal(10,2) DEFAULT 0.00 COMMENT '扣款合计',
  `net_salary` decimal(10,2) DEFAULT 0.00 COMMENT '实发工资',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `status` varchar(20) DEFAULT 'UNPAID' COMMENT '状态: UNPAID-未发薪, PAID-已发薪',
  `paid_time` datetime DEFAULT NULL COMMENT '发薪时间',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_salary_month` (`salary_month`),
  UNIQUE KEY `uk_user_month` (`user_id`, `salary_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资记录表';

-- 班次配置表
CREATE TABLE IF NOT EXISTS `att_shift_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `shift_code` varchar(50) NOT NULL COMMENT '班次编码',
  `shift_name` varchar(50) NOT NULL COMMENT '班次名称',
  `shift_color` varchar(20) DEFAULT '#1677ff' COMMENT '班次颜色',
  `start_time` time NOT NULL COMMENT '上班时间',
  `end_time` time NOT NULL COMMENT '下班时间',
  `work_hours` decimal(4,2) DEFAULT 0.00 COMMENT '标准工时',
  `hourly_wage` decimal(10,2) DEFAULT 0.00 COMMENT '默认时薪',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` tinyint(1) DEFAULT 1 COMMENT '状态 0禁用 1启用',
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_code` (`app_id`, `shift_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班次配置表';

-- 考勤地点配置表
CREATE TABLE IF NOT EXISTS `att_location_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `app_id` bigint NOT NULL DEFAULT 0 COMMENT '应用ID',
  `location_name` varchar(100) NOT NULL COMMENT '地点名称',
  `latitude` decimal(10,7) NOT NULL COMMENT '纬度',
  `longitude` decimal(10,7) NOT NULL COMMENT '经度',
  `allow_radius` int DEFAULT 500 COMMENT '允许打卡半径(米)',
  `is_default` tinyint(1) DEFAULT 0 COMMENT '是否默认',
  `sort_order` int DEFAULT 0,
  `status` tinyint(1) DEFAULT 1,
  `created_by` bigint DEFAULT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_by` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` tinyint(1) DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤地点配置表';

-- 初始化默认班次
INSERT IGNORE INTO `att_shift_config` (app_id, shift_code, shift_name, shift_color, start_time, end_time, work_hours, hourly_wage, sort_order, status) VALUES
(0, 'MORNING', '早班', '#1677ff', '08:00:00', '16:00:00', 8.00, 20.00, 1, 1),
(0, 'EVENING', '晚班', '#fa8c16', '14:00:00', '22:00:00', 8.00, 20.00, 2, 1),
(0, 'DOUBLE', '全天', '#722ed1', '09:00:00', '18:00:00', 8.00, 20.00, 3, 1),
(0, 'REST', '休息', '#52c41a', '00:00:00', '00:00:00', 0.00, 0.00, 99, 1);
