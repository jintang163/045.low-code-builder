USE lowcode_platform;

CREATE TABLE IF NOT EXISTS sys_app_theme (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    theme_name VARCHAR(100) NOT NULL COMMENT '主题名称',
    theme_mode VARCHAR(10) DEFAULT 'light' COMMENT '主题模式 light/dark',
    primary_color VARCHAR(20) DEFAULT '#1677ff' COMMENT '主色',
    success_color VARCHAR(20) DEFAULT '#52c41a' COMMENT '成功色',
    warning_color VARCHAR(20) DEFAULT '#faad14' COMMENT '警告色',
    error_color VARCHAR(20) DEFAULT '#ff4d4f' COMMENT '错误色',
    info_color VARCHAR(20) DEFAULT '#1677ff' COMMENT '信息色',
    border_radius VARCHAR(20) DEFAULT '6px' COMMENT '圆角大小',
    font_family VARCHAR(500) DEFAULT '-apple-system, BlinkMacSystemFont, sans-serif' COMMENT '字体',
    font_size VARCHAR(20) DEFAULT '14px' COMMENT '字号',
    layout_mode VARCHAR(20) DEFAULT 'side' COMMENT '布局模式 side/top/mix',
    sidebar_theme VARCHAR(10) DEFAULT 'dark' COMMENT '侧边栏主题 light/dark',
    header_theme VARCHAR(10) DEFAULT 'light' COMMENT '顶栏主题 light/dark',
    custom_css LONGTEXT COMMENT '自定义CSS',
    theme_config TEXT COMMENT '扩展主题配置JSON',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认 0否 1是',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_is_default (is_default),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用主题配置表';

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '默认主题', 'light', '#1677ff', '#52c41a', '#faad14', '#ff4d4f', '#1677ff', '6px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'side', 'dark', 'light', 1, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND is_default = 1);

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '活力橙', 'light', '#fa8c16', '#52c41a', '#faad14', '#ff4d4f', '#fa8c16', '8px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'side', 'light', 'light', 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND theme_name = '活力橙');

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '自然绿', 'light', '#52c41a', '#52c41a', '#faad14', '#ff4d4f', '#52c41a', '6px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'side', 'dark', 'light', 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND theme_name = '自然绿');

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '优雅紫', 'light', '#722ed1', '#52c41a', '#faad14', '#ff4d4f', '#722ed1', '6px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'side', 'dark', 'light', 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND theme_name = '优雅紫');

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '深邃蓝', 'dark', '#177ddc', '#49aa19', '#d89614', '#d32029', '#177ddc', '6px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'side', 'dark', 'dark', 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND theme_name = '深邃蓝');

INSERT INTO sys_app_theme (app_id, theme_name, theme_mode, primary_color, success_color, warning_color, error_color, info_color, border_radius, font_family, font_size, layout_mode, sidebar_theme, header_theme, is_default, status, created_time, updated_time)
SELECT 0, '暗夜模式', 'dark', '#177ddc', '#49aa19', '#d89614', '#d32029', '#177ddc', '6px', '-apple-system, BlinkMacSystemFont, ''Segoe UI'', Roboto, ''Helvetica Neue'', Arial, sans-serif', '14px', 'top', 'dark', 'dark', 0, 1, NOW(), NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM sys_app_theme WHERE app_id = 0 AND theme_name = '暗夜模式');
