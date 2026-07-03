-- ========================================
-- V19: t_user 增加 password_changed_at 列用于 90 天过期策略
-- 业务依据：TODO.md #17
-- 数据库兼容：H2 / PostgreSQL
-- ========================================
ALTER TABLE t_user ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMP NULL;
