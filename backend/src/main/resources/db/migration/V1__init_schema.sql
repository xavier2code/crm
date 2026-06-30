CREATE TABLE t_role (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    is_builtin SMALLINT NOT NULL DEFAULT 0,
    data_scope_type SMALLINT DEFAULT 1
);

CREATE TABLE t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(128),
    status SMALLINT NOT NULL DEFAULT 1,
    is_initial_password SMALLINT NOT NULL DEFAULT 1,
    last_login_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE TABLE t_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    business_domain VARCHAR(64),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES t_role(id)
);

CREATE TABLE t_data_permission (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scope_type SMALLINT NOT NULL,
    scope_value VARCHAR(64) NOT NULL,
    CONSTRAINT fk_data_permission_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);
CREATE INDEX idx_data_permission_user ON t_data_permission(user_id);

CREATE TABLE t_channel (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    region VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT 1
);

CREATE TABLE t_user_channel (
    user_id BIGINT NOT NULL,
    channel_id BIGINT NOT NULL,
    assign_type SMALLINT NOT NULL,
    assigned_by BIGINT,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, channel_id),
    CONSTRAINT fk_user_channel_user FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_user_channel_channel FOREIGN KEY (channel_id) REFERENCES t_channel(id)
);

CREATE TABLE t_unit (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    region VARCHAR(64),
    admin_level SMALLINT NOT NULL,
    address VARCHAR(255),
    status SMALLINT NOT NULL DEFAULT 1,
    UNIQUE (name, region)
);

CREATE TABLE t_dictionary (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    sort INT DEFAULT 0,
    remark VARCHAR(255),
    UNIQUE (type, code)
);

INSERT INTO t_role (code, name, is_builtin, data_scope_type) VALUES
('ADMIN', '系统管理员', 1, 1),
('REGION_HEAD', '大区总', 1, 3),
('CHANNEL_HEAD', '渠道负责人', 1, 2),
('CHANNEL_BD', '渠道 BD', 1, 5),
('CYBD', 'CYBD', 1, 1),
('ORANGE_EAGLE_SALES', '橙鹰销售', 1, 1),
('ORANGE_EAGLE_HEAD', '橙鹰负责人', 1, 1),
('FINANCE', '财务', 1, 1);

INSERT INTO t_user (username, password_hash, real_name, phone, email, status, is_initial_password, created_by, created_at, updated_at)
VALUES ('admin', '$2b$10$7bzy6Nes/7ZJygsI2hxH0uOOXHrntRkU.9z7Md.kNbDd0sT3IYSma', '系统管理员', '13800138000', 'admin@crm.com', 1, 0, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO t_user_role (user_id, role_id) VALUES (1, 1);

INSERT INTO t_dictionary (type, code, name, sort) VALUES
('police_type', 'CRIMINAL', '刑侦', 1),
('police_type', 'CYBER', '网安', 2),
('police_type', 'TECH', '技侦', 3),
('police_type', 'ECONOMIC', '经侦', 4),
('police_type', 'FOOD_DRUG', '环食药', 5),
('police_type', 'TOBACCO', '烟草', 6),
('police_type', 'CUSTOMS', '海关', 7),
('business_domain', 'SEARCHLIGHT', '探照灯', 1),
('business_domain', 'PANORAMA', '全景', 2),
('business_domain', '360', '360', 3),
('business_domain', 'OTHER', '其他', 4),
('project_type', 'NEW', '新签', 1),
('project_type', 'RENEW', '续签', 2),
('project_type', 'TRIAL', '试用', 3),
('opportunity_status', 'DRAFT', '草稿', 1),
('opportunity_status', 'PENDING', '审批中', 2),
('opportunity_status', 'ACTIVE', '生效中', 3),
('opportunity_status', 'FAILED', '报备失败', 4),
('opportunity_status', 'EXPIRED', '报备失效', 5),
('project_status', 'IN_PROGRESS', '项目中', 1),
('project_status', 'COMPLETED', '项目完成', 2),
('project_status', 'INTERRUPTED', '项目中断', 3),
('project_status', 'TERMINATED', '项目终止', 4),
('stage_6', 'VALUE_VERIFY', '价值验证', 1),
('stage_6', 'LIXIANG', '立项', 2),
('stage_6', 'ZHAOBIAO', '招投标', 3),
('stage_6', 'HETONG', '合同', 4),
('stage_6', 'FUWU', '服务', 5),
('stage_6', 'XUQIAN', '续签', 6);
