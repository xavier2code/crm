-- 添加商机 stage 字段
ALTER TABLE t_opportunity ADD COLUMN stage VARCHAR(32);

-- 添加索引
CREATE INDEX idx_opportunity_stage ON t_opportunity(stage);

-- 初始化现有商机状态
UPDATE t_opportunity SET stage = 'DRAFT' WHERE status = 1;
UPDATE t_opportunity SET stage = 'IN_PROGRESS' WHERE status = 2;
UPDATE t_opportunity SET stage = 'ACTIVE' WHERE status = 3;
UPDATE t_opportunity SET stage = 'FAILED' WHERE status = 4;
UPDATE t_opportunity SET stage = 'EXPIRED' WHERE status = 5;
