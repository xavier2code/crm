-- 添加完整的阶段字典数据

-- 客户引流阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'CUSTOMER_OPERATION', '客户运营', 1, '客户引流-售前及市场引入客户进行免费运营'),
('stage', 'CUSTOMER_EVANGELISM', '客户布道', 2, 'BD/经销商联系客户进行公司介绍、产品宣讲等布道动作');

-- 试用阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'TRIAL_CONVERSION', '试用转化', 3, '客户满足试用条件到试用期结束');

-- 立项阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'LIXIANG_PREPARE', '立项准备', 4, '客户明确采购意愿，输出立项材料到提交党委会审批之间'),
('stage', 'LIXIANG_REPORT', '立项上报', 5, '立项材料正式提交党委会到审批结果产出之间'),
('stage', 'LIXIANG_RESULT', '立项结果', 6, '立项材料经党委会审批出结果后');

-- 招投标阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'PURCHASE_APPLY', '政府采购方式申请', 7, '完成立项后提交采购方式申请到采购方式确认之间'),
('stage', 'BIDDING', '投标', 8, '招标公示挂网到正式投标之间'),
('stage', 'BIDDING_RESULT', '投标结果', 9, '正式投标到投标结果公示挂网之间');

-- 合同阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'CONTRACT_ORDER', '合同下单', 10, '提交公司内部合同审批到完成合同签约之间'),
('stage', 'ACCOUNT_OPEN', '账号开通', 11, '提交公司内部账号开通流程到账号完成开通之间');

-- 服务阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'PRODUCT_USE', '产品使用', 12, '正式账号开通到服务期满9个月'),
('stage', 'SERVICE_STAGE', '服务阶段', 13, '服务期1-9月');

-- 续签阶段
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('stage', 'RENEW_COMMUNICATE', '续签沟通', 14, '正常客户到期前3个月到服务期结束');

-- 更新商机阶段常量（用于初始化）
INSERT INTO t_dictionary (type, code, name, sort, remark) VALUES
('opportunity_stage', 'DRAFT', '草稿', 1, '商机草稿状态'),
('opportunity_stage', 'IN_PROGRESS', '商机中', 2, '商机审批通过，正在跟进'),
('opportunity_stage', 'IN_PROJECT', '项目中', 3, '商机已转项目'),
('opportunity_stage', 'SERVICE', '服务中', 4, '项目正式账号已开通'),
('opportunity_stage', 'COMPLETED', '完成', 5, '项目已完成');
