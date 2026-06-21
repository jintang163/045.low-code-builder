CREATE DATABASE IF NOT EXISTS lowcode_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos_config DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS flowable DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lowcode_platform;

CREATE TABLE IF NOT EXISTS sys_app (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_name VARCHAR(100) NOT NULL COMMENT '应用名称',
    app_code VARCHAR(50) NOT NULL COMMENT '应用编码',
    app_desc VARCHAR(500) COMMENT '应用描述',
    icon VARCHAR(255) COMMENT '应用图标',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    deploy_config TEXT COMMENT '部署配置',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_code (app_code),
    KEY idx_app_name (app_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用表';

CREATE TABLE IF NOT EXISTS sys_data_source (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    source_name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    source_code VARCHAR(50) NOT NULL COMMENT '数据源编码',
    source_type VARCHAR(20) DEFAULT 'DATABASE' COMMENT '数据源类型 DATABASE REST_API',
    db_type VARCHAR(20) NOT NULL COMMENT '数据库类型 mysql oracle sqlserver postgresql dm rest_api',
    host VARCHAR(100) COMMENT '主机地址',
    port INT COMMENT '端口',
    db_name VARCHAR(100) COMMENT '数据库名',
    username VARCHAR(50) COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码',
    driver_class VARCHAR(255) COMMENT '驱动类',
    connection_params TEXT COMMENT '连接参数',
    initial_size INT DEFAULT 2 COMMENT '初始连接数',
    min_idle INT DEFAULT 2 COMMENT '最小空闲连接数',
    max_active INT DEFAULT 10 COMMENT '最大活跃连接数',
    max_wait INT DEFAULT 60000 COMMENT '最大等待时间(ms)',
    time_between_eviction_runs_millis INT DEFAULT 60000 COMMENT '检测间隔(ms)',
    min_evictable_idle_time_millis INT DEFAULT 600000 COMMENT '最小空闲时间(ms)',
    max_lifetime INT DEFAULT 1800000 COMMENT '连接最大生命周期(ms)',
    connection_timeout INT DEFAULT 30000 COMMENT '连接超时(ms)',
    validation_query VARCHAR(200) COMMENT '验证SQL',
    test_while_idle TINYINT DEFAULT 1 COMMENT '空闲时检测 0否 1是',
    test_on_borrow TINYINT DEFAULT 0 COMMENT '借用时检测 0否 1是',
    test_on_return TINYINT DEFAULT 0 COMMENT '归还时检测 0否 1是',
    rest_api_url VARCHAR(500) COMMENT 'REST API地址',
    rest_api_method VARCHAR(10) DEFAULT 'GET' COMMENT 'REST API方法 GET POST PUT DELETE',
    rest_api_headers TEXT COMMENT 'REST API请求头JSON',
    rest_api_body TEXT COMMENT 'REST API请求体',
    rest_api_auth_type VARCHAR(20) COMMENT 'REST API认证类型 NONE BASIC BEARER API_KEY',
    rest_api_auth_token VARCHAR(500) COMMENT 'REST API认证Token(加密)',
    connect_timeout INT DEFAULT 5000 COMMENT 'HTTP连接超时(ms)',
    read_timeout INT DEFAULT 10000 COMMENT 'HTTP读取超时(ms)',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    last_health_check_time DATETIME COMMENT '最后健康检查时间',
    health_check_status VARCHAR(20) COMMENT '健康检查状态 HEALTHY UNHEALTHY',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_source_code (source_code),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

CREATE TABLE IF NOT EXISTS sys_data_model (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    entity_name VARCHAR(100) NOT NULL COMMENT '实体类名',
    model_desc VARCHAR(500) COMMENT '模型描述',
    primary_key_strategy VARCHAR(50) DEFAULT 'AUTO' COMMENT '主键策略 AUTO INPUT UUID',
    logic_delete_field VARCHAR(50) COMMENT '逻辑删除字段',
    version_field VARCHAR(50) COMMENT '乐观锁字段',
    tenant_field VARCHAR(50) COMMENT '租户字段',
    table_charset VARCHAR(20) DEFAULT 'utf8mb4' COMMENT '表字符集',
    table_engine VARCHAR(20) DEFAULT 'InnoDB' COMMENT '表引擎',
    position_x INT DEFAULT 0 COMMENT 'ER图X坐标',
    position_y INT DEFAULT 0 COMMENT 'ER图Y坐标',
    status TINYINT DEFAULT 1 COMMENT '状态 0设计中 1已发布',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_table_name (data_source_id, table_name),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据模型表';

CREATE TABLE IF NOT EXISTS sys_model_field (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    field_name VARCHAR(50) NOT NULL COMMENT '字段名',
    column_name VARCHAR(50) NOT NULL COMMENT '列名',
    field_type VARCHAR(50) NOT NULL COMMENT '字段类型 STRING NUMBER DATE ENUM REFERENCE',
    java_type VARCHAR(100) NOT NULL COMMENT 'Java类型',
    jdbc_type VARCHAR(50) NOT NULL COMMENT 'JDBC类型',
    length INT DEFAULT 255 COMMENT '长度',
    precision INT DEFAULT 0 COMMENT '精度',
    scale INT DEFAULT 0 COMMENT '小数位数',
    default_value VARCHAR(255) COMMENT '默认值',
    field_comment VARCHAR(255) COMMENT '字段注释',
    is_primary TINYINT DEFAULT 0 COMMENT '是否主键 0否 1是',
    is_required TINYINT DEFAULT 0 COMMENT '是否必填 0否 1是',
    is_unique TINYINT DEFAULT 0 COMMENT '是否唯一 0否 1是',
    is_index TINYINT DEFAULT 0 COMMENT '是否索引 0否 1是',
    is_auto_increment TINYINT DEFAULT 0 COMMENT '是否自增 0否 1是',
    is_logic_delete TINYINT DEFAULT 0 COMMENT '是否逻辑删除 0否 1是',
    is_version TINYINT DEFAULT 0 COMMENT '是否乐观锁 0否 1是',
    is_tenant TINYINT DEFAULT 0 COMMENT '是否租户字段 0否 1是',
    enum_values TEXT COMMENT '枚举值JSON',
    reference_model_id BIGINT COMMENT '关联模型ID',
    reference_field_id BIGINT COMMENT '关联字段ID',
    reference_type VARCHAR(20) COMMENT '关联类型 ONE_TO_ONE ONE_TO_MANY MANY_TO_MANY',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_model_id (model_id),
    KEY idx_column_name (model_id, column_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型字段表';

CREATE TABLE IF NOT EXISTS sys_model_index (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    index_name VARCHAR(100) NOT NULL COMMENT '索引名称',
    index_type VARCHAR(20) DEFAULT 'NORMAL' COMMENT '索引类型 NORMAL UNIQUE FULLTEXT',
    index_fields TEXT NOT NULL COMMENT '索引字段JSON',
    index_comment VARCHAR(255) COMMENT '索引注释',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_index_name (model_id, index_name),
    KEY idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型索引表';

CREATE TABLE IF NOT EXISTS sys_model_relation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    source_model_id BIGINT NOT NULL COMMENT '源模型ID',
    target_model_id BIGINT NOT NULL COMMENT '目标模型ID',
    relation_type VARCHAR(20) NOT NULL COMMENT '关联类型 ONE_TO_ONE ONE_TO_MANY MANY_TO_MANY',
    source_field_id BIGINT NOT NULL COMMENT '源字段ID',
    target_field_id BIGINT NOT NULL COMMENT '目标字段ID',
    foreign_key_name VARCHAR(100) COMMENT '外键名称',
    on_delete VARCHAR(20) DEFAULT 'RESTRICT' COMMENT '删除策略 CASCADE SET_NULL RESTRICT',
    on_update VARCHAR(20) DEFAULT 'RESTRICT' COMMENT '更新策略 CASCADE SET_NULL RESTRICT',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_source_model (source_model_id),
    KEY idx_target_model (target_model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型关联表';

CREATE TABLE IF NOT EXISTS sys_sql_migration (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    version VARCHAR(20) NOT NULL COMMENT '版本号',
    migration_name VARCHAR(100) NOT NULL COMMENT '迁移名称',
    sql_content TEXT NOT NULL COMMENT 'SQL内容',
    model_changes TEXT COMMENT '模型变更JSON',
    status TINYINT DEFAULT 0 COMMENT '状态 0待执行 1已执行 2执行失败',
    execute_time DATETIME COMMENT '执行时间',
    execute_result TEXT COMMENT '执行结果',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_data_source (data_source_id),
    KEY idx_version (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SQL迁移表';

CREATE TABLE IF NOT EXISTS sys_page (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    page_name VARCHAR(100) NOT NULL COMMENT '页面名称',
    page_code VARCHAR(50) NOT NULL COMMENT '页面编码',
    page_type VARCHAR(20) DEFAULT 'PC' COMMENT '页面类型 PC MOBILE BOTH',
    page_path VARCHAR(255) COMMENT '页面路由路径',
    layout_type VARCHAR(50) DEFAULT 'FLEX' COMMENT '布局类型 FLEX GRID ABSOLUTE',
    page_config TEXT COMMENT '页面配置JSON',
    page_schema TEXT COMMENT '页面Schema',
    is_home TINYINT DEFAULT 0 COMMENT '是否首页 0否 1是',
    status TINYINT DEFAULT 0 COMMENT '状态 0设计中 1已发布',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_page_code (app_id, page_code),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面表';

CREATE TABLE IF NOT EXISTS sys_page_component (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    page_id BIGINT NOT NULL COMMENT '页面ID',
    component_id VARCHAR(50) NOT NULL COMMENT '组件唯一ID',
    component_type VARCHAR(50) NOT NULL COMMENT '组件类型',
    component_name VARCHAR(100) COMMENT '组件名称',
    parent_id VARCHAR(50) COMMENT '父组件ID',
    slot_name VARCHAR(50) COMMENT '插槽名称',
    props_config TEXT COMMENT '属性配置JSON',
    style_config TEXT COMMENT '样式配置JSON',
    event_config TEXT COMMENT '事件配置JSON',
    data_source_config TEXT COMMENT '数据源配置JSON',
    validation_config TEXT COMMENT '校验配置JSON',
    position_x INT COMMENT 'X坐标',
    position_y INT COMMENT 'Y坐标',
    width INT COMMENT '宽度',
    height INT COMMENT '高度',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_component_id (page_id, component_id),
    KEY idx_page_id (page_id),
    KEY idx_parent_id (page_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面组件表';

CREATE TABLE IF NOT EXISTS sys_component_library (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    component_type VARCHAR(50) NOT NULL COMMENT '组件类型',
    component_name VARCHAR(100) NOT NULL COMMENT '组件名称',
    component_category VARCHAR(50) NOT NULL COMMENT '组件分类 基础 表单 布局 图表 业务',
    icon VARCHAR(255) COMMENT '组件图标',
    description VARCHAR(500) COMMENT '组件描述',
    default_props TEXT COMMENT '默认属性JSON',
    default_style TEXT COMMENT '默认样式JSON',
    prop_schema TEXT COMMENT '属性Schema',
    is_custom TINYINT DEFAULT 0 COMMENT '是否自定义组件 0否 1是',
    component_code TEXT COMMENT '自定义组件代码',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_component_type (component_type),
    KEY idx_category (component_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件库表';

CREATE TABLE IF NOT EXISTS sys_business_logic (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    logic_name VARCHAR(100) NOT NULL COMMENT '逻辑名称',
    logic_code VARCHAR(50) NOT NULL COMMENT '逻辑编码',
    logic_type VARCHAR(20) DEFAULT 'FLOW' COMMENT '逻辑类型 FLOW API',
    trigger_type VARCHAR(50) COMMENT '触发器类型 TIMER API TABLE_EVENT MANUAL',
    trigger_config TEXT COMMENT '触发器配置JSON',
    flow_graph TEXT COMMENT '流程图JSON',
    flow_nodes TEXT COMMENT '流程节点JSON',
    flow_edges TEXT COMMENT '流程连线JSON',
    variables TEXT COMMENT '变量定义JSON',
    generated_code TEXT COMMENT '生成的代码',
    status TINYINT DEFAULT 0 COMMENT '状态 0设计中 1已发布 2已部署',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    deploy_time DATETIME COMMENT '部署时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_logic_code (app_id, logic_code),
    KEY idx_app_id (app_id),
    KEY idx_trigger_type (trigger_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务逻辑表';

CREATE TABLE IF NOT EXISTS sys_logic_debug (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    logic_id BIGINT NOT NULL COMMENT '逻辑ID',
    debug_session_id VARCHAR(50) NOT NULL COMMENT '调试会话ID',
    current_node_id VARCHAR(50) COMMENT '当前节点ID',
    variables_snapshot TEXT COMMENT '变量快照JSON',
    execution_log TEXT COMMENT '执行日志',
    status VARCHAR(20) DEFAULT 'RUNNING' COMMENT '状态 RUNNING PAUSED FINISHED ERROR',
    breakpoints TEXT COMMENT '断点JSON',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_logic_id (logic_id),
    KEY idx_session_id (debug_session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑调试表';

CREATE TABLE IF NOT EXISTS sys_workflow_definition (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    process_key VARCHAR(50) NOT NULL COMMENT '流程定义Key',
    process_name VARCHAR(100) NOT NULL COMMENT '流程名称',
    process_desc VARCHAR(500) COMMENT '流程描述',
    bpmn_xml TEXT COMMENT 'BPMN XML内容',
    flow_graph TEXT COMMENT '流程图JSON',
    flowable_deployment_id VARCHAR(64) COMMENT 'Flowable部署ID',
    flowable_process_def_id VARCHAR(64) COMMENT 'Flowable流程定义ID',
    status TINYINT DEFAULT 0 COMMENT '状态 0设计中 1已部署 2已挂起',
    version INT DEFAULT 1 COMMENT '版本号',
    deploy_time DATETIME COMMENT '部署时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_process_key (app_id, process_key, version),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流定义表';

CREATE TABLE IF NOT EXISTS sys_generated_code (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    model_id BIGINT COMMENT '模型ID',
    logic_id BIGINT COMMENT '逻辑ID',
    code_type VARCHAR(50) NOT NULL COMMENT '代码类型 ENTITY MAPPER SERVICE CONTROLLER API JS',
    file_path VARCHAR(255) NOT NULL COMMENT '文件路径',
    file_name VARCHAR(100) NOT NULL COMMENT '文件名',
    code_content TEXT COMMENT '代码内容',
    language VARCHAR(20) DEFAULT 'Java' COMMENT '语言',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    generated_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_model_id (model_id),
    KEY idx_logic_id (logic_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生成代码表';

CREATE TABLE IF NOT EXISTS sys_app_deploy (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    deploy_version VARCHAR(20) NOT NULL COMMENT '部署版本',
    deploy_type VARCHAR(20) DEFAULT 'DOCKER' COMMENT '部署类型 DOCKER K8S',
    deploy_config TEXT COMMENT '部署配置JSON',
    deploy_status VARCHAR(20) DEFAULT 'PENDING' COMMENT '部署状态 PENDING RUNNING SUCCESS FAILED',
    deploy_log TEXT COMMENT '部署日志',
    access_url VARCHAR(255) COMMENT '访问地址',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_app_id (app_id),
    KEY idx_deploy_version (app_id, deploy_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用部署表';

CREATE TABLE IF NOT EXISTS sys_app_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    template_code VARCHAR(50) NOT NULL COMMENT '模板编码',
    template_desc VARCHAR(500) COMMENT '模板描述',
    icon VARCHAR(255) COMMENT '模板图标',
    category VARCHAR(50) COMMENT '分类 oa/crm/inventory/business/system/other',
    tags VARCHAR(255) COMMENT '标签，逗号分隔',
    version VARCHAR(20) DEFAULT '1.0.0' COMMENT '版本号',
    install_count INT DEFAULT 0 COMMENT '安装次数',
    star_count INT DEFAULT 0 COMMENT '收藏/点赞数',
    screenshot VARCHAR(500) COMMENT '截图URL，逗号分隔',
    template_data LONGTEXT COMMENT '模板数据JSON（包含模型/页面/逻辑等完整快照）',
    template_type TINYINT DEFAULT 1 COMMENT '模板类型 0官方 1用户 2团队',
    publisher VARCHAR(50) COMMENT '发布者名称',
    publisher_id BIGINT COMMENT '发布者ID',
    status TINYINT DEFAULT 0 COMMENT '状态 0草稿 1已发布 2已下架',
    publish_time DATETIME COMMENT '发布时间',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_code (template_code),
    KEY idx_category (category),
    KEY idx_template_type (template_type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用模板表';

INSERT INTO sys_component_library (component_type, component_name, component_category, icon, description, default_props, default_style, prop_schema) VALUES
('Input', '输入框', '表单', 'FormOutlined', '文本输入框', '{"placeholder":"请输入","maxLength":100}', '{"width":"100%"}', '{}'),
('TextArea', '多行文本', '表单', 'FormOutlined', '多行文本输入框', '{"placeholder":"请输入","rows":4}', '{"width":"100%"}', '{}'),
('InputNumber', '数字输入', '表单', 'FormOutlined', '数字输入框', '{"min":0,"precision":2}', '{"width":"100%"}', '{}'),
('Select', '下拉选择', '表单', 'FormOutlined', '下拉选择器', '{"options":[],"placeholder":"请选择"}', '{"width":"100%"}', '{}'),
('DatePicker', '日期选择', '表单', 'CalendarOutlined', '日期选择器', '{"placeholder":"请选择日期"}', '{"width":"100%"}', '{}'),
('TimePicker', '时间选择', '表单', 'ClockCircleOutlined', '时间选择器', '{"placeholder":"请选择时间"}', '{"width":"100%"}', '{}'),
('Checkbox', '复选框', '表单', 'CheckSquareOutlined', '复选框组件', '{"defaultChecked":false}', '{}', '{}'),
('Radio', '单选框', '表单', 'CheckCircleOutlined', '单选框组件', '{"options":[]}', '{}', '{}'),
('Switch', '开关', '表单', 'SwitcherOutlined', '开关组件', '{"defaultChecked":false}', '{}', '{}'),
('Upload', '文件上传', '表单', 'UploadOutlined', '文件上传组件', '{"maxCount":1,"accept":".jpg,.png,.pdf"}', '{}', '{}'),
('Button', '按钮', '基础', 'PlayCircleOutlined', '按钮组件', '{"text":"按钮","type":"primary"}', '{}', '{}'),
('Table', '表格', '展示', 'TableOutlined', '数据表格', '{"columns":[],"dataSource":[]}', '{"width":"100%"}', '{}'),
('Card', '卡片', '布局', 'IdcardOutlined', '卡片容器', '{"title":"卡片标题"}', '{}', '{}'),
('Tabs', '标签页', '布局', 'TabletOutlined', '标签页容器', '{"items":[]}', '{}', '{}'),
('Form', '表单', '表单', 'FormOutlined', '表单容器', '{"labelCol":6,"wrapperCol":18}', '{}', '{}'),
('Divider', '分割线', '布局', 'ColumnHeightOutlined', '分割线组件', '{"orientation":"center"}', '{}', '{}'),
('LineChart', '折线图', '图表', 'LineChartOutlined', '折线图组件', '{"xField":"name","yField":"value"}', '{"width":"100%","height":300}', '{}'),
('BarChart', '柱状图', '图表', 'BarChartOutlined', '柱状图组件', '{"xField":"name","yField":"value"}', '{"width":"100%","height":300}', '{}'),
('PieChart', '饼图', '图表', 'PieChartOutlined', '饼图组件', '{"angleField":"value","colorField":"name"}', '{"width":"100%","height":300}', '{}'),
('Row', '行布局', '布局', 'LayoutOutlined', '行布局容器', '{"gutter":16}', '{}', '{}'),
('Col', '列布局', '布局', 'LayoutOutlined', '列布局容器', '{"span":12}', '{}', '{}'),
('Rate', '评分', '表单', 'StarOutlined', '星级评分组件', '{"count":5,"allowHalf":false,"allowClear":true}', '{"width":"100%"}', '{"type":"object","properties":{"count":{"type":"number","title":"星星数量","default":5,"x-decorator":"FormItem","x-component":"NumberPicker"},"allowHalf":{"type":"boolean","title":"允许半选","default":false,"x-decorator":"FormItem","x-component":"Switch"},"allowClear":{"type":"boolean","title":"允许清除","default":true,"x-decorator":"FormItem","x-component":"Switch"},"disabled":{"type":"boolean","title":"禁用","default":false,"x-decorator":"FormItem","x-component":"Switch"}}}'),
('Steps', '步骤条', '布局', 'OrderedListOutlined', '分段步骤条组件', '{"current":0,"direction":"horizontal","status":"process"}', '{"width":"100%"}', '{"type":"object","properties":{"current":{"type":"number","title":"当前步骤","default":0,"x-decorator":"FormItem","x-component":"NumberPicker"},"direction":{"type":"string","title":"方向","default":"horizontal","x-decorator":"FormItem","x-component":"Select","enum":[{"label":"水平","value":"horizontal"},{"label":"垂直","value":"vertical"}]},"status":{"type":"string","title":"状态","default":"process","x-decorator":"FormItem","x-component":"Select","enum":[{"label":"进行中","value":"process"},{"label":"等待","value":"wait"},{"label":"完成","value":"finish"},{"label":"错误","value":"error"}]},"items":{"type":"array","title":"步骤项","x-decorator":"FormItem","x-component":"ArrayItems","items":{"type":"object","properties":{"title":{"type":"string","title":"标题","x-decorator":"FormItem","x-component":"Input"},"description":{"type":"string","title":"描述","x-decorator":"FormItem","x-component":"Input"}}}}}}'),
('Signature', '签名板', '表单', 'EditOutlined', '手写签名板组件', '{"width":400,"height":200,"penColor":"#000000","backgroundColor":"#ffffff"}', '{"width":"100%"}', '{"type":"object","properties":{"width":{"type":"number","title":"宽度","default":400,"x-decorator":"FormItem","x-component":"NumberPicker"},"height":{"type":"number","title":"高度","default":200,"x-decorator":"FormItem","x-component":"NumberPicker"},"penColor":{"type":"string","title":"画笔颜色","default":"#000000","x-decorator":"FormItem","x-component":"ColorPicker"},"backgroundColor":{"type":"string","title":"背景颜色","default":"#ffffff","x-decorator":"FormItem","x-component":"ColorPicker"},"penWidth":{"type":"number","title":"画笔粗细","default":2,"x-decorator":"FormItem","x-component":"NumberPicker"}}}'),
('LocationPicker', '地理位置', '表单', 'EnvironmentOutlined', '地理位置选择组件', '{"placeholder":"请选择位置","showCoordinate":true}', '{"width":"100%"}', '{"type":"object","properties":{"placeholder":{"type":"string","title":"占位文本","default":"请选择位置","x-decorator":"FormItem","x-component":"Input"},"showCoordinate":{"type":"boolean","title":"显示经纬度","default":true,"x-decorator":"FormItem","x-component":"Switch"},"mapType":{"type":"string","title":"地图类型","default":"amap","x-decorator":"FormItem","x-component":"Select","enum":[{"label":"高德地图","value":"amap"},{"label":"百度地图","value":"baidu"}]}}}'),
('SubForm', '子表单', '表单', 'FormOutlined', '一对多嵌套子表单', '{"title":"子表单","showAddButton":true,"showDeleteButton":true,"minRows":0,"maxRows":10}', '{"width":"100%"}', '{"type":"object","properties":{"title":{"type":"string","title":"标题","default":"子表单","x-decorator":"FormItem","x-component":"Input"},"showAddButton":{"type":"boolean","title":"显示添加按钮","default":true,"x-decorator":"FormItem","x-component":"Switch"},"showDeleteButton":{"type":"boolean","title":"显示删除按钮","default":true,"x-decorator":"FormItem","x-component":"Switch"},"minRows":{"type":"number","title":"最少行数","default":0,"x-decorator":"FormItem","x-component":"NumberPicker"},"maxRows":{"type":"number","title":"最多行数","default":10,"x-decorator":"FormItem","x-component":"NumberPicker"},"relationModelId":{"type":"string","title":"关联模型ID","x-decorator":"FormItem","x-component":"Input"},"foreignKeyField":{"type":"string","title":"外键字段名","x-decorator":"FormItem","x-component":"Input"}}}'),
('FormCopy', '表单复制', '高级', 'CopyOutlined', '表单数据复制功能', '{"sourceFormId":"","copyMode":"full"}', '{}', '{"type":"object","properties":{"sourceFormId":{"type":"string","title":"源表单ID","x-decorator":"FormItem","x-component":"Input"},"copyMode":{"type":"string","title":"复制模式","default":"full","x-decorator":"FormItem","x-component":"Select","enum":[{"label":"全量复制","value":"full"},{"label":"仅字段结构","value":"structure"},{"label":"字段+数据","value":"withData"}]}}}'),
('ExcelImport', 'Excel导入', '高级', 'ImportOutlined', 'Excel数据导入组件', '{"templateId":"","sheetIndex":0,"startRow":1}', '{}', '{"type":"object","properties":{"templateId":{"type":"string","title":"导入模板ID","x-decorator":"FormItem","x-component":"Input"},"sheetIndex":{"type":"number","title":"工作表索引","default":0,"x-decorator":"FormItem","x-component":"NumberPicker"},"startRow":{"type":"number","title":"起始行号","default":1,"x-decorator":"FormItem","x-component":"NumberPicker"},"showTemplateDownload":{"type":"boolean","title":"显示模板下载","default":true,"x-decorator":"FormItem","x-component":"Switch"}}}'),
('ExcelExport', 'Excel导出', '高级', 'ExportOutlined', 'Excel数据导出组件', '{"fileName":"导出数据","sheetName":"Sheet1"}', '{}', '{"type":"object","properties":{"fileName":{"type":"string","title":"文件名","default":"导出数据","x-decorator":"FormItem","x-component":"Input"},"sheetName":{"type":"string","title":"工作表名","default":"Sheet1","x-decorator":"FormItem","x-component":"Input"},"exportMode":{"type":"string","title":"导出模式","default":"current","x-decorator":"FormItem","x-component":"Select","enum":[{"label":"当前页数据","value":"current"},{"label":"全部数据","value":"all"},{"label":"选中数据","value":"selected"}]}}}');

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    salt VARCHAR(50) NOT NULL COMMENT '盐值',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar VARCHAR(255) COMMENT '头像',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    status TINYINT DEFAULT 0 COMMENT '状态 0启用 1禁用',
    user_type TINYINT DEFAULT 0 COMMENT '用户类型 0普通用户 1管理员',
    dept_id BIGINT COMMENT '部门ID',
    remark VARCHAR(500) COMMENT '备注',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(50) COMMENT '最后登录IP',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    permission_name VARCHAR(50) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    permission_type TINYINT DEFAULT 1 COMMENT '权限类型 1目录 2菜单 3按钮',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(50) COMMENT '图标',
    sort INT DEFAULT 0 COMMENT '排序',
    visible TINYINT DEFAULT 0 COMMENT '是否显示 0显示 1隐藏',
    status TINYINT DEFAULT 0 COMMENT '状态 0启用 1禁用',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_role_id (role_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS sys_oss_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    file_name VARCHAR(255) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_suffix VARCHAR(20) COMMENT '文件后缀',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    content_type VARCHAR(100) COMMENT '内容类型',
    storage_type VARCHAR(20) NOT NULL COMMENT '存储类型 minio aliyun tencent',
    bucket_name VARCHAR(100) NOT NULL COMMENT '存储桶名称',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    url VARCHAR(500) COMMENT '访问地址',
    md5 VARCHAR(32) COMMENT '文件MD5',
    upload_user_id BIGINT COMMENT '上传用户ID',
    upload_username VARCHAR(50) COMMENT '上传用户名',
    status TINYINT DEFAULT 0 COMMENT '状态 0正常 1已删除',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_md5 (md5),
    KEY idx_storage_type (storage_type),
    KEY idx_upload_user_id (upload_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OSS文件表';

INSERT INTO sys_user (username, password, salt, nickname, status, user_type, remark) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'lowcode-platform', '超级管理员', 0, 1, '系统默认超级管理员账号，密码：123456'),
('user', 'e10adc3949ba59abbe56e057f20f883e', 'lowcode-platform', '普通用户', 0, 0, '系统默认普通用户账号，密码：123456');

INSERT INTO sys_role (role_name, role_code, role_sort, status, remark) VALUES
('超级管理员', 'admin', 1, 0, '拥有所有权限'),
('管理员', 'manager', 2, 0, '拥有大部分管理权限'),
('普通用户', 'user', 3, 0, '普通用户权限');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1),
(2, 3);

INSERT INTO sys_permission (parent_id, permission_name, permission_code, permission_type, path, component, icon, sort) VALUES
(0, '系统管理', 'system', 1, '/system', '', 'SettingOutlined', 1),
(1, '用户管理', 'system:user', 2, '/system/user', 'system/user/index', 'UserOutlined', 1),
(1, '角色管理', 'system:role', 2, '/system/role', 'system/role/index', 'TeamOutlined', 2),
(1, '权限管理', 'system:permission', 2, '/system/permission', 'system/permission/index', 'SafetyOutlined', 3),
(0, '数据模型', 'model', 1, '/model', '', 'DatabaseOutlined', 2),
(5, '数据源管理', 'model:datasource', 2, '/model/datasource', 'model/datasource/index', 'CloudServerOutlined', 1),
(5, '模型设计', 'model:design', 2, '/model/designer', 'model/designer/index', 'TableOutlined', 2),
(0, '页面设计', 'page', 1, '/page', '', 'LayoutOutlined', 3),
(8, '页面列表', 'page:list', 2, '/page/list', 'page/list/index', 'UnorderedListOutlined', 1),
(8, '页面设计器', 'page:designer', 2, '/page/designer', 'page/designer/index', 'EditOutlined', 2),
(0, '流程编排', 'flow', 1, '/flow', '', 'ApartmentOutlined', 4),
(11, '业务逻辑', 'flow:logic', 2, '/flow/logic', 'flow/logic/index', 'BranchesOutlined', 1),
(11, '工作流设计', 'flow:workflow', 2, '/flow/workflow', 'flow/workflow/index', 'GitBranchOutlined', 2),
(0, '应用管理', 'app', 1, '/app', '', 'AppstoreOutlined', 5),
(14, '应用列表', 'app:list', 2, '/app/list', 'app/list/index', 'AppstoreOutlined', 1),
(14, '应用生成', 'app:generate', 2, '/app/generate', 'app/generate/index', 'CloudDownloadOutlined', 2),
(2, '用户新增', 'system:user:add', 3, '', '', '', 1),
(2, '用户编辑', 'system:user:edit', 3, '', '', '', 2),
(2, '用户删除', 'system:user:delete', 3, '', '', '', 3),
(3, '角色新增', 'system:role:add', 3, '', '', '', 1),
(3, '角色编辑', 'system:role:edit', 3, '', '', '', 2),
(3, '角色删除', 'system:role:delete', 3, '', '', '', 3),
(6, '数据源新增', 'model:datasource:add', 3, '', '', '', 1),
(6, '数据源编辑', 'model:datasource:edit', 3, '', '', '', 2),
(6, '数据源删除', 'model:datasource:delete', 3, '', '', '', 3),
(7, '模型新增', 'model:design:add', 3, '', '', '', 1),
(7, '模型编辑', 'model:design:edit', 3, '', '', '', 2),
(7, '模型删除', 'model:design:delete', 3, '', '', '', 3),
(7, '模型发布', 'model:design:publish', 3, '', '', '', 4),
(9, '页面新增', 'page:list:add', 3, '', '', '', 1),
(9, '页面编辑', 'page:list:edit', 3, '', '', '', 2),
(9, '页面删除', 'page:list:delete', 3, '', '', '', 3),
(9, '页面发布', 'page:list:publish', 3, '', '', '', 4),
(12, '逻辑新增', 'flow:logic:add', 3, '', '', '', 1),
(12, '逻辑编辑', 'flow:logic:edit', 3, '', '', '', 2),
(12, '逻辑删除', 'flow:logic:delete', 3, '', '', '', 3),
(12, '逻辑发布', 'flow:logic:publish', 3, '', '', '', 4),
(13, '流程新增', 'flow:workflow:add', 3, '', '', '', 1),
(13, '流程编辑', 'flow:workflow:edit', 3, '', '', '', 2),
(13, '流程删除', 'flow:workflow:delete', 3, '', '', '', 3),
(13, '流程部署', 'flow:workflow:deploy', 3, '', '', '', 4),
(15, '应用新增', 'app:list:add', 3, '', '', '', 1),
(15, '应用编辑', 'app:list:edit', 3, '', '', '', 2),
(15, '应用删除', 'app:list:delete', 3, '', '', '', 3),
(16, '应用生成', 'app:generate:create', 3, '', '', '', 1),
(16, '应用下载', 'app:generate:download', 3, '', '', '', 2);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8), (1, 9), (1, 10),
(1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16), (1, 17), (1, 18), (1, 19), (1, 20),
(1, 21), (1, 22), (1, 23), (1, 24), (1, 25), (1, 26), (1, 27), (1, 28), (1, 29), (1, 30),
(1, 31), (1, 32), (1, 33), (1, 34), (1, 35), (1, 36), (1, 37), (1, 38), (1, 39), (1, 40),
(1, 41), (1, 42), (1, 43), (1, 44), (1, 45),
(2, 1), (2, 2), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9), (2, 10), (2, 11), (2, 12), (2, 13), (2, 14), (2, 15), (2, 16),
(3, 5), (3, 8), (3, 11), (3, 14);

ALTER TABLE sys_app ADD COLUMN IF NOT EXISTS template_id BIGINT COMMENT '来源模板ID' AFTER version;
ALTER TABLE sys_app ADD COLUMN IF NOT EXISTS template_version VARCHAR(20) COMMENT '模板版本号' AFTER template_id;
ALTER TABLE sys_app ADD KEY IF NOT EXISTS idx_template_id (template_id);

CREATE TABLE IF NOT EXISTS sys_template_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    version VARCHAR(20) NOT NULL COMMENT '版本号',
    change_log VARCHAR(500) COMMENT '更新说明',
    template_data LONGTEXT COMMENT '该版本模板数据快照',
    md5 VARCHAR(32) COMMENT '模板数据MD5签名',
    published_by BIGINT COMMENT '发布人ID',
    publish_time DATETIME COMMENT '发布时间',
    status TINYINT DEFAULT 1 COMMENT '状态 0历史 1当前',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_template_version (template_id, version),
    KEY idx_template_id (template_id),
    KEY idx_publish_time (publish_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板版本历史表';

CREATE TABLE IF NOT EXISTS sys_app_install (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '模板ID',
    template_version VARCHAR(20) NOT NULL COMMENT '安装时模板版本',
    app_id BIGINT NOT NULL COMMENT '安装生成的应用ID',
    user_id BIGINT NOT NULL COMMENT '安装用户ID',
    install_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '安装时间',
    last_update_time DATETIME COMMENT '最后更新时间',
    current_version VARCHAR(20) COMMENT '当前模板版本',
    latest_version VARCHAR(20) COMMENT '最新模板版本',
    has_update TINYINT DEFAULT 0 COMMENT '是否有更新 0否 1是',
    update_diff TEXT COMMENT '更新差异JSON',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_id (app_id),
    KEY idx_template_id (template_id),
    KEY idx_user_id (user_id),
    KEY idx_has_update (has_update)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用安装关联表';

CREATE TABLE IF NOT EXISTS sys_custom_component (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    component_type VARCHAR(50) NOT NULL COMMENT '组件类型（唯一标识）',
    component_name VARCHAR(100) NOT NULL COMMENT '组件名称',
    component_category VARCHAR(50) NOT NULL COMMENT '组件分类 基础 表单 布局 图表 业务 自定义',
    icon VARCHAR(255) COMMENT '组件图标',
    description VARCHAR(500) COMMENT '组件描述',
    author VARCHAR(50) COMMENT '作者',
    current_version VARCHAR(20) DEFAULT '1.0.0' COMMENT '当前版本号',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统组件 0否 1是',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_component_type (component_type),
    KEY idx_category (component_category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义组件表';

CREATE TABLE IF NOT EXISTS sys_custom_component_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    component_id BIGINT NOT NULL COMMENT '组件ID',
    version VARCHAR(20) NOT NULL COMMENT '版本号（如1.0.0）',
    change_log VARCHAR(500) COMMENT '版本更新说明',
    package_path VARCHAR(500) NOT NULL COMMENT 'MinIO包路径',
    package_size BIGINT COMMENT '包大小（字节）',
    main_file VARCHAR(100) COMMENT '主入口文件',
    prop_schema TEXT COMMENT '属性定义JSON Schema',
    event_schema TEXT COMMENT '事件定义JSON Schema',
    exposed_events TEXT COMMENT '暴露的事件JSON数组',
    default_props TEXT COMMENT '默认属性JSON',
    default_style TEXT COMMENT '默认样式JSON',
    is_deprecated TINYINT DEFAULT 0 COMMENT '是否已废弃 0否 1是',
    status TINYINT DEFAULT 1 COMMENT '状态 0历史 1当前',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_component_version (component_id, version),
    KEY idx_component_id (component_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义组件版本表';

CREATE TABLE IF NOT EXISTS sys_virtual_view (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    view_name VARCHAR(100) NOT NULL COMMENT '视图名称',
    view_code VARCHAR(50) NOT NULL COMMENT '视图编码',
    view_sql TEXT COMMENT '视图SQL',
    view_config TEXT COMMENT '视图配置JSON',
    join_config TEXT COMMENT '关联配置JSON',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记 0未删除 1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_view_code (app_id, view_code),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='虚拟视图表';

ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS source_type VARCHAR(20) DEFAULT 'DATABASE' COMMENT '数据源类型 DATABASE REST_API' AFTER source_code;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS initial_size INT DEFAULT 2 COMMENT '初始连接数' AFTER connection_params;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS min_idle INT DEFAULT 2 COMMENT '最小空闲连接数' AFTER initial_size;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS max_active INT DEFAULT 10 COMMENT '最大活跃连接数' AFTER min_idle;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS max_wait INT DEFAULT 60000 COMMENT '最大等待时间(ms)' AFTER max_active;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS time_between_eviction_runs_millis INT DEFAULT 60000 COMMENT '检测间隔(ms)' AFTER max_wait;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS min_evictable_idle_time_millis INT DEFAULT 600000 COMMENT '最小空闲时间(ms)' AFTER time_between_eviction_runs_millis;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS max_lifetime INT DEFAULT 1800000 COMMENT '连接最大生命周期(ms)' AFTER min_evictable_idle_time_millis;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS connection_timeout INT DEFAULT 30000 COMMENT '连接超时(ms)' AFTER max_lifetime;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS validation_query VARCHAR(200) COMMENT '验证SQL' AFTER connection_timeout;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS test_while_idle TINYINT DEFAULT 1 COMMENT '空闲时检测' AFTER validation_query;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS test_on_borrow TINYINT DEFAULT 0 COMMENT '借用时检测' AFTER test_while_idle;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS test_on_return TINYINT DEFAULT 0 COMMENT '归还时检测' AFTER test_on_borrow;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_url VARCHAR(500) COMMENT 'REST API地址' AFTER test_on_return;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_method VARCHAR(10) DEFAULT 'GET' COMMENT 'REST API方法' AFTER rest_api_url;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_headers TEXT COMMENT 'REST API请求头JSON' AFTER rest_api_method;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_body TEXT COMMENT 'REST API请求体' AFTER rest_api_headers;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_auth_type VARCHAR(20) COMMENT 'REST API认证类型' AFTER rest_api_body;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS rest_api_auth_token VARCHAR(500) COMMENT 'REST API认证Token(加密)' AFTER rest_api_auth_type;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS connect_timeout INT DEFAULT 5000 COMMENT 'HTTP连接超时(ms)' AFTER rest_api_auth_token;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS read_timeout INT DEFAULT 10000 COMMENT 'HTTP读取超时(ms)' AFTER connect_timeout;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS last_health_check_time DATETIME COMMENT '最后健康检查时间' AFTER status;
ALTER TABLE sys_data_source ADD COLUMN IF NOT EXISTS health_check_status VARCHAR(20) COMMENT '健康检查状态' AFTER last_health_check_time;

CREATE TABLE IF NOT EXISTS sys_field_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    page_id BIGINT COMMENT '页面ID',
    component_id VARCHAR(100) COMMENT '组件ID',
    source_table VARCHAR(100) COMMENT '源表名',
    source_field VARCHAR(100) COMMENT '源字段名',
    source_type VARCHAR(50) COMMENT '源字段类型',
    target_component VARCHAR(100) COMMENT '目标组件类型',
    target_component_id VARCHAR(100) COMMENT '目标组件ID',
    target_property VARCHAR(100) COMMENT '目标属性名',
    mapping_type VARCHAR(50) DEFAULT 'DIRECT' COMMENT '映射类型 DIRECT FORMAT EXPRESSION',
    sort_order INT DEFAULT 0 COMMENT '排序',
    description VARCHAR(500) COMMENT '描述',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    KEY idx_data_source_id (data_source_id),
    KEY idx_page_id (page_id),
    KEY idx_component_id (component_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段映射表';
