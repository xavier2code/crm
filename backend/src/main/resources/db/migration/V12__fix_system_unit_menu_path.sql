-- 修复系统管理-单位管理菜单路径：与前端路由 /system/units 保持一致
UPDATE t_menu SET path = '/system/units' WHERE code = 'UNIT_MANAGE' AND path = '/system/unit';
