-- ========================================
-- CRM 渠道版 - 部门表及用户部门关联
-- ========================================

CREATE TABLE t_department (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) UNIQUE,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT,
    status SMALLINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_id) REFERENCES t_department(id)
);
CREATE INDEX idx_department_parent ON t_department(parent_id);

ALTER TABLE t_user ADD COLUMN department_id BIGINT;
ALTER TABLE t_user ADD CONSTRAINT fk_user_department FOREIGN KEY (department_id) REFERENCES t_department(id);

-- 初始化系统管理部门，并将 admin 归属到该部门
INSERT INTO t_department (code, name, status) VALUES ('ADMIN_DEPT', '系统管理部', 1);
UPDATE t_user SET department_id = (SELECT id FROM t_department WHERE code = 'ADMIN_DEPT') WHERE username = 'admin';
