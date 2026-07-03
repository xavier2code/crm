# CRM 渠道版 - 未完成功能清单

> 更新时间：2026-07-03
> 基于 `CRM-渠道版-开发文档.md` 与当前 `main` 分支（`b829764`）代码扫描结果
> 标注图例：[x] 已完成 · [~] 部分完成 · [ ] 未开始

---

## 高优先级（核心业务流程，建议优先补齐）

### 1. 项目列表页
- [x] 实现 `frontend/src/pages/project/index.tsx`（306 行，已实现）
- [x] 项目列表：搜索、筛选、分页
- [x] 状态筛选：项目中 / 项目中断 / 项目完成 / 项目终止
- [x] P 级节点筛选
- [x] 新建项目入口（商机转项目）
- [x] 列表跳转详情页

### 2. 项目详情路由
- [x] 在 `frontend/src/router/index.tsx` 注册 `project/:id` 路由指向 `pages/project/detail/index.tsx`（719 行已实现；`path="project"` 已正确指向 306 行列表页）

### 3. 项目过程聚合接口
- [x] 后端新增 `GET /api/projects/{id}/process` 或前端直接组合现有端点
- [x] 当前 `ProjectController` 已有 P 节点/招投标/合同/回款/双精评分等分散端点，缺少统一聚合

### 4. 客户管理前端
- [x] 客户列表页 `frontend/src/pages/customer/index.tsx`（搜索、分页、分配）
- [x] 新增客户页 `frontend/src/pages/customer/create/index.tsx`
- [x] 客户详情页 `frontend/src/pages/customer/detail/index.tsx`
- [x] 编辑客户页 `frontend/src/pages/customer/edit/index.tsx`
- [x] 联系人维护弹窗/页面（决策人/对接人/操作员）
- [x] 单位选择、警种下拉、手机号脱敏展示
- [x] 后端 `CustomerController` 10 个端点已完整（`/api/customers`）

### 5. 商机报备前端
- [x] 我的报备/全部商机列表页 `frontend/src/pages/opportunity/index.tsx`
- [x] 新建/编辑报备表单
- [x] 报备审批列表与操作（通过/驳回/审批意见）
- [x] 报备状态流转展示（草稿/审批中/生效中/失败/失效）
- [x] 报备保护提示（同客户+同业务域唯一）
- [x] 商机转项目按钮（详情页 + 列表页跳转 `/project?createFromOpportunityId=X`）
- [x] 后端 `OpportunityController` 7 个端点已完整（含 `submit`/`approve`）
- 提交：`fa57d26` feat(opportunity): implement opportunity list, form, approval and convert-to-project
- 合并：`200ae13` Merge branch 'feat/opportunity-frontend'

### 6. 合同管理
- [x] 后端 `ContractController`（6 端点：分页/详情/创建/更新/状态变更/删除）已完整
- [x] 前端 `api/contract.ts` API 客户端已就位
- [x] 前端合同列表页 `frontend/src/pages/contract/index.tsx`（搜索、筛选、分页、新增、状态变更、删除）
- [x] 合同详情/状态变更页 `frontend/src/pages/contract/detail/index.tsx`
- [x] 合同新建/编辑页 `frontend/src/pages/contract/create/index.tsx`、`frontend/src/pages/contract/edit/index.tsx`
- [x] 后端修复 `ContractNodeService.getChannelIdFromProject` 硬编码桩（`return 1L`）
- [x] 后端修复 `ContractNodeService.getProductCategoryFromProject` 硬编码桩（`return "DEFAULT"`）
- [x] 后端修复 `ContractService` 中同名硬编码桩
- [x] 合同与项目关联选择
- 提交：`e4fa99e` feat(contract): implement contract management list/detail/status pages and fix stubs

### 7. 单位/渠道分配（业务侧 4 级链路）
- [x] 后端 `module/unit/` 业务侧模块：`UnitAssignmentService` + controller + mapper（`fe9237d`）
- [x] Flyway V20：`t_unit_assignment` 表 + `UNIT_ASSIGN` 菜单 + `unit:assign` 操作码 + 5 角色授权
- [x] 4 级分配链路：大区总→BD（BD 范围）、大区总→渠道负责人 / BD→渠道（沿用 t_user_channel 1/2）、渠道负责人→渠道 BD（CHANNEL_BD 范围，仅 head 可操作）
- [x] 渠道 BD 角色：自动按 userId=self 过滤；CHANNEL_BD 范围操作用户必须为 head（强制校验）
- [x] 12 个单测覆盖：范围校验、渠道权限、去重、撤销、CHANNEL_BD 必传 channelId、BD 范围拒绝传 channelId、Self-only 过滤
- [x] `ChannelController` 补齐：`GET/POST/PUT/DELETE /api/admin/channels` + assignments 6 端点（前端 system/channel 此前因缺 controller 全部 404）
- [x] `UserChannelService.countByChannel` 新增（删除渠道时校验）
- [x] 前端业务侧入口 `pages/business/units/index.tsx`（400+ 行）：分配总览 + 新建分配（按范围/渠道动态字段）+ 撤销
- [x] 前端路由 `/business/units` 已注册；菜单由后端 seed 提供
- 提交：`ae1fd33`（merge `4336a7e` 已 push origin/main）
- 后续：业务域 / 警种维度二级过滤继续沿用 DataScope 内存字段（SQL 拦截器未动）

### 8. 系统管理 - 用户/角色
- [x] 用户列表页 `frontend/src/pages/system/users/index.tsx`（搜索/分页/启停用/重置密码/数据权限）
- [x] 角色列表页 `frontend/src/pages/system/roles/index.tsx`（内置角色保护 + 操作编码勾选）
- [x] 角色权限配置：操作编码勾选（菜单树待后端补 `MenuController`，见 #9 续）
- [x] 修复 `UserController.create` 中 `operatorId=1L` 硬编码，改读 `SecurityContext.getCurrentUserId()`
- [x] `UserController` 补齐 `POST /{id}/reset-password` 与 `PUT /{id}/status` 端点
- 提交：`498b750` 数据权限维度对齐 + `ea6ea8b` 系统管理前端（merge `ce50e9e` 已 push origin/main）

### 9. 数据权限配置
- [x] 新增 `DataScopeDimension` 枚举作为 SSOT：`ALL / CHANNEL / REGION / UNIT / BUSINESS_DOMAIN / POLICE_TYPE / SELF`
- [x] `V16 Flyway`：`t_data_permission.scope_type` 与 `t_role.data_scope_type` 由 SMALLINT 改 VARCHAR(32)，按 V2 字典翻译历史值
- [x] `DataScope` 重写：`fromPermissions(List)` 静态构造；未知 scopeType 永不静默降级 ALL
- [x] `AuthService.buildDataScope` 改用 `DataScope.fromPermissions`，根除 1=业务域/1=ALL、4=警种/4=UNIT 歧义
- [x] 新增 `DataPermissionController`：`GET / PUT /api/admin/users/{userId}/data-permissions`
- [x] 用户管理页"数据权限"弹窗（7 个维度逐项保存，ALL/SELF 无值，其他多值换行/逗号分隔）
- [x] 前端 `DATA_SCOPE_DIMENSIONS` 字符串 code 替代数字 `SCOPE_TYPE`
- [x] `RoleService` 补 `menuIds` / `operationCodes` 在 `t_role_menu` / `t_role_operation` 的整组覆盖
- [x] 单测：`DataScopeTest`（16 用例） + `DataPermissionServiceTest`（7 用例） + `DataScopeIntegrationTest` 适配
- 提交：`498b750` + `ea6ea8b`（merge `ce50e9e` 已 push origin/main）
- 后续：业务域 / 警种维度已存到内存 `DataScope.businessDomainCodes / policeTypeCodes`，SQL 不直接生效；需要 service 层白名单二次过滤时复用该字段

### 10. 跟进记录 / 日程
- [x] 跟进记录页 `/followup`（搜索/分页/新建/编辑/删除）
- [x] 阶段反馈必填校验（`currentStage` 与 `nextStage` 不同时 `stageFeedback` 必填）
- [x] 客户跟进历史：`pageFollowups({ customerId })` 透传给后端
- [x] V17 菜单：`FOLLOW_UP` 授权 管理员/渠道负责人/渠道BD/CYBD
- [x] 操作权限：followup:manage / followup:view
- 提交：`59282ff`（merge `1dc1a2a` 已 push origin/main）
- 后续：关键词搜索（后端 `pageFollowups` 需补 `keyword` 参数）；任务"按时填写跟进自动消除闭环"业务规则由后端 scheduler 实现（暂未做）

---

## 中优先级（重要支撑模块）

### 11. 任务管理
- [x] 任务管理页 `/task`（今日待办/待完成/已完成/已关闭 4 个 Tab）
- [x] 今日任务专门接口 `GET /api/tasks/today`，逾期高亮
- [x] 完成任务 `POST /{id}/complete`
- [x] 关闭任务强制要求原因（前端校验 + 弹窗）
- [x] V17 菜单：`TASK` 授权 管理员/渠道负责人/渠道BD/CYBD/大区总（只读）
- 提交：`59282ff`（merge `1dc1a2a` 已 push origin/main）

### 12. 返利管理
- [x] 后端 `RebateController`（6 端点）+ `RebateRateController`（5 端点）已完整
- [x] 前端 `pages/rebate/index.tsx`（469 行）+ `pages/rebate-rates/index.tsx`（260 行）
- [x] 路由 `/business/rebate`、`/business/rebate/rates` 已注册
- [x] V15 seed 加了"返利率配置"菜单（仅 CYBD）
- [x] 后端**已实现返利自动生成 scheduler**（业绩完成返利/回款返利/服务返利）
- [x] scheduler 中 30 天未跟进 → 报备失效已存在（`OpportunityExpiryScheduler`）且返利侧已补定时任务（`RebateGenerationScheduler`）

### 13. 个人工作台
- [x] 前端 `pages/dashboard/index.tsx`（110 行）已接 `useDashboard` 真实 API（`a35f4eb` 提交）
- [x] 渠道工作台入口按钮（CHANNEL_HEAD/CYBD 角色）

### 14. 渠道工作台
- [x] 前端 `pages/channel-dashboard/index.tsx`（262 行）— 4 大块：总览/业绩/成员/客户分布
- [x] 路由 `/dashboard/channel/:channelId`
- [x] 后端 `DashboardController.getChannelDashboard` + `ChannelDashboardVO`（含 @PreAuthorize）
- [x] V15 seed 对齐了工作台子菜单路径

---

## 低优先级（可延后）

### 15. 审计日志
- [x] 后端 `AuditLogController` 3 端点（`/api/admin/audit-logs`）
- [x] 前端 `pages/system/audit-log/index.tsx`（395 行）
- [x] 路由 `system/audit` 已注册（`ea6ea8b` 提交）
- 附加：路由 `system/channel` 也已注册（页面 582 行 + ChannelController/V15 seed 已就位，#7 中"渠道管理页"前置补齐）

### 16. 通知中心
- [x] 顶部 Bell 角标 + Popover Top 5（BasicLayout NotificationBell 子组件）
- [x] 通知中心页 `/notifications`（全部/未读/已读 Tab + 标已读 + 查看跳转）
- [x] 全部已读（`POST /api/notifications/read-all`）
- [x] 30s 轮询 + window focus 刷新（hooks/useNotifications.ts useUnreadCount）
- [x] V18 菜单：`NOTIFICATION_CENTER` 全部角色可见，`notification:view` 操作权限
- [x] 48h 报备审批超时 scheduler（`OpportunityApprovalReminderScheduler`，每整点）
- [x] 30 天报备失效 scheduler（`OpportunityExpiryScheduler`，每天 02:00）
- 提交：`7358ca6`（merge `75c32a6` 已 push origin/main）

### 17. 认证登录优化
- [x] 首次登录强制改密：`AuthService.login` 检测 `is_initial_password=1` → 抛 2007
- [x] 90 天强制过期策略：`t_user.password_changed_at`（V19） + `AuthService.login` 检测 `now - changed_at >= 90` → 抛 2007
- [x] 改密清除标志：`AuthService.changePassword` 末尾设 `is_initial_password=0` + 更新 `passwordChangedAt`
- [x] 前端 `/change-password` 路由注册（页面 100+ 行已存在但路由缺失 → 修）
- [x] 前端 client 拦截器 2007 → `/change-password`（已存在）
- [x] 登录响应 `userInfo.departmentId/departmentName` 填充：新增 `t_department`（V23）+ `User.department_id` + `AuthService.login` 填充
- [x] 密码强度校验（`AuthService.changePassword` 接入 `PasswordPolicyService.validateStrength`）：默认 8 位以上且含大/小写/数字/特殊字符，错误码 2009
- 提交：`bd8962a`（merge `c61cc59` 已 push origin/main）、`2356e91`（merge `feat/auth-login-optimization`）

### 18. 商机报备优化
- [x] 48h 钉钉催办 scheduler（`OpportunityApprovalReminderScheduler`）— 站内信形式实现
- [x] 独立 re-submit 端点 `POST /api/opportunities/{id}/resubmit`：`submit` 收紧为草稿→审批中，resubmit 处理 FAILED/EXPIRED 重提（`OpportunityController`）
- [x] 报备锁定 1 个月：`OpportunityService.resubmitOpportunity` 校验 `submit_count < 2` + `cooling_until` 未到期（错误码 4002/4004），重新启用后清空 `cooling_until`；`OpportunityExpiryScheduler` 在 submit_count ≥ 2 时置 `cooling_until = +1m`
- [x] 前端 `resubmitOpportunity` API + `useResubmitOpportunity` hook + 详情页/列表页"重提"按钮 + `OpportunityVO.resubmittable` 字段；冷却期展示 Tag
- 后续：见 #21 已知代码问题 — `ProjectService:453` 项目完成时报备状态未实际更新

### 19. 销售分配梯队
- [x] 评估是否需要实现：**需要**。业务文档 §8 已确认该功能，渠道工作台的销售分配需要按"梯队 + 区域"过滤客户/单位，且管理员后台可配置。
- [x] 实现 `t_sales_team_config` 表及后端 CRUD（Flyway V24 + module/sales_team）
- [x] 后端测试通过（11 用例）

### 20. 报销管理
- [x] 确认保留（差旅/招待/单级审批/关联项目/可上传附件）
- [x] 后端 V22：`t_reimbursement` + `t_reimbursement_attachment` + `reimbursement_type` 字典（TRAVEL/ENTERTAIN）+ 菜单 + 操作码 + 6 角色授权
- [x] `module/reimbursement` 完整模块：Entity/Mapper/Converter/Service/2 Controller
- [x] 生命周期 DRAFT→PENDING→APPROVED/REJECTED→PAID；申请人 IDOR 校验（6015）；状态机守卫（6011/6012/6014）
- [x] 单级审批（CHANNEL_HEAD/ADMIN 走 `reimbursement:approve`）；财务/管理员 走 `reimbursement:pay` 标记已付款
- [x] 关联项目必填；提交后项目名称快照防改名失真
- [x] 附件上传/下载/删除：白名单 MIME + 20MB 上限（前端拦截 + 后端 `FileStorageService` 二次校验）
- [x] 错误码段 6010-6018 复用现有 6xxx 段（不新开 7xxx）
- [x] 业务基础：`FileStorageService` + `FileStorageProperties`（`base-path: ${FILE_STORAGE_PATH:./data/uploads}`）
- [x] `RoleConstants.FINANCE` 补齐（V1 seed 已有角色但常量类未声明）
- [x] 前端 `pages/reimbursement/index.tsx` 重写 5 行占位 → 594 行：6 个 Tab（待审批/已审批/已付款/已驳回/我的草稿/全部）+ 详情 Drawer + 申请 Modal + 审批 Modal + 附件上传/下载/删除
- [x] 前端 `api/reimbursement.ts` 类型化 API 客户端（10 个端点 + 类型/状态/类型映射）
- [x] 12 个单测覆盖：生命周期/IDOR/状态机/附件守卫；167/167 后端测试通过；前端 lint/build 零警告
- 提交：`303bdc8`（merge `6025b64` 已 push origin/main）
- 后续：业务域/警种应用层白名单（#9 续）未做（与本任务解耦）

---

## 已知代码问题（代码扫描发现，需后续修复）

### 21. 后端遗留 TODO 与死代码
- [ ] `ProjectService.java:400` — 状态变更后审计日志未写入（`// TODO: 写入审计日志`）
- [ ] `ProjectService.java:453` — 项目完成时报备状态未实际更新（`// TODO: 需要确认报备的"已转化"状态值`），仅 log.info，opportunity 状态未改
- [ ] `PasswordPolicyService.java:141` — `isPasswordExpired()` 永远返回 false（`// TODO: 从密码历史表获取...`），实际 90 天过期逻辑已在 `AuthService.login` 中通过 `passwordChangedAt` 实现，此方法为死代码
- [ ] 前端占位页面：`pages/system/units/index.tsx`（9 行占位，管理侧单位管理）、`pages/business/index.tsx`（9 行占位，业务域落地页）
- [ ] Flyway 迁移跳号：V11 → V13（无 V12 文件），可能为已删除的回滚脚本，建议确认

## 当前进度概览（2026-07-03 `b829764` 重新盘点）

| 类别 | 完成 | 部分 | 未开始 |
|---|---|---|---|
| 高优先级 10 项 | 10 | 0 | 0 |
| 中优先级 4 项 | 4 | 0 | 0 |
| 低优先级 6 项 | 5 | 0 | 1 |
| **合计 20 项** | **19** | **0** | **1** |

**最近合并的相关 commit**：
- `b829764` docs(todo): mark #17 auth login optimization as completed
- `2356e91` Merge branch 'feat/auth-login-optimization'
- `c09d563` feat(auth): fill department info in login response and enforce password strength
- `58a2518` refactor(audit): extract async audit log save to AuditLogWriter bean
- `6025b64` Merge branch 'feat/reimbursement'
- `303bdc8` feat(reimbursement): implement reimbursement management module (TODO #20)
- `4e9f10e` Merge branch 'feat/rebate-scheduler'
- `649ba6d` feat(rebate): implement auto-generation scheduler for performance/payment/service rebates
- `439012e` Merge branch 'feat/contract-management'
- `e4fa99e` feat(contract): implement contract management list/detail/status pages and fix stubs
- `cb638bc` docs(todo): mark #17 first-login + 90-day expiry as completed
- `4336a7e` Merge branch 'feat/unit-channel-assignment'
- `ae1fd33` feat(unit): add business-side unit assignment 4-level chain
- `c61cc59` Merge branch 'feat/initial-password-rotation'
- `bd8962a` feat(auth): force password change on first login and 90-day expiry
- `75c32a6` Merge branch 'feat/notification-center-frontend'
- `7358ca6` feat(notification): add notification center UI and header bell badge
- `1dc1a2a` Merge branch 'feat/followup-task-frontend'
- `59282ff` feat(followup-task): add follow-up and task management UI
- `ce50e9e` Merge branch 'feat/data-scope-align'
- `ea6ea8b` feat(system): users/roles management UI and audit/channel routing
- `498b750` feat(permission): align data scope dimensions and unblock admin module
- `7920171` feat(project): add list page, filters, /process aggregator
- `a35f4eb` feat(dashboard): add channel dashboard and project statistics
- `984279d` feat(project): add detail page with process management and scoring
- `c0d0d9c` feat(audit): add audit log query page
- `82440af` feat(auth): add change password page
- `84cd144` feat(router): add channel dashboard, rebate, and rebate-rates routes
- `50166df` feat(permission): add user data permission management（已被 498b750 取代为 SSOT 枚举）
- `77f0f04` feat(channel): add channel management backend and frontend
- `8c7705f` chore(db): add audit columns and align menu paths（V15 seed）

---

## 建议执行顺序

1. **业务域 / 警种应用层白名单过滤**（#9 续）— 让 DataScope.businessDomainCodes / policeTypeCodes 真正生效
2. **角色菜单树权限**（#8 续）— 需后端补 `MenuController`（当前不存在，前端角色页只能配操作编码）
3. **已知代码问题修复**（#21）— ProjectService 状态联动 + 审计日志、PasswordPolicyService 死代码清理、前端占位页面补齐
4. **中低优先级**（#19 销售梯队）— 按产品节奏逐步补齐

> 已完成：#6 合同管理、#7 单位/渠道分配、#8 系统管理前端、#9 数据权限维度对齐、#10 跟进、#11 任务、#12 返利管理（含 scheduler）、#13 个人工作台、#14 渠道工作台、#15 审计路由、#16 通知中心、#17 首次登录强制改密 + 90 天过期 + 密码强度校验、#20 报销管理
