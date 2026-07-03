-- ========================================
-- V24: 销售分配梯队配置
-- 业务依据：CRM-渠道版-开发文档.md §8 / TODO.md #19
-- 设计：
--   * t_sales_team_config：区域下的销售梯队划分（后台可配置）
--       - team_code / team_name：梯队编码/名称
--       - region_code：区域字典 code
--       - unit_codes：该梯队包含的单位 code，逗号分隔（H2/PG 兼容）
--       - effective_from / effective_to：有效期
--   * 菜单挂在系统管理（parent_id=6）下，路径 /system/sales-team
--   * 操作权限：view（ADMIN/CYBD/CHANNEL_HEAD/REGION_HEAD）/ manage（ADMIN/CYBD）
-- 数据库兼容：H2 / PostgreSQL
-- ========================================

-- 1) 销售梯队配置表
CREATE TABLE t_sales_team_config (
    id BIGSERIAL PRIMARY KEY,
    team_code VARCHAR(32) NOT NULL,
    team_name VARCHAR(64) NOT NULL,
    region_code VARCHAR(64) NOT NULL,
    unit_codes VARCHAR(2000),
    sort INT DEFAULT 0,
    remark VARCHAR(255),
    effective_from DATE,
    effective_to DATE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    UNIQUE (team_code, region_code, effective_from)
);

CREATE INDEX idx_sales_team_config_region ON t_sales_team_config(region_code);
CREATE INDEX idx_sales_team_config_team_code ON t_sales_team_config(team_code);
CREATE INDEX idx_sales_team_config_sort ON t_sales_team_config(sort);

-- 2) 销售梯队字典（供下拉选择）
INSERT INTO t_dictionary (type, code, name, sort, remark)
SELECT 'sales_team', 'TEAM_1', '第一梯队', 1, '重点销售梯队'
WHERE NOT EXISTS (SELECT 1 FROM t_dictionary WHERE type = 'sales_team' AND code = 'TEAM_1');

INSERT INTO t_dictionary (type, code, name, sort, remark)
SELECT 'sales_team', 'TEAM_2', '第二梯队', 2, '次重点销售梯队'
WHERE NOT EXISTS (SELECT 1 FROM t_dictionary WHERE type = 'sales_team' AND code = 'TEAM_2');

INSERT INTO t_dictionary (type, code, name, sort, remark)
SELECT 'sales_team', 'TEAM_3', '第三梯队', 3, '普通销售梯队'
WHERE NOT EXISTS (SELECT 1 FROM t_dictionary WHERE type = 'sales_team' AND code = 'TEAM_3');

INSERT INTO t_dictionary (type, code, name, sort, remark)
SELECT 'sales_team', 'TEAM_4', '第四梯队', 4, '潜力销售梯队'
WHERE NOT EXISTS (SELECT 1 FROM t_dictionary WHERE type = 'sales_team' AND code = 'TEAM_4');

INSERT INTO t_dictionary (type, code, name, sort, remark)
SELECT 'sales_team', 'TEAM_XUQIAN', '续签组', 5, '续签专用组'
WHERE NOT EXISTS (SELECT 1 FROM t_dictionary WHERE type = 'sales_team' AND code = 'TEAM_XUQIAN');

-- 3) 系统管理下新增"销售梯队配置"菜单
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '销售梯队配置', 'SALES_TEAM_CONFIG', '/system/sales-team', 6, 7, NULL, 2, 'sales-team:view'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'SALES_TEAM_CONFIG');

-- 4) 菜单授权：管理员 / CYBD / 渠道负责人 / 大区总
INSERT INTO t_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM t_role r, t_menu m
WHERE m.code = 'SALES_TEAM_CONFIG'
  AND r.code IN ('ADMIN', 'CYBD', 'CHANNEL_HEAD', 'REGION_HEAD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_menu rm
      WHERE rm.role_id = r.id AND rm.menu_id = m.id
  );

-- 5) 操作权限：view / manage
INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'sales-team:view'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CYBD', 'CHANNEL_HEAD', 'REGION_HEAD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'sales-team:view'
  );

INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'sales-team:manage'
FROM t_role r
WHERE r.code IN ('ADMIN', 'CYBD')
  AND NOT EXISTS (
      SELECT 1 FROM t_role_operation ro
      WHERE ro.role_id = r.id AND ro.operation_code = 'sales-team:manage'
  );
