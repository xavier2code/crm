-- ========================================
-- V20: 单位分配 4 级链路（开发文档 §9.5）
-- 业务侧 model：把"单位"分配给 BD / 渠道 BD。
-- 大区总 → BD / 大区总 → 渠道负责人（沿用 t_user_channel）/
-- BD → 渠道（沿用 t_user_channel）/ 渠道负责人 → 渠道 BD 全部以本表 + t_user_channel 表达。
-- ========================================

CREATE TABLE t_unit_assignment (
    id BIGSERIAL PRIMARY KEY,
    unit_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    -- BD = 大区总或 BD 直接指派的 BD；CHANNEL_BD = 渠道负责人指派的渠道 BD
    assign_scope VARCHAR(16) NOT NULL,
    -- 仅 assign_scope = CHANNEL_BD 时有值
    channel_id BIGINT,
    assigned_by BIGINT,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_unit_assignment_unit
        FOREIGN KEY (unit_id) REFERENCES t_unit(id),
    CONSTRAINT fk_unit_assignment_user
        FOREIGN KEY (user_id) REFERENCES t_user(id),
    CONSTRAINT fk_unit_assignment_channel
        FOREIGN KEY (channel_id) REFERENCES t_channel(id),
    CONSTRAINT fk_unit_assignment_assignor
        FOREIGN KEY (assigned_by) REFERENCES t_user(id),
    CONSTRAINT chk_unit_assignment_scope
        CHECK (assign_scope IN ('BD', 'CHANNEL_BD')),
    CONSTRAINT chk_unit_assignment_channel_required
        CHECK (
            (assign_scope = 'CHANNEL_BD' AND channel_id IS NOT NULL AND channel_id <> 0)
         OR (assign_scope = 'BD' AND (channel_id IS NULL OR channel_id = 0))
        )
);

-- 同一 (单位, BD, 渠道[或 0 代表 BD 范围]) 唯一，避免重复指派。
-- PG 不支持 partial index WHERE 写法；改用 sentinel：BD 范围下 channel_id 存 0，
-- Java 端读写时把 0 当作 null。CHECK 约束保持原语义（BD 范围禁止真 null）。
ALTER TABLE t_unit_assignment ALTER COLUMN channel_id SET DEFAULT 0;

CREATE UNIQUE INDEX uk_unit_assignment_triplet
    ON t_unit_assignment(unit_id, user_id, channel_id);

CREATE INDEX idx_unit_assignment_user_scope
    ON t_unit_assignment(user_id, assign_scope);

CREATE INDEX idx_unit_assignment_channel
    ON t_unit_assignment(channel_id);

-- 1) 新增"单位分配"菜单（业务侧，非系统管理）
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '单位分配', 'UNIT_ASSIGN', '/business/units', 5, 4, NULL, 2, 'unit:assign'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'UNIT_ASSIGN');

-- 2) 授权菜单给：大区总 / 渠道负责人 / 渠道 BD / CYBD / 管理员
INSERT INTO t_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM t_role r, t_menu m
WHERE m.code = 'UNIT_ASSIGN'
  AND r.code IN ('ADMIN', 'CYBD', 'REGION_HEAD', 'CHANNEL_HEAD', 'CHANNEL_BD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 3) 授权 unit:assign 操作码（同上 5 个角色）
INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'unit:assign'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CYBD', 'REGION_HEAD', 'CHANNEL_HEAD', 'CHANNEL_BD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'unit:assign'
  );
