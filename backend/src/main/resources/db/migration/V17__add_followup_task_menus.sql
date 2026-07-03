-- ========================================
-- V17: 新增「跟进记录」「任务管理」菜单
-- 业务依据：TODO.md #10 #11
-- 角色映射（V2 seed 注释）：
--   id=1 管理员（SUPER_ADMIN）
--   id=2 大区总（REGION_HEAD）
--   id=3 渠道负责人（CHANNEL_HEAD）
--   id=4 渠道 BD（CHANNEL_BD）
--   id=5 CYBD
-- ========================================

-- 1) 跟进记录
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '跟进记录', 'FOLLOW_UP', '/followup', 5, 1, 'Opportunity', 2, 'followup:manage'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'FOLLOW_UP');

-- 2) 任务管理
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '任务管理', 'TASK', '/task', 5, 2, 'Project', 2, 'task:manage'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'TASK');

-- 3) 角色授权：管理员 1 / 渠道负责人 3 / 渠道 BD 4 / CYBD 5 可见；大区总 2 仅任务（只读）
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 1, m.id FROM t_menu m WHERE m.code IN ('FOLLOW_UP', 'TASK')
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);

INSERT INTO t_role_menu (role_id, menu_id)
SELECT 3, m.id FROM t_menu m WHERE m.code IN ('FOLLOW_UP', 'TASK')
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 3 AND rm.menu_id = m.id);

INSERT INTO t_role_menu (role_id, menu_id)
SELECT 4, m.id FROM t_menu m WHERE m.code IN ('FOLLOW_UP', 'TASK')
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 4 AND rm.menu_id = m.id);

INSERT INTO t_role_menu (role_id, menu_id)
SELECT 5, m.id FROM t_menu m WHERE m.code IN ('FOLLOW_UP', 'TASK')
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 5 AND rm.menu_id = m.id);

INSERT INTO t_role_menu (role_id, menu_id)
SELECT 2, m.id FROM t_menu m WHERE m.code = 'TASK'
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 2 AND rm.menu_id = m.id);

-- 4) 操作权限：管理员 / 渠道负责人 / 渠道 BD 完整；CYBD 跟进只读 + 任务管理
INSERT INTO t_role_operation (role_id, operation_code) VALUES
  (1, 'followup:manage'), (1, 'task:manage'),
  (3, 'followup:manage'), (3, 'task:manage'),
  (4, 'followup:manage'), (4, 'task:manage'),
  (5, 'followup:view'), (5, 'task:manage');
