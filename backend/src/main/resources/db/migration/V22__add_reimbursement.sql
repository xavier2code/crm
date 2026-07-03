-- ========================================
-- V22: 报销管理（差旅 / 招待）
-- 业务依据：TODO.md #20
-- 设计：
--   * t_reimbursement：报销主表
--       - type：字典 reimbursement_type（TRAVEL=差旅 / ENTERTAIN=招待）
--       - status：DRAFT/PENDING/APPROVED/REJECTED/PAID
--   * t_reimbursement_attachment：附件（报销可上传多张凭证）
--   * 菜单挂在商务管理（parent_id=5）下，路径 /business/reimbursement
--   * 操作权限：view / create / approve / pay
--   * 角色授权：ADMIN 全权；CHANNEL_BD 创建/查看自己的；CHANNEL_HEAD 审批；
--               CYBD 创建/查看；FINANCE 付款 + 查已审批；ORANGE_EAGLE_SALES 申报
-- 数据库兼容：H2 / PostgreSQL
-- ========================================

-- 1) 报销主表
CREATE TABLE t_reimbursement (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    project_name_snapshot VARCHAR(255) NOT NULL,
    applicant_id BIGINT NOT NULL,
    applicant_name_snapshot VARCHAR(64) NOT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    amount DECIMAL(12,2) NOT NULL,
    expense_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    approver_id BIGINT,
    approver_name_snapshot VARCHAR(64),
    approved_at TIMESTAMP,
    approval_comment VARCHAR(1000),
    paid_at TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reimbursement_project
        FOREIGN KEY (project_id) REFERENCES t_project(id),
    CONSTRAINT fk_reimbursement_applicant
        FOREIGN KEY (applicant_id) REFERENCES t_user(id),
    CONSTRAINT fk_reimbursement_approver
        FOREIGN KEY (approver_id) REFERENCES t_user(id),
    CONSTRAINT fk_reimbursement_creator
        FOREIGN KEY (created_by) REFERENCES t_user(id),
    CONSTRAINT chk_reimbursement_type
        CHECK (type IN ('TRAVEL', 'ENTERTAIN')),
    CONSTRAINT chk_reimbursement_status
        CHECK (status IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED', 'PAID')),
    CONSTRAINT chk_reimbursement_amount_nonneg
        CHECK (amount >= 0)
);

CREATE INDEX idx_reimbursement_project ON t_reimbursement(project_id);
CREATE INDEX idx_reimbursement_applicant ON t_reimbursement(applicant_id);
CREATE INDEX idx_reimbursement_status ON t_reimbursement(status);
CREATE INDEX idx_reimbursement_created_at ON t_reimbursement(created_at);

-- 2) 报销附件表
CREATE TABLE t_reimbursement_attachment (
    id BIGSERIAL PRIMARY KEY,
    reimbursement_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    content_type VARCHAR(128),
    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_reimbursement_attachment_reimbursement
        FOREIGN KEY (reimbursement_id) REFERENCES t_reimbursement(id),
    CONSTRAINT fk_reimbursement_attachment_uploader
        FOREIGN KEY (uploaded_by) REFERENCES t_user(id),
    CONSTRAINT chk_reimbursement_attachment_size_nonneg
        CHECK (file_size >= 0)
);

CREATE INDEX idx_reimbursement_attachment_reimbursement
    ON t_reimbursement_attachment(reimbursement_id);

-- 3) 报销类型字典
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
    ('reimbursement_type', 'TRAVEL',    '差旅', 1, '差旅交通/住宿等'),
    ('reimbursement_type', 'ENTERTAIN', '招待', 2, '业务招待');

-- 4) 报销菜单：挂在商务管理（parent_id=5）下
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '报销管理', 'REIMBURSEMENT_MANAGE', '/reimbursement', 5, 3, 'AccountBook', 2, 'reimbursement:view'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'REIMBURSEMENT_MANAGE');

-- 5) 菜单角色授权：管理员 / 渠道负责人 / 渠道 BD / CYBD / 财务 / 橙鹰销售
-- 角色 code 对照（V1 seed）：ADMIN / REGION_HEAD / CHANNEL_HEAD / CHANNEL_BD /
--                          CYBD / ORANGE_EAGLE_SALES / ORANGE_EAGLE_HEAD / FINANCE
INSERT INTO t_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM t_role r, t_menu m
WHERE m.code = 'REIMBURSEMENT_MANAGE'
  AND r.code IN ('ADMIN', 'CHANNEL_HEAD', 'CHANNEL_BD', 'CYBD', 'FINANCE', 'ORANGE_EAGLE_SALES')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 6) 操作权限
--   reimbursement:view     - 查看（申请者本人 + 审批人 + 财务 + 管理员）
--   reimbursement:create   - 创建（CHANNEL_BD / CYBD / ORANGE_EAGLE_SALES + ADMIN）
--   reimbursement:approve  - 审批（CHANNEL_HEAD + ADMIN）
--   reimbursement:pay      - 标记已付款（FINANCE + ADMIN）
INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'reimbursement:view'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CHANNEL_HEAD', 'CHANNEL_BD', 'CYBD', 'FINANCE', 'ORANGE_EAGLE_SALES')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'reimbursement:view'
  );

INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'reimbursement:create'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CHANNEL_BD', 'CYBD', 'ORANGE_EAGLE_SALES')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'reimbursement:create'
  );

INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'reimbursement:approve'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CHANNEL_HEAD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'reimbursement:approve'
  );

INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'reimbursement:pay'
FROM t_role r
WHERE r.code IN ('ADMIN', 'FINANCE')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'reimbursement:pay'
  );
