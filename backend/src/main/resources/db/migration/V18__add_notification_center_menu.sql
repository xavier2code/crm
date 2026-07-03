-- ========================================
-- V18: 新增「通知中心」菜单
-- 业务依据：TODO.md #16
-- 站内信属于通用入口，对所有登录用户可见
-- 角色映射（V2 seed 注释）：
--   id=1 管理员 / 2 大区总 / 3 渠道负责人 / 4 渠道 BD / 5 CYBD
-- ========================================

INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '通知中心', 'NOTIFICATION_CENTER', '/notifications', NULL, 7, 'Bell', 1, 'notification:view'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'NOTIFICATION_CENTER');

-- 全部角色可见
INSERT INTO t_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM t_role r, t_menu m
WHERE m.code = 'NOTIFICATION_CENTER'
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = r.id AND rm.menu_id = m.id);

-- 操作权限：通知查看 + 标记已读
INSERT INTO t_role_operation (role_id, operation_code)
SELECT r.id, 'notification:view'
FROM t_role r
WHERE NOT EXISTS (SELECT 1 FROM t_role_operation ro WHERE ro.role_id = r.id AND ro.operation_code = 'notification:view');
