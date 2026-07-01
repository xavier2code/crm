# 前端项目开发设计文档

## 1. 背景与范围

### 1.1 背景

本项目为前后端分离的 CRM 管理系统。后端已实现客户、商机、项目、合同、跟进、任务、返利、通知、后台管理等模块的 REST API。前端目前仅完成登录、路由、布局和占位页面，需要基于后端 API 和开发文档完成业务页面的开发。

### 1.2 范围

本阶段实现最小可用闭环：

- 工作台（Dashboard）
- 客户管理（Customer）
- 商机管理（Opportunity）
- 项目管理（Project）
- 合同管理（Contract）
- 后台管理（用户 / 角色 / 字典 / 单位）

二期功能（跟进、任务、返利、通知、日程）不在本次范围内。

## 2. 整体架构与技术栈

### 2.1 技术栈

| 层级 | 技术 |
|------|------|
| 构建工具 | Vite 8 + TypeScript 6 |
| UI 框架 | Ant Design 6 + `@ant-design/icons` |
| 路由 | React Router 7 `createBrowserRouter` |
| 服务端状态 | TanStack Query 5 |
| 全局状态 | Zustand 5 |
| 类型生成 | `openapi-typescript` |
| 表单校验 | Zod |
| HTTP 客户端 | Axios |

### 2.2 目录结构

```
frontend/src/
├── api/                    # 按模块封装的原始 API 函数
│   ├── client.ts
│   ├── auth.ts
│   ├── customer.ts
│   ├── opportunity.ts
│   ├── project.ts
│   ├── contract.ts
│   ├── dashboard.ts
│   └── admin/
│       ├── user.ts
│       ├── role.ts
│       ├── dictionary.ts
│       └── unit.ts
├── hooks/                  # TanStack Query hooks
│   ├── useCustomers.ts
│   ├── useCreateCustomer.ts
│   └── ...
├── components/             # 通用组件
│   ├── DataTable.tsx
│   ├── SearchForm.tsx
│   ├── DictSelect.tsx
│   ├── DictTag.tsx
│   ├── PageHeader.tsx
│   ├── AuthButton.tsx
│   ├── AmountInput.tsx
│   └── FormFooter.tsx
├── stores/                 # Zustand stores
│   ├── auth.ts
│   ├── dict.ts
│   └── menu.ts
├── schemas/                # Zod 校验 schema
│   ├── customer.ts
│   ├── opportunity.ts
│   └── ...
├── types/                  # 生成的 OpenAPI 类型 + 自定义类型
│   ├── api.d.ts
│   └── app.ts
├── utils/                  # 工具函数
│   ├── format.ts
│   └── permission.ts
├── layouts/
│   ├── BasicLayout.tsx
│   └── BlankLayout.tsx
├── pages/                  # 业务页面
│   ├── login/
│   ├── dashboard/
│   ├── customer/
│   │   ├── index.tsx
│   │   ├── create.tsx
│   │   ├── edit.tsx
│   │   └── detail.tsx
│   ├── opportunity/
│   ├── project/
│   ├── contract/
│   └── system/
│       ├── users/
│       ├── roles/
│       ├── dictionaries/
│       └── units/
└── router/
    └── index.tsx
```

### 2.3 实施顺序

采用水平切片方式，先完成共享基础设施，再实现业务页面：

1. 类型层：配置 OpenAPI 生成脚本。
2. API 层：按模块封装所有原始 API 函数。
3. 状态层：TanStack Query hooks + Zustand 字典/菜单 store。
4. 组件层：通用表格、表单、字典选择、权限按钮等。
5. 布局层：动态菜单渲染、权限路由守卫、面包屑集成。
6. 页面层：按模块实现列表 / 新增 / 编辑 / 详情页面。
7. 打磨：统一错误提示、空状态、加载状态、表单校验。

## 3. 共享基础设施

### 3.1 OpenAPI 类型生成

在 `package.json` 增加脚本：

```json
"gen:api": "openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.d.ts"
```

- 每次后端接口变化后执行 `npm run gen:api`。
- 生成文件不手动修改。
- 后端需处于运行状态才能生成。

### 3.2 API 层

每个模块一个文件，导出原始请求函数。示例：

```ts
// src/api/customer.ts
import { request } from './client'
import type { paths } from '@/types/api'

export const getCustomers = (params: paths['/api/customers']['get']['parameters']['query']) =>
  request<paths['/api/customers']['get']['responses']['200']['content']['*/*']['data']>({
    url: '/customers',
    method: 'GET',
    params,
  })

export const createCustomer = (body: paths['/api/customers']['post']['requestBody']['content']['application/json']) =>
  request<paths['/api/customers']['post']['responses']['200']['content']['*/*']['data']>({
    url: '/customers',
    method: 'POST',
    data: body,
  })
```

### 3.3 TanStack Query Hooks

按模块/query/mutation 组织：

```ts
// src/hooks/useCustomers.ts
export const useCustomers = (params) =>
  useQuery({ queryKey: ['customers', params], queryFn: () => getCustomers(params) })

export const useCreateCustomer = () =>
  useMutation({ mutationFn: createCustomer })
```

约定：

- QueryKey 按 `[entity, action, params]` 组织。
- 创建/更新成功后调用 `queryClient.invalidateQueries({ queryKey: ['customers'] })` 刷新列表。

### 3.4 全局错误处理

在 `src/api/client.ts` 统一处理：

- `code === 0`：正常返回 `response.data.data`。
- `code === 2001 / 2003`：清除 token，跳转 `/login`。
- `code === 2007`：跳转修改密码页。
- `code 4xxx / 5xxx / 6xxx`：使用 `Modal.error` 展示 `message + data.hint`。
- `code 5002`（乐观锁）：提示“数据已被修改，请刷新”。
- 其他业务错误：`message.error(message)`。
- 网络错误：`message.error('网络错误，请稍后重试')`。

### 3.5 动态菜单与权限

- 登录成功后，把 `menuTree` 存入 `menuStore`，`BasicLayout` 据此渲染侧边栏。
- 路由守卫在加载路由时检查 `permissionCodes`，无权限则跳转 `/403`。
- `AuthButton` 组件接收 `code`，根据 `permissionCodes` 控制按钮显隐/禁用。
- 菜单图标通过 `menu.icon` 字段映射到 `@ant-design/icons`。

### 3.6 字典缓存

- 应用启动时（或登录后）批量拉取 `GET /api/admin/dictionaries`，按 `type` 存入 `dictStore`。
- `DictSelect` 和 `DictTag` 从 store 读取。
- 字典变更后刷新页面或重新登录生效。

## 4. 通用组件

### 4.1 DataTable<T>

封装 Ant Design `Table + Pagination`：

- 默认分页：`current=1`，`pageSize=20`，`showSizeChanger`，`showTotal`。
- 将 AntD 的 `current/pageSize` 转换为后端参数 `current/size`。
- 加载状态由 TanStack Query 的 `isLoading` 注入。

### 4.2 SearchForm

基于 AntD `Form` 的可折叠搜索区：

- 每个表单项通过配置数组渲染。
- 支持 `onSearch`、`onReset`。
- 搜索时触发父组件更新 `searchParams`。

### 4.3 DictSelect / DictTag

```tsx
<DictSelect type="police_type" value={value} onChange={setValue} />
<DictTag type="opportunity_status" value={status} />
```

- 从 `dictStore` 读取对应 `type` 的字典列表。
- `DictTag` 按类型维护颜色映射。

### 4.4 PageHeader

页面顶部标题 + 面包屑 + 右侧操作区。

### 4.5 AuthButton

```tsx
<AuthButton code="customer:create" type="primary">新增客户</AuthButton>
```

- 无权限时自动隐藏或禁用。

### 4.6 AmountInput / DatePickerField

- `AmountInput`：数字输入，单位“万元”，保留两位小数。
- `DatePickerField`：统一 `YYYY-MM-DD` 格式。
- `WeekPickerField`：用于项目“预计签约周”。

### 4.7 FormFooter

表单页底部固定操作栏：提交、重置、取消。

## 5. 业务模块页面设计

### 5.1 工作台（/dashboard）

- 调用 `GET /api/dashboard/my`。
- 展示：渠道名称、客户总数、商机总数、生效商机数、项目总数、进行中项目数、合同总额、待办任务数、今日跟进数。
- 用 `Card + Statistic` 做数据卡片，下方用 `Table` 展示最近跟进和到期项目。

### 5.2 客户管理

路由：

- `/customer` — 列表
- `/customer/create` — 新增
- `/customer/:id` — 详情
- `/customer/:id/edit` — 编辑

列表页：

- 搜索：关键字、警种、区域、业务领域、客户分层。
- 表格列：客户名称、单位、警种、分层、负责人、区域、操作（查看/编辑/分配）。

新增/编辑页：

- 选择单位（下拉从 `/api/admin/units/all`）。
- 警种（`DictSelect`）、客户分层 A/B/C。
- 联系人列表：至少一个，字段包括姓名、职务、手机号、联系人类型、是否主联系人。
- Zod 校验：单位必填、警种必填、至少一个联系人且手机号合法。

详情页：

- 只读展示客户基本信息和联系人列表。
- 提供“编辑”入口。

### 5.3 商机管理

路由：

- `/opportunity` — 列表
- `/opportunity/create` — 新增
- `/opportunity/:id` — 详情
- `/opportunity/:id/edit` — 编辑

列表页：

- 搜索：状态、业务领域、项目类型、关键字。
- 表格列：客户、业务领域、项目类型、预估金额、状态、最近跟进、有效/到期/冷却时间、操作。
- 操作：提交审批、编辑、查看审批记录。

新增/编辑页：

- 选择客户（带搜索）、业务领域、项目类型（新签/续签/试用）、预估金额（万元）。
- Zod 校验：客户必填、金额非负。

详情页：

- 展示商机详情 + 审批记录时间线。
- CYBD 角色显示“通过/驳回”操作。

### 5.4 项目管理

路由：

- `/project` — 列表
- `/project/create` — 新增
- `/project/:id` — 详情
- `/project/:id/edit` — 编辑

列表页：

- 搜索：状态、P 节点、stage_6、业务领域、区域。
- 表格列：项目名称、业务领域、金额、业绩数、销售方式、负责人 BD、预计签约日期、状态、P 节点、stage_6。

新增/编辑页：

- 从商机转化或手动创建；字段：名称、业务领域、产品类别、金额、业绩数、销售方式、负责人 BD、销售负责人、预计签约周、客户分层。
- 只读字段（灰色）：由跟进记录自动更新。

详情页：

- 顶部基本信息。
- P 节点进度条 + stage_6 进度条。
- 9 个里程碑勾选。
- 双精评分 8 维度输入 + 自动计算加权总分。
- 招投标节点表单、合同节点表单、付款节点列表。

### 5.5 合同管理

路由：

- `/contract` — 列表
- `/contract/create` — 新增
- `/contract/:id` — 详情/编辑

列表页：

- 搜索：状态、项目。
- 表格列：项目、金额、状态、创建时间、操作。
- 金额对 BD 角色脱敏显示 `**万`。

新增/编辑页：

- 选择项目、填写金额、选择状态。

### 5.6 后台管理

路由统一在 `/system/*`：

- `/system/users`：用户列表、新增/编辑抽屉、分配角色、重置密码。
- `/system/roles`：角色列表、新增/编辑、查看权限码。
- `/system/dictionaries`：字典类型分组展示，支持按 type 筛选、CRUD。
- `/system/units`：单位列表、新增/编辑、启用/禁用。

后台管理路由统一由后端返回的 `permissionCodes` 控制；由于后端 `/api/admin/**` 已限制为 ADMIN 角色，前端按钮级权限同样基于 `permissionCodes` 做显隐/禁用。

## 6. 数据流

### 6.1 登录与初始化

1. 用户在 `/login` 提交账号密码 + 验证码。
2. 登录成功后：
   - `authStore` 保存 `token`、`user`、`roles`、`permissionCodes`。
   - `menuStore` 保存 `menuTree`。
   - `dictStore` 批量拉取字典并缓存。
3. `BasicLayout` 从 store 渲染侧边栏和用户信息。

### 6.2 列表页数据流

1. 页面初始化：从 URL query 或默认值构建 `searchParams`。
2. `useCustomers(searchParams)` 自动发起请求。
3. `DataTable` 接收 `data` 和 `isLoading`，渲染表格和分页。
4. 用户点击搜索/重置/分页：更新 `searchParams`，TanStack Query 自动重新请求。

### 6.3 表单页数据流

1. 新增页：用 Zod schema 初始化空表单。
2. 编辑页：通过 `useCustomer(id)` 预填表单。
3. 提交前做 Zod 校验，调用 mutation hook。
4. 成功后 `message.success` 并跳转回列表，同时失效相关 query key。
5. 失败由 Axios 拦截器统一提示。

### 6.4 权限数据流

1. 登录返回 `permissionCodes` 存入 `authStore`。
2. 路由守卫检查当前路由所需权限码。
3. 按钮级权限通过 `AuthButton` 检查。
4. 后端同时做最终权限校验。

## 7. 错误处理与边界情况

### 7.1 错误码映射

| 错误码 | 场景 | 前端行为 |
|--------|------|----------|
| `0` | 成功 | 正常返回数据 |
| `1001` | 参数校验失败 | 表单字段高亮并显示字段错误 |
| `1002` | 资源不存在 | `message.error`，返回上一页 |
| `2001/2003` | 未登录/Token 过期 | 清除登录态，跳转 `/login` |
| `2002` | 账号密码错误 | `message.error` |
| `2004` | 无权限 | 跳转 `/403` 或隐藏按钮 |
| `2007` | 首次登录强制改密 | 跳转 `/change-password` |
| `3003` | 客户已存在但非本人 | Modal 提示并引导申请分配 |
| `5002` | 乐观锁冲突 | Modal 提示“数据已被修改，请刷新” |
| `4xxx/5xxx/6xxx` | 业务冲突 | Modal.error 展示 `message + data.hint` |

### 7.2 其他边界

- 表格空数据：显示 `Empty` 组件。
- 编辑页加载：显示 `Spin`。
- 提交按钮：使用 mutation 的 `isPending` 防止重复提交。
- 删除/分配操作：使用 `Modal.confirm` 二次确认。
- 无权限菜单：不显示，直接访问跳转 `/403`。

## 8. 测试策略

### 8.1 测试范围

- 本阶段以端到端手工验证为主。
- 共享层完成后可补充：
  - Zod schema 单元测试。
  - 权限工具函数测试。
  - 格式化工具函数测试。

### 8.2 端到端验证清单

每个模块完成后验证：

1. 列表：搜索、分页、排序、空状态。
2. 新增：表单校验、提交成功、列表刷新。
3. 编辑：数据回填、提交成功。
4. 详情：只读展示正确。
5. 删除/状态变更：二次确认、成功后刷新。
6. 权限：无权限用户看不到菜单/按钮。

### 8.3 类型与代码检查

- `npm run build` 执行 `tsc -b`。
- 提交前运行 `npm run lint` 和 `npm run build`。

## 9. 风险与依赖

- **后端需启动**才能生成 OpenAPI 类型；如后端 API 文档不全，需手动补充类型。
- **开发文档与后端实现存在差异**，实现时以实际后端 API 为准。
- **项目详情页较复杂**，是本阶段最大开发量，需重点关注。
