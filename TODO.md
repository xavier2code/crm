# CRM 渠道版 - 未完成功能清单

> 生成时间：2026-07-02
> 基于 `CRM-渠道版-开发文档.md` 及当前前后端代码扫描结果整理

---

## 🔴 高优先级（核心业务流程，建议优先补齐）

### 1. 项目列表页
- [ ] 实现 `frontend/src/pages/project/index.tsx`
- [ ] 项目列表：搜索、筛选、分页
- [ ] 状态筛选：项目中 / 项目中断 / 项目完成 / 项目终止
- [ ] P 级节点筛选
- [ ] 新建项目入口（商机转项目）
- [ ] 列表跳转详情页

### 2. 项目详情路由
- [ ] 在 `frontend/src/router/index.tsx` 注册 `project/:id` 路由
- [ ] 确保从列表/商机页可正常跳转

### 3. 项目过程聚合接口
- [ ] 后端新增 `GET /api/projects/{id}/process` 或前端改为直接调用 `getProject`
- [ ] 统一 P 级节点 / 6 大阶段 / 里程碑 / 招投标 / 合同 / 回款 / 双精评分 数据聚合

### 4. 客户管理前端
- [ ] 客户列表页 `frontend/src/pages/customer/index.tsx`
- [ ] 新增客户页 `frontend/src/pages/customer/create/index.tsx`
- [ ] 客户详情页 `frontend/src/pages/customer/detail/index.tsx`
- [ ] 编辑客户页 `frontend/src/pages/customer/edit/index.tsx`
- [ ] 联系人维护弹窗/页面
- [ ] 单位选择、警种下拉、手机号脱敏展示

### 5. 商机报备前端
- [ ] 我的报备/全部商机列表页 `frontend/src/pages/opportunity/index.tsx`
- [ ] 新建/编辑报备表单
- [ ] 报备审批列表与操作（通过/驳回/审批意见）
- [ ] 报备状态流转展示

### 6. 合同管理
- [ ] 前端合同列表页 `frontend/src/pages/contract/index.tsx`
- [ ] 合同详情/状态变更页
- [ ] 后端修复 `ContractNodeService.getChannelIdFromProject` 硬编码桩
- [ ] 后端修复 `ContractNodeService.getProductCategoryFromProject` 硬编码桩
- [ ] 合同与项目关联选择

### 7. 单位/渠道分配
- [ ] 后端完整实现 4 级分配链路（大区总→BD→渠道→渠道BD）
- [ ] 单位分配 API：`POST /api/units/{id}/assign`
- [ ] 单位列表页 `frontend/src/pages/system/units/index.tsx`
- [ ] 渠道负责人/BD 分配 UI

### 8. 系统管理 - 用户/角色
- [ ] 用户列表页 `frontend/src/pages/system/users/index.tsx`
- [ ] 新增/编辑用户、重置密码
- [ ] 角色列表页 `frontend/src/pages/system/roles/index.tsx`
- [ ] 角色权限配置：菜单树 + 操作编码勾选
- [ ] 修复用户创建时 `operatorId` 硬编码为 `1L`

### 9. 数据权限配置
- [ ] 数据权限配置页面
- [ ] 用户详情页中“数据权限”配置 UI
- [ ] 后台管理菜单中增加“数据权限”入口

### 10. 跟进记录 / 日程
- [ ] 日程页（今日/未来 3 天/已完成）
- [ ] 客户跟进历史页
- [ ] 新增/编辑跟进记录弹窗
- [ ] 阶段反馈必填校验（当前阶段 ≠ 下一步阶段时）
- [ ] 任务按时填写跟进后自动消除闭环

---

## 🟡 中优先级（重要支撑模块）

### 11. 任务管理
- [ ] 任务列表/日历 UI
- [ ] 今日任务、完成任务、关闭任务（必填原因）

### 12. 返利管理
- [ ] 顶部菜单增加“返利管理”入口
- [ ] 返利率配置菜单入口
- [ ] 后端补全回款返利、服务返利的自动扫描生成
- [ ] 修复返利生成时渠道 ID / 产品类别硬编码问题

### 13. 个人工作台
- [ ] `frontend/src/pages/dashboard/index.tsx` 对接真实 API
- [ ] 使用 `useDashboard` hook 替换写死数据

### 14. 渠道工作台
- [ ] 顶部菜单增加“渠道工作台”入口
- [ ] 确保角色权限匹配后可见

---

## 🟢 低优先级（可延后）

### 15. 审计日志
- [ ] 注册审计日志路由
- [ ] 实现 `frontend/src/pages/system/audit-log/index.tsx`
- [ ] 按模块/操作/时间筛选

### 16. 通知中心
- [ ] 顶部导航消息角标
- [ ] 通知列表页
- [ ] 全部已读功能

### 17. 认证登录优化
- [ ] 设置部门信息
- [ ] 检查密码强度
- [ ] 密码历史 90 天强制改密策略

### 18. 商机报备优化
- [ ] 实现 48h 钉钉催办通知 `notifyApprovalDeadline`
- [ ] 独立重新提交（re-submit）端点

### 19. 销售分配梯队
- [ ] 评估是否需要实现
- [ ] 如需要：设计 `t_sales_team_config` 表及后端 CRUD

### 20. 报销管理
- [ ] 与产品确认是否保留该模块
- [ ] 如保留：补齐前后端

---

## 当前建议执行顺序

1. **项目模块收尾**：项目列表页 + 项目详情路由 + `/process` 接口
2. **补齐客户/商机/合同前端**：让核心业务流转可用
3. **系统管理前端**：用户/角色/单位/数据权限
4. **跟进与日程**：日常操作入口
5. **中低优先级模块**：按产品节奏逐步补齐
