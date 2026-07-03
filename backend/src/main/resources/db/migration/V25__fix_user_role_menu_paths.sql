-- 修复系统管理菜单路径：与前端路由 system/users, system/roles 对齐
UPDATE t_menu SET path = '/system/users' WHERE code = 'USER_MANAGE' AND path = '/system/user';
UPDATE t_menu SET path = '/system/roles' WHERE code = 'ROLE_MANAGE' AND path = '/system/role';
