-- 返利自动生成 scheduler 需要按回款节点去重，避免重复生成回款返利
ALTER TABLE t_rebate ADD COLUMN payment_node_id BIGINT;

CREATE INDEX idx_rebate_payment_node ON t_rebate(payment_node_id);
