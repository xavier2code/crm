-- 为字典表增加预置项标记
ALTER TABLE t_dictionary ADD COLUMN is_builtin SMALLINT NOT NULL DEFAULT 0;

-- 将系统初始化时写入的字典项标记为预置
UPDATE t_dictionary SET is_builtin = 1 WHERE type IN (
    'police_type', 'business_domain', 'project_type',
    'opportunity_status', 'project_status', 'stage_6'
);

-- V5 中写入的 stage / opportunity_stage 也标记为预置
UPDATE t_dictionary SET is_builtin = 1 WHERE type IN ('stage', 'opportunity_stage');
