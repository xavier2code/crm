-- ========================================
-- CRM 渠道版 - 核心业务表
-- ========================================

-- 客户表
CREATE TABLE t_customer (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit_id BIGINT NOT NULL,
    police_type VARCHAR(32) NOT NULL,
    customer_layer CHAR(1),
    owner_user_id BIGINT,
    region VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT 1,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_customer_unit FOREIGN KEY (unit_id) REFERENCES t_unit(id),
    CONSTRAINT fk_customer_user FOREIGN KEY (owner_user_id) REFERENCES t_user(id),
    UNIQUE (unit_id, police_type)
);
CREATE INDEX idx_customer_owner ON t_customer(owner_user_id);
CREATE INDEX idx_customer_region ON t_customer(region);

-- 联系人表
CREATE TABLE t_contact (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    title VARCHAR(64),
    phone VARCHAR(20),
    contact_type SMALLINT NOT NULL,
    is_primary SMALLINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contact_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id)
);
CREATE INDEX idx_contact_customer ON t_contact(customer_id);

-- 商机/报备表
CREATE TABLE t_opportunity (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    business_domain VARCHAR(64) NOT NULL,
    project_type SMALLINT NOT NULL,
    amount DECIMAL(12,2),
    status SMALLINT NOT NULL DEFAULT 1,
    submit_count SMALLINT DEFAULT 0,
    last_follow_up_at TIMESTAMP,
    effective_at TIMESTAMP,
    expired_at TIMESTAMP,
    cooling_until TIMESTAMP,
    submitted_by BIGINT NOT NULL,
    approved_by BIGINT,
    approved_at TIMESTAMP,
    reject_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_opportunity_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id),
    CONSTRAINT fk_opportunity_submitted_by FOREIGN KEY (submitted_by) REFERENCES t_user(id),
    CONSTRAINT fk_opportunity_approved_by FOREIGN KEY (approved_by) REFERENCES t_user(id)
);

-- 报备保护：同一客户同业务域只能有一个生效/审批中的报备
-- PostgreSQL 支持 partial index，但为简化数据校验逻辑，改用普通索引 + 应用层校验
CREATE INDEX uk_opportunity_protection ON t_opportunity(customer_id, business_domain, status);
CREATE INDEX idx_opportunity_submitted ON t_opportunity(submitted_by);
CREATE INDEX idx_opportunity_status ON t_opportunity(status);

-- 商机审批日志表
CREATE TABLE t_opportunity_approval_log (
    id BIGSERIAL PRIMARY KEY,
    opportunity_id BIGINT NOT NULL,
    action SMALLINT NOT NULL,
    operator_id BIGINT NOT NULL,
    comment VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_approval_log_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id),
    CONSTRAINT fk_approval_log_operator FOREIGN KEY (operator_id) REFERENCES t_user(id)
);
CREATE INDEX idx_approval_log_opportunity ON t_opportunity_approval_log(opportunity_id);

-- 项目表
CREATE TABLE t_project (
    id BIGSERIAL PRIMARY KEY,
    opportunity_id BIGINT UNIQUE,
    name VARCHAR(255) NOT NULL,
    business_domain VARCHAR(64),
    product_category VARCHAR(64),
    admin_level SMALLINT,
    amount DECIMAL(12,2),
    performance_count INT,
    sales_method VARCHAR(32),
    owner_bd_id BIGINT,
    sales_user_id BIGINT,
    expected_sign_date DATE,
    status SMALLINT NOT NULL DEFAULT 1,
    p_node SMALLINT DEFAULT 1,
    stage_6 VARCHAR(32),
    customer_layer CHAR(1),
    trial_at TIMESTAMP,
    formal_at TIMESTAMP,
    expire_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_project_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id),
    CONSTRAINT fk_project_bd FOREIGN KEY (owner_bd_id) REFERENCES t_user(id),
    CONSTRAINT fk_project_sales FOREIGN KEY (sales_user_id) REFERENCES t_user(id)
);
CREATE INDEX idx_project_bd ON t_project(owner_bd_id);
CREATE INDEX idx_project_status ON t_project(status);

-- 项目双精评分表
CREATE TABLE t_project_score (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    snapshot_week VARCHAR(8) NOT NULL,
    score_dimension VARCHAR(32) NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    weight DECIMAL(5,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_score_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);
CREATE INDEX idx_score_project_week ON t_project_score(project_id, snapshot_week);

-- 项目9项里程碑表
CREATE TABLE t_project_milestone (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT UNIQUE NOT NULL,
    pre_open_business SMALLINT DEFAULT 0,
    bidding_published SMALLINT DEFAULT 0,
    bid_submitted SMALLINT DEFAULT 0,
    bid_won_published SMALLINT DEFAULT 0,
    contract_signed SMALLINT DEFAULT 0,
    service_opened SMALLINT DEFAULT 0,
    acceptance_done SMALLINT DEFAULT 0,
    invoice_issued SMALLINT DEFAULT 0,
    payment_done SMALLINT DEFAULT 0,
    service_fee_received SMALLINT DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_milestone_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);

-- 招投标节点表
CREATE TABLE t_bidding_node (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT UNIQUE NOT NULL,
    bidding_agency VARCHAR(128),
    purchase_method SMALLINT,
    announcement_date DATE,
    registration_start DATE,
    registration_end DATE,
    bid_date DATE,
    bid_result_start DATE,
    bid_result_end DATE,
    notice_received_date DATE,
    notice_original_archived SMALLINT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bidding_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);

-- 合同节点表
CREATE TABLE t_contract_node (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT UNIQUE NOT NULL,
    draft_date DATE,
    review_dept VARCHAR(255),
    approve_date DATE,
    original_archived SMALLINT DEFAULT 0,
    payment_method VARCHAR(64),
    payment_ratio VARCHAR(255),
    payment_terms VARCHAR(255),
    payment_nodes VARCHAR(500),
    has_warranty SMALLINT DEFAULT 0,
    warranty_amount DECIMAL(12,2),
    acceptance_dept VARCHAR(255),
    has_settlement_audit SMALLINT DEFAULT 0,
    invoice_date DATE,
    payment_voucher_dept VARCHAR(255),
    received_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_contract_node_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);

-- 回款节点表
CREATE TABLE t_payment_node (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    payment_no INT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    received_date DATE,
    invoice_no VARCHAR(64),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);
CREATE INDEX idx_payment_project ON t_payment_node(project_id);

-- 跟进记录表
CREATE TABLE t_follow_up (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    project_id BIGINT,
    opportunity_id BIGINT,
    current_stage VARCHAR(32),
    next_stage VARCHAR(32),
    stage_feedback TEXT,
    follow_up_date DATE NOT NULL,
    follow_up_method VARCHAR(32),
    contact_id BIGINT,
    content TEXT NOT NULL,
    next_plan TEXT,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_followup_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id),
    CONSTRAINT fk_followup_project FOREIGN KEY (project_id) REFERENCES t_project(id),
    CONSTRAINT fk_followup_opportunity FOREIGN KEY (opportunity_id) REFERENCES t_opportunity(id),
    CONSTRAINT fk_followup_contact FOREIGN KEY (contact_id) REFERENCES t_contact(id),
    CONSTRAINT fk_followup_creator FOREIGN KEY (created_by) REFERENCES t_user(id)
);
CREATE INDEX idx_followup_customer_date ON t_follow_up(customer_id, follow_up_date DESC);
CREATE INDEX idx_followup_opportunity ON t_follow_up(opportunity_id);
CREATE INDEX idx_followup_project ON t_follow_up(project_id);
CREATE INDEX idx_followup_creator ON t_follow_up(created_by);

-- 任务表
CREATE TABLE t_task (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    follow_up_id BIGINT,
    plan_stage VARCHAR(32),
    plan_date DATE NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    close_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_task_user FOREIGN KEY (owner_user_id) REFERENCES t_user(id),
    CONSTRAINT fk_task_customer FOREIGN KEY (customer_id) REFERENCES t_customer(id),
    CONSTRAINT fk_task_followup FOREIGN KEY (follow_up_id) REFERENCES t_follow_up(id)
);
CREATE INDEX idx_task_user_status_date ON t_task(owner_user_id, status, plan_date);

-- 合同表
CREATE TABLE t_contract (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_contract_project FOREIGN KEY (project_id) REFERENCES t_project(id)
);
CREATE INDEX idx_contract_project ON t_contract(project_id);

-- 返利表
CREATE TABLE t_rebate (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL,
    contract_id BIGINT,
    product_category VARCHAR(64),
    rebate_rate DECIMAL(5,4),
    total_amount DECIMAL(12,2),
    actual_amount DECIMAL(12,2),
    confirm_status SMALLINT DEFAULT 1,
    payment_status SMALLINT DEFAULT 1,
    rebate_type SMALLINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rebate_channel FOREIGN KEY (channel_id) REFERENCES t_channel(id),
    CONSTRAINT fk_rebate_contract FOREIGN KEY (contract_id) REFERENCES t_contract(id)
);
CREATE INDEX idx_rebate_channel ON t_rebate(channel_id);
CREATE INDEX idx_rebate_contract ON t_rebate(contract_id);

-- 返利率配置表（按产品和渠道配置返利率）
CREATE TABLE t_rebate_rate (
    id BIGSERIAL PRIMARY KEY,
    product_category VARCHAR(64) NOT NULL,
    channel_id BIGINT,
    rate DECIMAL(5,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_rebate_rate_channel FOREIGN KEY (channel_id) REFERENCES t_channel(id),
    CONSTRAINT uq_rebate_rate UNIQUE (product_category, channel_id, effective_from)
);
CREATE INDEX idx_rebate_rate_product ON t_rebate_rate(product_category);
CREATE INDEX idx_rebate_rate_channel ON t_rebate_rate(channel_id);

-- 阶段字典表
CREATE TABLE t_stage (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    parent_id BIGINT,
    level INT NOT NULL DEFAULT 1,
    sort INT DEFAULT 0,
    stage_type VARCHAR(32),
    is_enabled SMALLINT DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stage_parent FOREIGN KEY (parent_id) REFERENCES t_stage(id)
);
CREATE INDEX idx_stage_parent ON t_stage(parent_id);
CREATE INDEX idx_stage_type ON t_stage(stage_type);

-- 初始化阶段字典数据
INSERT INTO t_stage (code, name, parent_id, level, sort, stage_type) VALUES
-- P级节点
('P1', 'P1 价值验证', NULL, 1, 1, 'P_NODE'),
('P2', 'P2 立项', NULL, 1, 2, 'P_NODE'),
('P3', 'P3 招投标', NULL, 1, 3, 'P_NODE'),
('P4', 'P4 合同', NULL, 1, 4, 'P_NODE'),
('P5', 'P5 服务', NULL, 1, 5, 'P_NODE'),
('P6', 'P6 续签', NULL, 1, 6, 'P_NODE'),
('P7', 'P7 服务中', NULL, 1, 7, 'P_NODE'),
('P8', 'P8 已完成', NULL, 1, 8, 'P_NODE'),

-- 6大阶段
('VALUE_VERIFY', '价值验证', NULL, 1, 1, 'STAGE_6'),
('LIXIANG', '立项', NULL, 1, 2, 'STAGE_6'),
('ZHAOBIAO', '招投标', NULL, 1, 3, 'STAGE_6'),
('HETONG', '合同', NULL, 1, 4, 'STAGE_6'),
('FUWU', '服务', NULL, 1, 5, 'STAGE_6'),
('XUQIAN', '续签', NULL, 1, 6, 'STAGE_6'),

-- 价值验证子阶段
('VV_DEMONSTRATION', '产品演示', (SELECT id FROM t_stage WHERE code = 'VALUE_VERIFY'), 2, 1, 'STAGE_6'),
('VV_PROOF', 'POC验证', (SELECT id FROM t_stage WHERE code = 'VALUE_VERIFY'), 2, 2, 'STAGE_6'),
('VV_BUDGET', '预算落实', (SELECT id FROM t_stage WHERE code = 'VALUE_VERIFY'), 2, 3, 'STAGE_6'),

-- 立项子阶段
('LX_PLAN', '立项计划', (SELECT id FROM t_stage WHERE code = 'LIXIANG'), 2, 1, 'STAGE_6'),
('LX_APPROVAL', '立项审批', (SELECT id FROM t_stage WHERE code = 'LIXIANG'), 2, 2, 'STAGE_6'),

-- 招投标子阶段
('ZB_Announcement', '招标公告', (SELECT id FROM t_stage WHERE code = 'ZHAOBIAO'), 2, 1, 'STAGE_6'),
('ZB_REGISTER', '报名', (SELECT id FROM t_stage WHERE code = 'ZHAOBIAO'), 2, 2, 'STAGE_6'),
('ZB_BID', '投标', (SELECT id FROM t_stage WHERE code = 'ZHAOBIAO'), 2, 3, 'STAGE_6'),
('ZB_WIN', '中标公示', (SELECT id FROM t_stage WHERE code = 'ZHAOBIAO'), 2, 4, 'STAGE_6'),

-- 合同子阶段
('HT_DRAFT', '起草', (SELECT id FROM t_stage WHERE code = 'HETONG'), 2, 1, 'STAGE_6'),
('HT_REVIEW', '审核', (SELECT id FROM t_stage WHERE code = 'HETONG'), 2, 2, 'STAGE_6'),
('HT_SIGN', '签订', (SELECT id FROM t_stage WHERE code = 'HETONG'), 2, 3, 'STAGE_6'),
('HT_ARCHIVE', '归档', (SELECT id FROM t_stage WHERE code = 'HETONG'), 2, 4, 'STAGE_6'),

-- 服务子阶段
('FW_OPEN', '开通', (SELECT id FROM t_stage WHERE code = 'FUWU'), 2, 1, 'STAGE_6'),
('FW_TRAINING', '培训', (SELECT id FROM t_stage WHERE code = 'FUWU'), 2, 2, 'STAGE_6'),
('FW_ACCEPTANCE', '验收', (SELECT id FROM t_stage WHERE code = 'FUWU'), 2, 3, 'STAGE_6'),

-- 续签子阶段
('XQ_NEGOTIATE', '续约谈判', (SELECT id FROM t_stage WHERE code = 'XUQIAN'), 2, 1, 'STAGE_6'),
('XQ_CONTRACT', '续签合同', (SELECT id FROM t_stage WHERE code = 'XUQIAN'), 2, 2, 'STAGE_6');

-- 扩展字典表数据
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
-- 跟进方式
('follow_up_method', 'VISIT', '拜访', 1, '上门拜访'),
('follow_up_method', 'PHONE', '电话', 2, '电话沟通'),
('follow_up_method', 'WECHAT', '微信', 3, '微信沟通'),
('follow_up_method', 'EMAIL', '邮件', 4, '邮件往来'),
('follow_up_method', 'VIDEO', '视频', 5, '视频会议'),
('follow_up_method', 'EVENT', '活动', 6, '参加活动'),

-- 联系人类型
('contact_type', 'DECISION_MAKER', '重要决策人', 1, '关键决策人'),
('contact_type', 'BUSINESS_CONTACT', '业务对接人', 2, '日常业务对接'),
('contact_type', 'OPERATOR', '操作员', 3, '系统操作员'),

-- 采购方式
('purchase_method', 'INQUIRY_INTERNAL', '询价内', 1, '内部询价'),
('purchase_method', 'INQUIRY_EXTERNAL', '询价外', 2, '外部询价'),
('purchase_method', 'COMPETITIVE_NEGOTIATION', '竞争性谈判', 3, '竞争性谈判'),
('purchase_method', 'COMPETITIVE_CONSULTATION', '竞争性磋商', 4, '竞争性磋商'),
('purchase_method', 'PUBLIC_BIDDING', '公开招标', 5, '公开招标'),
('purchase_method', 'SINGLE_SOURCE', '单一来源', 6, '单一来源'),
('purchase_method', 'INVITATION_BIDDING', '邀请招标', 7, '邀请招标'),

-- 行政级别
('admin_level', 'PROVINCIAL', '省厅', 1, '省级单位'),
('admin_level', 'CITY', '地市', 2, '地市级单位'),
('admin_level', 'COUNTY', '区县', 3, '区县级单位'),

-- 销售方式
('sales_method', 'DIRECT', '直销', 1, '直销模式'),
('sales_method', 'DISTRIBUTION', '经销', 2, '经销模式'),

-- 客户分层
('customer_layer', 'A', 'A类', 1, '高价值客户'),
('customer_layer', 'B', 'B类', 2, '中价值客户'),
('customer_layer', 'C', 'C类', 3, '普通客户'),

-- 任务状态
('task_status', 'PENDING', '待完成', 1, ''),
('task_status', 'COMPLETED', '已完成', 2, ''),
('task_status', 'CLOSED', '已关闭', 3, ''),

-- 返利类型
('rebate_type', 'PERFORMANCE', '业绩完成返利', 1, '合同签订后生成'),
('rebate_type', 'PAYMENT', '回款返利', 2, '回款到账后生成'),
('rebate_type', 'SERVICE', '服务返利', 3, '服务期满9个月后生成'),

-- 返利确认状态
('rebate_confirm_status', 'PENDING', '未确认', 1, ''),
('rebate_confirm_status', 'CONFIRMED', '已确认', 2, ''),

-- 返利付款状态
('rebate_payment_status', 'UNPAID', '未付款', 1, ''),
('rebate_payment_status', 'PAID', '已付款', 2, ''),

-- 回款状态
('payment_status', 'PENDING', '待回款', 1, ''),
('payment_status', 'RECEIVED', '已到账', 2, ''),
('payment_status', 'OVERDUE', '逾期', 3, ''),

-- 数据权限范围类型
('data_scope_type', 'ALL', '全部', 1, '查看全部数据'),
('data_scope_type', 'CHANNEL', '本渠道', 2, '查看本渠道数据'),
('data_scope_type', 'REGION', '本区域', 3, '查看本区域数据'),
('data_scope_type', 'DEPT', '本部门', 4, '查看本部门数据'),
('data_scope_type', 'SELF', '本人', 5, '仅查看本人数据'),

-- 区域字典
('region', 'EAST_CHINA', '华东', 1, '华东区域'),
('region', 'SOUTH_CHINA', '华南', 2, '华南区域'),
('region', 'NORTH_CHINA', '华北', 3, '华北区域'),
('region', 'CENTRAL_CHINA', '华中', 4, '华中区域'),
('region', 'NORTHWEST', '西北', 5, '西北区域'),
('region', 'SOUTHWEST', '西南', 6, '西南区域'),
('region', 'NORTHEAST', '东北', 7, '东北区域');

-- ========================================
-- 权限与菜单相关表
-- ========================================

-- 菜单表
CREATE TABLE t_menu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL UNIQUE,
    path VARCHAR(255),
    parent_id BIGINT,
    sort INT DEFAULT 0,
    icon VARCHAR(64),
    type SMALLINT NOT NULL,
    permission VARCHAR(128),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_menu_parent ON t_menu(parent_id);
CREATE INDEX idx_menu_status ON t_menu(status);

-- 角色菜单关联表
CREATE TABLE t_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id),
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES t_role(id),
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES t_menu(id)
);

-- 角色操作关联表
CREATE TABLE t_role_operation (
    role_id BIGINT NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (role_id, operation_code),
    CONSTRAINT fk_role_operation_role FOREIGN KEY (role_id) REFERENCES t_role(id)
);

-- ========================================
-- 审计日志表
-- ========================================

CREATE TABLE t_audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    username VARCHAR(64),
    operation VARCHAR(32) NOT NULL,
    module VARCHAR(64),
    method VARCHAR(128),
    params TEXT,
    ip VARCHAR(64),
    status SMALLINT NOT NULL,
    error_msg TEXT,
    execute_time INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);
CREATE INDEX idx_audit_user ON t_audit_log(user_id);
CREATE INDEX idx_audit_created ON t_audit_log(created_at);
CREATE INDEX idx_audit_module ON t_audit_log(module);

-- ========================================
-- 站内信通知相关表
-- ========================================

-- 通知表
CREATE TABLE t_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type VARCHAR(32) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    related_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES t_user(id)
);
CREATE INDEX idx_notification_user ON t_notification(user_id);
CREATE INDEX idx_notification_status ON t_notification(status);
CREATE INDEX idx_notification_created ON t_notification(created_at);

-- 通知模板表
CREATE TABLE t_notification_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(32) NOT NULL,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ========================================
-- 初始化菜单数据
-- ========================================

INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission) VALUES
-- 一级菜单
('工作台', 'DASHBOARD', '/dashboard', NULL, 1, 'Dashboard', 1, 'dashboard:view'),
('客户管理', 'CUSTOMER', '/customer', NULL, 2, 'Customer', 1, 'customer:view'),
('商机管理', 'OPPORTUNITY', '/opportunity', NULL, 3, 'Opportunity', 1, 'opportunity:view'),
('项目管理', 'PROJECT', '/project', NULL, 4, 'Project', 1, 'project:view'),
('商务管理', 'BUSINESS', '/business', NULL, 5, 'Business', 1, 'business:view'),
('系统管理', 'SYSTEM', '/system', NULL, 6, 'System', 1, 'system:view'),

-- 工作台子菜单
('我的工作台', 'MY_DASHBOARD', '/dashboard/my', 1, 1, NULL, 2, 'dashboard:my'),
('渠道工作台', 'CHANNEL_DASHBOARD', '/dashboard/channel', 1, 2, NULL, 2, 'dashboard:channel'),

-- 客户管理子菜单
('客户列表', 'CUSTOMER_LIST', '/customer/list', 2, 1, NULL, 2, 'customer:list'),
('联系人管理', 'CONTACT_MANAGE', '/customer/contact', 2, 2, NULL, 2, 'contact:manage'),

-- 商机管理子菜单
('我的报备', 'MY_OPPORTUNITY', '/opportunity/my', 3, 1, NULL, 2, 'opportunity:my'),
('全部商机', 'ALL_OPPORTUNITY', '/opportunity/all', 3, 2, NULL, 2, 'opportunity:all'),
('报备审批', 'OPPORTUNITY_APPROVAL', '/opportunity/approval', 3, 3, NULL, 2, 'opportunity:approval'),

-- 项目管理子菜单
('项目列表', 'PROJECT_LIST', '/project/list', 4, 1, NULL, 2, 'project:list'),
('项目统计', 'PROJECT_STATISTICS', '/project/statistics', 4, 2, NULL, 2, 'project:statistics'),

-- 商务管理子菜单
('合同管理', 'CONTRACT_MANAGE', '/business/contract', 5, 1, NULL, 2, 'contract:manage'),
('返利管理', 'REBATE_MANAGE', '/business/rebate', 5, 2, NULL, 2, 'rebate:manage'),

-- 系统管理子菜单（仅管理员）
('用户管理', 'USER_MANAGE', '/system/user', 6, 1, NULL, 2, 'system:user'),
('角色管理', 'ROLE_MANAGE', '/system/role', 6, 2, NULL, 2, 'system:role'),
('字典管理', 'DICTIONARY_MANAGE', '/system/dictionary', 6, 3, NULL, 2, 'system:dictionary'),
('单位管理', 'UNIT_MANAGE', '/system/unit', 6, 4, NULL, 2, 'system:unit'),
('渠道分配', 'CHANNEL_ASSIGN', '/system/channel', 6, 5, NULL, 2, 'system:channel'),
('审计日志', 'AUDIT_LOG', '/system/audit', 6, 6, NULL, 2, 'system:audit');

-- 初始化角色菜单关联（管理员全部权限，其他角色根据需要配置）
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 1, id FROM t_menu;

-- 初始化 CYBD 角色菜单（除系统管理外的全部权限）
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 5, id FROM t_menu WHERE code NOT LIKE 'SYSTEM%';

-- 初始化角色操作权限
INSERT INTO t_role_operation (role_id, operation_code) VALUES
-- 管理员全部权限
(1, 'dashboard:view'), (1, 'dashboard:my'), (1, 'dashboard:channel'),
(1, 'customer:view'), (1, 'customer:list'), (1, 'customer:create'), (1, 'customer:edit'), (1, 'customer:delete'),
(1, 'contact:manage'),
(1, 'opportunity:view'), (1, 'opportunity:my'), (1, 'opportunity:all'), (1, 'opportunity:create'), (1, 'opportunity:edit'), (1, 'opportunity:delete'), (1, 'opportunity:approval'),
(1, 'project:view'), (1, 'project:list'), (1, 'project:create'), (1, 'project:edit'), (1, 'project:delete'), (1, 'project:statistics'),
(1, 'business:view'), (1, 'contract:manage'), (1, 'rebate:manage'),
(1, 'system:view'), (1, 'system:user'), (1, 'system:role'), (1, 'system:dictionary'), (1, 'system:unit'), (1, 'system:channel'), (1, 'system:audit'),

-- CYBD 权限
(5, 'opportunity:approval'),
(5, 'system:user'), (5, 'system:role'), (5, 'system:dictionary'), (5, 'system:unit'),

-- 渠道 BD 权限
(4, 'dashboard:my'),
(4, 'customer:view'), (4, 'customer:list'), (4, 'customer:create'), (4, 'customer:edit'), (4, 'contact:manage'),
(4, 'opportunity:view'), (4, 'opportunity:my'), (4, 'opportunity:create'), (4, 'opportunity:edit'),
(4, 'project:view'), (4, 'project:list'), (4, 'project:create'), (4, 'project:edit'),

-- 渠道负责人权限
(3, 'dashboard:my'), (3, 'dashboard:channel'), (3, 'rebate:manage'),
(3, 'customer:view'), (3, 'customer:list'), (3, 'customer:create'), (3, 'customer:edit'), (3, 'contact:manage'),
(3, 'opportunity:view'), (3, 'opportunity:my'), (3, 'opportunity:create'), (3, 'opportunity:edit'),
(3, 'project:view'), (3, 'project:list'), (3, 'project:create'), (3, 'project:edit'),

-- 大区总权限
(2, 'dashboard:channel'),
(2, 'customer:view'), (2, 'customer:list'), (2, 'customer:create'), (2, 'customer:edit'),
(2, 'opportunity:view'), (2, 'opportunity:all'),
(2, 'project:view'), (2, 'project:list'), (2, 'project:statistics');
