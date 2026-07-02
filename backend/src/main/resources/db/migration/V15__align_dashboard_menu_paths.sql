-- ========================================
-- V15: 对齐工作台子菜单路径 + 新增「返利率配置」菜单
-- 业务依据：CRM-渠道版-开发文档.md §9.1 / §9.8
-- ========================================

-- 1) /dashboard/my → /dashboard（前端只有 1 个 DashboardPage；CHANNEL_HEAD 走页面内的"查看渠道工作台"按钮跳转）
UPDATE t_menu SET path = '/dashboard' WHERE code = 'MY_DASHBOARD';

-- 2) /dashboard/channel → /dashboard（同上，点击菜单回到"我的工作台"，由页面内按钮带 channelId 跳转）
UPDATE t_menu SET path = '/dashboard' WHERE code = 'CHANNEL_DASHBOARD';

-- 3) 新增"返利率配置"子菜单（商务管理下，仅 CYBD 可见）
INSERT INTO t_menu (name, code, path, parent_id, sort, icon, type, permission)
SELECT '返利率配置', 'REBATE_RATE_CONFIG', '/business/rebate/rates', 5, 3, NULL, 2, 'rebate:rate-config'
WHERE NOT EXISTS (SELECT 1 FROM t_menu WHERE code = 'REBATE_RATE_CONFIG');

-- 4) 授权：CYBD（role id=1，按 V2 seed 约定）
INSERT INTO t_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM t_menu m
WHERE m.code = 'REBATE_RATE_CONFIG'
  AND NOT EXISTS (SELECT 1 FROM t_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id);
