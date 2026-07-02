# CRM 渠道版 - 未完成功能清单

> 更新时间：2026-07-02
> 基于 `CRM-渠道版-开发文档.md` 与当前 `main` 分支（`fb4a2aa`）代码扫描结果
> 标注图例：✅ 已完成 · 🟡 部分完成 · ❌ 未开始

---

## 🔴 高优先级（核心业务流程，建议优先补齐）

### 1. 项目列表页
- [ ] 实现 `frontend/src/pages/project/index.tsx`（当前 3 行占位）
- [ ] 项目列表：搜索、筛选、分页
- [ ] 状态筛选：项目中 / 项目中断 / 项目完成 / 项目终止
- [ ] P 级节点筛选
- [ ] 新建项目入口（商机转项目）
- [ ] 列表跳转详情页

### 2. 项目详情路由
- [ ] 在 `frontend/src/router/index.tsx` 注册 `project/:id` 路由指向 `pages/project/detail/index.tsx`（719 行已实现，但当前 `path="project"` 仍指向 3 行占位列表）

### 3. 项目过程聚合接口
- [ ] 后端新增 `GET /api/projects/{id}/process` 或前端直接组合现有端点
- [ ] 当前 `ProjectController` 已有 P 节点/招投标/合同/回款/双精评分等分散端点，缺少统一聚合

### 4. 客户管理前端
- [x] ✅ 客户列表页 `frontend/src/pages/customer/index.tsx`（搜索、分页、分配）
- [x] ✅ 新增客户页 `frontend/src/pages/customer/create/index.tsx`
- [x] ✅ 客户详情页 `frontend/src/pages/customer/detail/index.tsx`
- [x] ✅ 编辑客户页 `frontend/src/pages/customer/edit/index.tsx`
- [x] ✅ 联系人维护弹窗/页面（决策人/对接人/操作员）
- [x] ✅ 单位选择、警种下拉、手机号脱敏展示
- [x] ✅ 后端 `CustomerController` 10 个端点已完整（`/api/customers`）

### 5. 商机报备前端
- [ ] 我的报备/全部商机列表页 `frontend/src/pages/opportunity/index.tsx`（9 行占位）
- [ ] 新建/编辑报备表单
- [ ] 报备审批列表与操作（通过/驳回/审批意见）
- [ ] 报备状态流转展示（审批中/生效中/失效/锁定）
- [ ] 报备保护提示（同客户+同业务域唯一）
- [ ] 商机转项目按钮
- [ ] 后端 `OpportunityController` 7 个端点已完整（含 `submit`/`approve`）

### 6. 合同管理
- [ ] 前端合同列表页 `frontend/src/pages/contract/index.tsx`（3 行占位）
- [ ] 合同详情/状态变更页
- [ ] 后端修复 `ContractNodeService.getChannelIdFromProject` 硬编码桩（`return 1L`）— 文件位于 `backend/src/main/java/com/cy/crm/module/project/service/ContractNodeService.java:174-178`
- [ ] 后端修复 `ContractNodeService.getProductCategoryFromProject` 硬编码桩（`return "DEFAULT"`）— 同文件 line 180-184
- [ ] 合同与项目关联选择

### 7. 单位/渠道分配（业务侧 4 级链路）
- [ ] 后端 `backend/src/main/java/com/cy/crm/module/unit/` 目录**空**，需补 4 级分配 API
- [ ] 大区总→渠道负责人、大区总→BD、BD→渠道、渠道→BD 业务侧 UI
- [ ] 后端 `admin/UserChannelService`（`77f0f04` 提交）已存在 channel 维度分配，可复用
- [x] ✅ 前端渠道管理页 `pages/system/channel/index.tsx`（582 行）已实现
- [ ] ❌ **前端渠道管理页路由 `system/channel` 未注册**（router 中缺少）
- [ ] 前端业务侧单位列表/分配入口**未实现**

### 8. 系统管理 - 用户/角色
- [ ] 用户列表页 `frontend/src/pages/system/users/index.tsx`（当前 9 行占位）
- [ ] 新增/编辑用户、重置密码、启停用
- [ ] 角色列表页 `frontend/src/pages/system/roles/index.tsx`（9 行占位）
- [ ] 角色权限配置：菜单树 + 操作编码勾选
- [ ] 修复用户创建时 `operatorId` 硬编码为 `1L` 的兜底逻辑
- [ ] **后端 `UserController` 缺 `/reset-password` 与 `/status` 端点**（只有 5 个基础端点）

### 9. 数据权限配置
- [x] ✅ 后端 `DataPermissionService`、`DataPermissionUpdateRequest`、`DataPermissionVO`（`50166df` 提交）
- [x] ✅ 前端 `api/admin/dataPermission.ts`、`hooks/useAdminDataPermissions.ts`（`50166df` 提交）
- [ ] ❌ **后端 `DataPermissionController` 缺失**（`/api/admin/users/{id}/data-permissions` 未实现）
- [ ] ❌ 用户管理页面中"数据权限"配置 UI 缺失
- [ ] ❌ 后台管理菜单中"数据权限"独立入口未加
- [ ] ⚠️ **风险**：前端 `SCOPE_TYPE`（1=业务域 2=区域 3=渠道 4=警种）与 `AuthService.buildDataScope` / `DataScopeInterceptor` 的语义（1=ALL 2=CHANNEL 3=REGION 4=UNIT 5=SELF）不一致，且拦截器未配置 `business_domain`、`police_type` 字段映射。补 Controller/UI 前需先统一维度定义，否则权限过滤无法按预期工作

### 10. 跟进记录 / 日程
- [ ] 日程页（今日/未来 3 天/已完成）
- [ ] 客户跟进历史页
- [ ] 新增/编辑跟进记录弹窗
- [ ] 阶段反馈必填校验（当前阶段 ≠ 下一步阶段时）
- [ ] 任务按时填写跟进后自动消除闭环
- [ ] 后端 `FollowUpController` 4 端点 + `TaskController` 4 端点已完整

---

## 🟡 中优先级（重要支撑模块）

### 11. 任务管理
- [ ] 任务列表/日历 UI
- [ ] 今日任务、完成任务、关闭任务（必填原因）
- [ ] 后端 `TaskController` 已完整（4 端点）

### 12. 返利管理
- [x] ✅ 后端 `RebateController`（6 端点）+ `RebateRateController`（5 端点）已完整
- [x] ✅ 前端 `pages/rebate/index.tsx`（469 行）+ `pages/rebate-rates/index.tsx`（260 行）
- [x] ✅ 路由 `/business/rebate`、`/business/rebate/rates` 已注册
- [x] ✅ V15 seed 加了"返利率配置"菜单（仅 CYBD）
- [ ] ❌ 后端**未实现返利自动生成 scheduler**（业绩完成返利/回款返利/服务返利）
- [ ] ❌ scheduler 中 30 天未跟进 → 报备失效已存在（`OpportunityExpiryScheduler`）但返利侧无定时

### 13. 个人工作台
- [x] ✅ 前端 `pages/dashboard/index.tsx`（110 行）已接 `useDashboard` 真实 API（`a35f4eb` 提交）
- [x] ✅ 渠道工作台入口按钮（CHANNEL_HEAD/CYBD 角色）

### 14. 渠道工作台
- [x] ✅ 前端 `pages/channel-dashboard/index.tsx`（262 行）— 4 大块：总览/业绩/成员/客户分布
- [x] ✅ 路由 `/dashboard/channel/:channelId`
- [x] ✅ 后端 `DashboardController.getChannelDashboard` + `ChannelDashboardVO`（含 @PreAuthorize）
- [x] ✅ V15 seed 对齐了工作台子菜单路径

---

## 🟢 低优先级（可延后）

### 15. 审计日志
- [x] ✅ 后端 `AuditLogController` 3 端点（`/api/admin/audit-logs`）
- [x] ✅ 前端 `pages/system/audit-log/index.tsx`（395 行）— 真实实现（`c0d0d9c` 提交）
- [ ] ❌ **路由 `system/audit` 未注册**（当前 router 只有 system/users/roles/dictionary/units，缺少 audit）

### 16. 通知中心
- [ ] 顶部导航消息角标（接 `NotificationController.count/unread`）
- [ ] 通知列表页（`unread` + `list` 端点已就绪）
- [ ] 全部已读功能（`/read-all` 端点已就绪）
- [ ] 48h 报备审批超时 scheduler 已实现（`OpportunityApprovalReminderScheduler`，每整点执行）
- [ ] 30 天报备失效 scheduler 已实现（`OpportunityExpiryScheduler`，每天 02:00 执行）

### 17. 认证登录优化
- [ ] 登录响应 `userInfo.departmentId/departmentName` 填充（`AuthService.login` 有 `TODO`）
- [ ] 密码强度校验（`AuthService.changePassword` 有 `TODO: 检查密码强度`）
- [ ] 密码 90 天强制过期策略（`PasswordPolicyService.expireDays` 已支持配置，未接入强制改密流程）
- [ ] **首次登录强制改密流程**（用户表有 `is_initial_password` 字段，登录接口未检测、change-password 后未重置）

### 18. 商机报备优化
- [x] ✅ 48h 钉钉催办 scheduler（`OpportunityApprovalReminderScheduler`）— 站内信形式实现
- [ ] 独立 re-submit 端点（当前用 `POST /{id}/submit` 复用）
- [ ] 报备锁定 1 个月（`t_opportunity.locked_until` 字段已存在，逻辑未审）

### 19. 销售分配梯队
- [ ] 评估是否需要实现
- [ ] 如需要：设计 `t_sales_team_config` 表及后端 CRUD

### 20. 报销管理
- [ ] 与产品确认是否保留该模块（路由 `/reimbursement` 已注册，但 Flyway 种子中**未找到报销菜单**，且**后端无报销模块**）
- [ ] 如保留：补齐前后端

---

## 当前进度概览（2026-07-02 `fb4a2aa` 盘点）

| 类别 | 完成 | 部分 | 未开始 |
|---|---|---|---|
| 🔴 高优先级 10 项 | 0 | 4 | 6 |
| 🟡 中优先级 4 项 | 2 | 1 | 1 |
| 🟢 低优先级 6 项 | 0 | 3 | 3 |
| **合计 20 项** | **2** | **8** | **10** |

**最近合并的相关 commit**：
- `a35f4eb` feat(dashboard): add channel dashboard and project statistics
- `984279d` feat(project): add detail page with process management and scoring
- `c0d0d9c` feat(audit): add audit log query page
- `82440af` feat(auth): add change password page
- `84cd144` feat(router): add channel dashboard, rebate, and rebate-rates routes
- `50166df` feat(permission): add user data permission management（缺 Controller）
- `77f0f04` feat(channel): add channel management backend and frontend
- `8c7705f` chore(db): add audit columns and align menu paths（V15 seed）

---

## 建议执行顺序

1. **统一数据权限维度定义**（#9）— 前端 `SCOPE_TYPE` 与 `DataScopeInterceptor` 语义不一致，先对齐再补 Controller/UI，否则权限过滤会按错误维度生效
2. **补齐系统管理前端**（#8 用户/角色 + #9 数据权限 Controller + UI）— 让 admin 模块闭环
3. **客户/商机/合同前端**（#4 #5 #6）— 让核心业务流转可用
4. **修硬编码桩**（#6 ContractNodeService 1L/DEFAULT）
5. **跟进与日程**（#10 #11）— 日常操作入口
6. **通知中心 UI**（#16）— 站内信入口
7. **首次登录强制改密**（#17）— 安全合规
8. **中低优先级模块**（#12 返利 scheduler、#19 #20）— 按产品节奏逐步补齐
