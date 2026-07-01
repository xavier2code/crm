-- 添加数据库唯一约束以防止竞态条件
-- Task 15: 修复竞态条件
--
-- 说明：项目的唯一约束保护工作在 V2 迁移中已大部分完成：
-- - Role.code ✅ UNIQUE (V2)
-- - Unit.code ✅ UNIQUE (V2)
-- - Customer(unit_id, police_type) ✅ UNIQUE (V2)
-- - Project.opportunity_id ✅ UNIQUE (V2)
-- - BiddingNode.project_id ✅ UNIQUE (V2)
-- - ContractNode.project_id ✅ UNIQUE (V2)
-- - User.username ✅ UNIQUE (V2)
--
-- V8 仅补充遗漏的 Contract.project_id 约束

-- 合同表：每个项目只能有一个合同
ALTER TABLE t_contract ADD CONSTRAINT uk_contract_project UNIQUE (project_id);

-- 注意：其他表（role, user, unit, customer）已有唯一约束
-- t_role.code - 已有 UNIQUE 约束
-- t_user.username - 已有 UNIQUE 约束
-- t_unit(name, region) - 已有 UNIQUE 约束
-- t_customer(unit_id, police_type) - 已有 UNIQUE 约束
-- t_project.opportunity_id - 已有 UNIQUE 约束
-- t_bidding_node.project_id - 已有 UNIQUE 约束
-- t_contract_node.project_id - 已有 UNIQUE 约束

-- 商机保护索引（已存在，仅作记录）
-- CREATE INDEX uk_opportunity_protection ON t_opportunity(customer_id, business_domain, status);
