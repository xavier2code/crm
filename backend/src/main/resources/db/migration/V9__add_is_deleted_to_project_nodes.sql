-- 为项目相关节点表补充逻辑删除字段，与 BaseEntity 的 @TableLogic 保持一致

ALTER TABLE t_bidding_node ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE t_contract_node ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE t_payment_node ADD COLUMN is_deleted SMALLINT NOT NULL DEFAULT 0;
