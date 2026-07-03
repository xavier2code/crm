-- ========================================
-- V16: 统一数据权限维度定义
-- 业务依据：TODO.md #9
-- 设计：
--   - t_data_permission.scope_type SMALLINT -> VARCHAR(32)
--   - t_role.data_scope_type        SMALLINT -> VARCHAR(32)
--   - 历史数字按 V2 字典（1=ALL/2=CHANNEL/3=REGION/4=DEPT/5=SELF）翻译为枚举 code
--   - DataPermissionService 旧 1=业务域/4=警种 与 AuthService 1=ALL 冲突；
--     V16 之后 DataScopeDimension 枚举为单一事实来源
-- 数据库兼容：H2 / PostgreSQL
-- ========================================

-- 1) t_data_permission.scope_type 列类型迁移
ALTER TABLE t_data_permission ADD COLUMN scope_type_new VARCHAR(32);
UPDATE t_data_permission SET scope_type_new =
    CASE scope_type
        WHEN 1 THEN 'ALL'         -- 旧 1 历史上歧义为 ALL/业务域；按最广义取 ALL
        WHEN 2 THEN 'CHANNEL'
        WHEN 3 THEN 'REGION'
        WHEN 4 THEN 'UNIT'        -- V2 字典中 DEPT 改写为 UNIT
        WHEN 5 THEN 'SELF'
        ELSE NULL
    END
WHERE scope_type_new IS NULL;
ALTER TABLE t_data_permission DROP COLUMN scope_type;
ALTER TABLE t_data_permission RENAME COLUMN scope_type_new TO scope_type;
ALTER TABLE t_data_permission ALTER COLUMN scope_type VARCHAR(32) NOT NULL;

-- 2) t_role.data_scope_type 列类型迁移
ALTER TABLE t_role ADD COLUMN data_scope_type_new VARCHAR(32);
UPDATE t_role SET data_scope_type_new =
    CASE data_scope_type
        WHEN 1 THEN 'ALL'
        WHEN 2 THEN 'CHANNEL'
        WHEN 3 THEN 'REGION'
        WHEN 4 THEN 'UNIT'
        WHEN 5 THEN 'SELF'
        ELSE 'SELF'              -- 安全兜底：未识别值按 SELF 处理（最严格）
    END
WHERE data_scope_type_new IS NULL;
ALTER TABLE t_role DROP COLUMN data_scope_type;
ALTER TABLE t_role RENAME COLUMN data_scope_type_new TO data_scope_type;
ALTER TABLE t_role ALTER COLUMN data_scope_type VARCHAR(32) NOT NULL;
ALTER TABLE t_role ALTER COLUMN data_scope_type SET DEFAULT 'SELF';
