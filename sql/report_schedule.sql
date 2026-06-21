USE lowcode_platform;

CREATE TABLE IF NOT EXISTS sys_report_schedule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    report_id BIGINT NOT NULL COMMENT '报表ID',
    schedule_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    cron_expression VARCHAR(100) COMMENT 'Cron表达式',
    email_subject VARCHAR(500) COMMENT '邮件主题',
    email_recipients TEXT COMMENT '邮件收件人，多个用逗号分隔',
    email_cc TEXT COMMENT '邮件抄送人，多个用逗号分隔',
    email_content TEXT COMMENT '邮件内容',
    attach_type VARCHAR(20) DEFAULT 'pdf' COMMENT '附件类型 pdf/excel/html',
    status TINYINT DEFAULT 0 COMMENT '状态 0禁用 1启用',
    last_execute_time DATETIME COMMENT '最后执行时间',
    next_execute_time DATETIME COMMENT '下次执行时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_report_id (report_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报表定时任务表';
