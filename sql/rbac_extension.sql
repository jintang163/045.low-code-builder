USE lowcode_platform;

ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS app_id BIGINT COMMENT '应用ID，NULL表示全局角色' AFTER id;
ALTER TABLE sys_role ADD COLUMN IF NOT EXISTS role_type VARCHAR(20) DEFAULT 'CUSTOM' COMMENT '角色类型 SYSTEM_ADMIN APP_ADMIN USER GUEST CUSTOM' AFTER role_code;
ALTER TABLE sys_role ADD KEY IF NOT EXISTS idx_app_id (app_id);

ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS app_id BIGINT COMMENT '应用ID，NULL表示全局权限' AFTER id;
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS resource_type VARCHAR(20) DEFAULT 'MENU' COMMENT '资源类型 MENU PAGE BUTTON FIELD DATA' AFTER permission_type;
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS resource_id VARCHAR(100) COMMENT '资源ID（页面ID、组件ID、字段ID等）' AFTER resource_type;
ALTER TABLE sys_permission ADD COLUMN IF NOT EXISTS action VARCHAR(50) COMMENT '操作类型 VIEW EDIT DELETE CREATE' AFTER resource_id;
ALTER TABLE sys_permission ADD KEY IF NOT EXISTS idx_app_id (app_id);
ALTER TABLE sys_permission ADD KEY IF NOT EXISTS idx_resource (app_id, resource_type, resource_id);

CREATE TABLE IF NOT EXISTS sys_app_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(500) COMMENT '角色描述',
    is_default TINYINT DEFAULT 0 COMMENT '是否默认角色 0否 1是',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_role (app_id, role_id),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用角色表';

CREATE TABLE IF NOT EXISTS sys_user_app_role (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_app_role (user_id, app_id, role_id),
    KEY idx_user_id (user_id),
    KEY idx_app_id (app_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户应用角色关联表';

CREATE TABLE IF NOT EXISTS sys_field_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    model_id BIGINT NOT NULL COMMENT '数据模型ID',
    field_id BIGINT NOT NULL COMMENT '字段ID',
    can_view TINYINT DEFAULT 1 COMMENT '是否可见 0否 1是',
    can_edit TINYINT DEFAULT 0 COMMENT '是否可编辑 0否 1是',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_model_field (role_id, model_id, field_id),
    KEY idx_app_id (app_id),
    KEY idx_role_id (role_id),
    KEY idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='字段权限表';

CREATE TABLE IF NOT EXISTS sys_row_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    model_id BIGINT NOT NULL COMMENT '数据模型ID',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    expression TEXT NOT NULL COMMENT '权限表达式，如 {user.deptId} = {data.deptId}',
    condition_type VARCHAR(20) DEFAULT 'AND' COMMENT '多条件组合类型 AND OR',
    priority INT DEFAULT 0 COMMENT '优先级，数字越小优先级越高',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_code (app_id, permission_code),
    KEY idx_app_id (app_id),
    KEY idx_role_id (role_id),
    KEY idx_model_id (model_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行级权限表';

CREATE TABLE IF NOT EXISTS sys_component_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    page_id BIGINT NOT NULL COMMENT '页面ID',
    component_id VARCHAR(100) NOT NULL COMMENT '组件ID',
    can_visible TINYINT DEFAULT 1 COMMENT '是否可见 0否 1是',
    can_disabled TINYINT DEFAULT 0 COMMENT '是否禁用 0否 1是',
    visible_expression TEXT COMMENT '可见性表达式',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_page_component (role_id, page_id, component_id),
    KEY idx_app_id (app_id),
    KEY idx_role_id (role_id),
    KEY idx_page_id (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组件权限表';

CREATE TABLE IF NOT EXISTS sys_page_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    app_id BIGINT NOT NULL COMMENT '应用ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    page_id BIGINT NOT NULL COMMENT '页面ID',
    can_access TINYINT DEFAULT 1 COMMENT '是否可访问 0否 1是',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_page (role_id, page_id),
    KEY idx_app_id (app_id),
    KEY idx_role_id (role_id),
    KEY idx_page_id (page_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面权限表';

ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS dept_id BIGINT COMMENT '部门ID' AFTER user_type;
ALTER TABLE sys_user ADD COLUMN IF NOT EXISTS user_attributes TEXT COMMENT '用户扩展属性JSON' AFTER remark;

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    dept_name VARCHAR(100) NOT NULL COMMENT '部门名称',
    dept_code VARCHAR(50) NOT NULL COMMENT '部门编码',
    dept_type VARCHAR(20) DEFAULT 'DEPT' COMMENT '部门类型 COMPANY DEPT TEAM',
    sort_order INT DEFAULT 0 COMMENT '排序',
    leader VARCHAR(50) COMMENT '负责人',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    created_by BIGINT COMMENT '创建人',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dept_code (dept_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

INSERT INTO sys_dept (parent_id, dept_name, dept_code, dept_type, sort_order, leader, status) VALUES
(0, '总公司', 'HQ', 'COMPANY', 1, '张总', 1),
(1, '技术部', 'TECH', 'DEPT', 1, '李经理', 1),
(1, '市场部', 'MARKET', 'DEPT', 2, '王经理', 1),
(2, '开发组', 'DEV', 'TEAM', 1, '赵组长', 1),
(2, '测试组', 'QA', 'TEAM', 2, '孙组长', 1);

INSERT INTO sys_row_permission (app_id, role_id, model_id, permission_name, permission_code, expression, condition_type, priority, status, remark) VALUES
(1, 1, 1, '查看全部数据', 'view_all_data', '1 = 1', 'AND', 1, 1, '管理员可查看所有数据'),
(1, 2, 1, '查看本部门数据', 'view_dept_data', '{user.deptId} = {data.deptId}', 'AND', 10, 1, '普通用户只能查看本部门数据'),
(1, 2, 1, '查看自己创建的数据', 'view_own_data', '{user.id} = {data.createdBy}', 'OR', 20, 1, '普通用户可以查看自己创建的数据');

INSERT INTO sys_app_role (app_id, role_id, role_code, role_name, description, is_default) VALUES
(1, 1, 'admin', '应用管理员', '拥有应用的所有权限', 1),
(1, 2, 'user', '普通用户', '拥有基础操作权限', 1),
(1, 3, 'guest', '访客', '只有查看权限', 1);
