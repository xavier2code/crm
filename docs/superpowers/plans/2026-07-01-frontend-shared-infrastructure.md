# 前端共享基础设施实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立前端共享基础设施，包括 OpenAPI 类型生成、统一 API 客户端、按模块封装的 API 函数、TanStack Query Hooks、Zustand 状态管理、通用 UI 组件、动态菜单布局和权限路由守卫，为后续业务页面开发提供基础。

**Architecture:** 采用水平切片方式，先统一建设类型/API/状态/组件等共享层；API 层调用后端 `/api` 接口并统一处理错误；UI 状态使用 TanStack Query 管理服务端状态，Zustand 管理全局客户端状态（认证/菜单/字典）；通用组件基于原生 Ant Design 封装。

**Tech Stack:** Vite 8 + React 19 + TypeScript 6 + Ant Design 6 + TanStack Query 5 + Zustand 5 + Zod + openapi-typescript

## Global Constraints

- 使用原生 Ant Design 组件，不使用 `@ant-design/pro-components`。
- API 统一响应结构为 `{ code, message, data, traceId, timestamp }`，`code === 0` 表示成功。
- 路由采用 `React Router 7 createBrowserRouter`。
- 路径别名 `@/` 映射到 `src/`。
- 菜单从后端 `menuTree` 动态渲染，权限从 `permissionCodes` 控制。
- 表单校验使用 Zod。
- 类型从后端 `/v3/api-docs` 通过 `openapi-typescript` 生成。
- 提交前运行 `npm run lint` 和 `npm run build`，必须无错误。

---

## File Structure

本次计划新增/修改以下文件：

| 文件 | 职责 |
|------|------|
| `frontend/package.json` | 添加 `openapi-typescript` 依赖和 `gen:api` 脚本 |
| `frontend/src/types/api.d.ts` | OpenAPI 生成的类型（不手动修改） |
| `frontend/src/types/app.ts` | 自定义 App 类型 |
| `frontend/src/api/client.ts` | 增强 Axios 拦截器，统一错误处理 |
| `frontend/src/utils/permission.ts` | 权限判断工具函数 |
| `frontend/src/utils/format.ts` | 金额、日期格式化工具 |
| `frontend/src/stores/menu.ts` | Zustand 动态菜单 store |
| `frontend/src/stores/dict.ts` | Zustand 字典缓存 store |
| `frontend/src/api/customer.ts` | 客户模块 API 函数 |
| `frontend/src/api/opportunity.ts` | 商机模块 API 函数 |
| `frontend/src/api/project.ts` | 项目模块 API 函数 |
| `frontend/src/api/contract.ts` | 合同模块 API 函数 |
| `frontend/src/api/dashboard.ts` | 工作台 API 函数 |
| `frontend/src/api/admin/*.ts` | 后台管理 API 函数 |
| `frontend/src/hooks/useCustomers.ts` | 客户模块 Query hooks |
| `frontend/src/hooks/useOpportunities.ts` | 商机模块 Query hooks |
| `frontend/src/hooks/useProjects.ts` | 项目模块 Query hooks |
| `frontend/src/hooks/useContracts.ts` | 合同模块 Query hooks |
| `frontend/src/hooks/useDashboard.ts` | 工作台 Query hooks |
| `frontend/src/hooks/useAdmin*.ts` | 后台管理 Query hooks |
| `frontend/src/components/DataTable.tsx` | 通用分页表格 |
| `frontend/src/components/SearchForm.tsx` | 通用搜索表单 |
| `frontend/src/components/DictSelect.tsx` | 字典下拉选择 |
| `frontend/src/components/DictTag.tsx` | 字典标签展示 |
| `frontend/src/components/PageHeader.tsx` | 页面标题 + 面包屑 |
| `frontend/src/components/AuthButton.tsx` | 权限按钮 |
| `frontend/src/components/AmountInput.tsx` | 金额输入（万元） |
| `frontend/src/components/DatePickerField.tsx` | 日期选择字段 |
| `frontend/src/components/WeekPickerField.tsx` | 周选择字段 |
| `frontend/src/components/FormFooter.tsx` | 表单底部操作栏 |
| `frontend/src/layouts/BasicLayout.tsx` | 动态菜单布局 |
| `frontend/src/layouts/BlankLayout.tsx` | 空白布局（登录页用） |
| `frontend/src/router/index.tsx` | 动态路由 + 权限守卫 |
| `frontend/src/pages/error/403.tsx` | 403 无权限页面 |

---

### Task 1: 安装 openapi-typescript 并配置类型生成脚本

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/types/app.ts`

**Interfaces:**
- Produces: `npm run gen:api` 命令，生成 `src/types/api.d.ts`。

- [ ] **Step 1: 安装依赖**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm install --save-dev openapi-typescript
```

- [ ] **Step 2: 在 package.json 中添加生成脚本**

修改 `frontend/package.json` 的 `scripts` 字段：

```json
"scripts": {
  "dev": "vite",
  "build": "tsc -b && vite build",
  "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
  "format": "prettier --write \"src/**/*.{ts,tsx,css,json}\"",
  "preview": "vite preview",
  "gen:api": "openapi-typescript http://localhost:8080/v3/api-docs -o src/types/api.d.ts"
}
```

- [ ] **Step 3: 创建自定义 App 类型文件**

创建 `frontend/src/types/app.ts`：

```ts
export interface MenuItem {
  id: number
  name: string
  path?: string
  icon?: string
  children?: MenuItem[]
  permission?: string
}

export interface CurrentUser {
  id: number
  username: string
  realName?: string
  phone?: string
  email?: string
  roles: string[]
}

export interface AuthState {
  token: string | null
  user: CurrentUser | null
  roles: string[]
  permissionCodes: string[]
}
```

- [ ] **Step 4: 生成 OpenAPI 类型**

确保后端已启动，然后执行：

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run gen:api
```

Expected: 生成 `frontend/src/types/api.d.ts`，无报错。

- [ ] **Step 5: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/types/app.ts frontend/src/types/api.d.ts
git commit -m "chore: setup openapi-typescript and generate API types"
```

---

### Task 2: 增强 API 客户端统一错误处理

**Files:**
- Modify: `frontend/src/api/client.ts`

**Interfaces:**
- Consumes: `ApiResult<T>` 结构。
- Produces: `request<T>` 函数，统一错误处理，401/403/2007 自动跳转。

- [ ] **Step 1: 用完整代码替换 client.ts**

将 `frontend/src/api/client.ts` 替换为：

```ts
import axios, { AxiosError, type AxiosInstance, type AxiosRequestConfig } from 'axios'
import { message, Modal } from 'antd'

export interface ApiResult<T> {
  code: number
  success: boolean
  message: string
  data: T
  traceId?: string
  timestamp?: string
}

const client: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('crm-token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (response) => {
    const data = response.data as ApiResult<unknown>
    if (data.code === 0) {
      return response
    }

    handleBusinessError(data)
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const messageText = error.response?.data?.message || error.message || '网络错误'
    const status = error.response?.status
    const data = error.response?.data

    if (status === 401) {
      localStorage.removeItem('crm-token')
      window.location.href = '/login'
    } else if (status === 403) {
      message.error('无权限访问该资源')
    } else if (data) {
      handleBusinessError(data)
    } else {
      message.error(messageText)
    }

    return Promise.reject(new Error(messageText))
  }
)

function handleBusinessError<T>(data: ApiResult<T>) {
  const code = data.code
  const msg = data.message || '请求失败'

  if (code === 2001 || code === 2003) {
    localStorage.removeItem('crm-token')
    window.location.href = '/login'
    return
  }

  if (code === 2007) {
    window.location.href = '/change-password'
    return
  }

  if (code === 5002) {
    Modal.warning({
      title: '数据已过期',
      content: '数据已被他人修改，请刷新后重试',
    })
    return
  }

  if ((code >= 4000 && code < 7000) || (code >= 1003 && code <= 1999)) {
    const hint = (data.data as { hint?: string })?.hint
    Modal.error({
      title: '操作失败',
      content: hint ? `${msg}（${hint}）` : msg,
    })
    return
  }

  message.error(msg)
}

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await client.request<ApiResult<T>>(config)
  return response.data.data
}

export default client
```

- [ ] **Step 2: 验证 lint 通过**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
```

Expected: 无 lint 错误。

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/client.ts
git commit -m "feat: enhance axios client with unified error handling"
```

---

### Task 3: 创建权限与格式化工具函数

**Files:**
- Create: `frontend/src/utils/permission.ts`
- Create: `frontend/src/utils/format.ts`

**Interfaces:**
- Produces: `hasPermission(codes, code): boolean`，`formatMoney(value)`，`formatDate(value)`。

- [ ] **Step 1: 创建权限工具**

创建 `frontend/src/utils/permission.ts`：

```ts
export function hasPermission(permissionCodes: string[] | undefined, code: string): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return permissionCodes.includes(code)
}

export function hasAnyPermission(permissionCodes: string[] | undefined, codes: string[]): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return codes.some((code) => permissionCodes.includes(code))
}

export function hasAllPermissions(permissionCodes: string[] | undefined, codes: string[]): boolean {
  if (!permissionCodes || permissionCodes.length === 0) return false
  return codes.every((code) => permissionCodes.includes(code))
}
```

- [ ] **Step 2: 创建格式化工具**

创建 `frontend/src/utils/format.ts`：

```ts
import dayjs from 'dayjs'

export function formatMoney(value: number | string | undefined): string {
  if (value === undefined || value === null || value === '') return '-'
  const num = typeof value === 'string' ? parseFloat(value) : value
  if (Number.isNaN(num)) return '-'
  return `${num.toFixed(2)} 万`
}

export function formatDate(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD')
}

export function formatDateTime(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-MM-DD HH:mm:ss')
}

export function formatWeek(value: string | Date | undefined): string {
  if (!value) return '-'
  return dayjs(value).format('YYYY-[W]WW')
}

export function maskMoney(value: number | string | undefined): string {
  if (value === undefined || value === null || value === '') return '-'
  return '** 万'
}
```

- [ ] **Step 3: 验证 lint 和构建**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
npm run build
```

Expected: 无错误。

- [ ] **Step 4: Commit**

```bash
git add frontend/src/utils/permission.ts frontend/src/utils/format.ts
git commit -m "feat: add permission and format utilities"
```

---

### Task 4: 扩展 Zustand Stores

**Files:**
- Modify: `frontend/src/stores/auth.ts`
- Create: `frontend/src/stores/menu.ts`
- Create: `frontend/src/stores/dict.ts`

**Interfaces:**
- Consumes: `/api/auth/login` response data, `/api/admin/dictionaries` response data。
- Produces: `useAuthStore`, `useMenuStore`, `useDictStore`。

- [ ] **Step 1: 扩展 auth store**

将 `frontend/src/stores/auth.ts` 替换为：

```ts
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

import type { CurrentUser } from '@/types/app'
import type { LoginResult } from '@/api/auth'

interface AuthState {
  token: string | null
  user: CurrentUser | null
  roles: string[]
  permissionCodes: string[]
  setAuth: (result: LoginResult) => void
  setUser: (user: CurrentUser) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      roles: [],
      permissionCodes: [],
      setAuth: (result) => {
        localStorage.setItem('crm-token', result.accessToken)
        set({
          token: result.accessToken,
          user: result.userInfo,
          roles: result.roles || [],
          permissionCodes: result.permissionCodes || [],
        })
      },
      setUser: (user) => set({ user }),
      clearAuth: () => {
        localStorage.removeItem('crm-token')
        set({ token: null, user: null, roles: [], permissionCodes: [] })
      },
    }),
    { name: 'crm-auth' }
  )
)
```

注意：`LoginResult` 需与 `src/api/auth.ts` 中的实际类型对齐。

- [ ] **Step 2: 创建 menu store**

创建 `frontend/src/stores/menu.ts`：

```ts
import { create } from 'zustand'

import type { MenuItem } from '@/types/app'

interface MenuState {
  menus: MenuItem[]
  setMenus: (menus: MenuItem[]) => void
  clearMenus: () => void
}

export const useMenuStore = create<MenuState>((set) => ({
  menus: [],
  setMenus: (menus) => set({ menus }),
  clearMenus: () => set({ menus: [] }),
}))
```

- [ ] **Step 3: 创建 dict store**

创建 `frontend/src/stores/dict.ts`：

```ts
import { create } from 'zustand'

export interface DictionaryItem {
  id: number
  type: string
  code: string
  name: string
  sort: number
  remark?: string
}

interface DictState {
  dicts: Record<string, DictionaryItem[]>
  setDicts: (dicts: Record<string, DictionaryItem[]>) => void
  getDict: (type: string) => DictionaryItem[]
  getDictName: (type: string, code: string | undefined) => string
  clearDicts: () => void
}

export const useDictStore = create<DictState>((set, get) => ({
  dicts: {},
  setDicts: (dicts) => set({ dicts }),
  getDict: (type) => get().dicts[type] || [],
  getDictName: (type, code) => {
    if (!code) return '-'
    const item = get().dicts[type]?.find((d) => d.code === code)
    return item?.name || code
  },
  clearDicts: () => set({ dicts: {} }),
}))
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/stores/auth.ts frontend/src/stores/menu.ts frontend/src/stores/dict.ts
git commit -m "feat: extend auth store and add menu/dict stores"
```

---

### Task 5: 更新登录流程以初始化菜单和字典

**Files:**
- Modify: `frontend/src/api/auth.ts`
- Modify: `frontend/src/pages/login/index.tsx`

**Interfaces:**
- Consumes: `useAuthStore.setAuth`, `useMenuStore.setMenus`, `useDictStore.setDicts`。
- Produces: 登录成功后自动加载菜单和字典。

- [ ] **Step 1: 更新 auth.ts 添加 login 返回类型和字典获取函数**

将 `frontend/src/api/auth.ts` 替换/扩展为：

```ts
import { request } from './client'
import type { paths } from '@/types/api'
import type { CurrentUser, MenuItem } from '@/types/app'
import type { DictionaryItem } from '@/stores/dict'

export interface LoginResult {
  accessToken: string
  refreshToken: string
  tokenType: string
  userInfo: CurrentUser
  roles: string[]
  menuTree: MenuItem[]
  permissionCodes: string[]
}

export interface CaptchaResponse {
  image: string
  uuid: string
}

export interface LoginRequest {
  username: string
  password: string
  captchaUuid: string
  captchaCode: string
}

export function getCaptcha() {
  return request<CaptchaResponse>({ url: '/auth/captcha', method: 'GET' })
}

export function login(data: LoginRequest) {
  return request<LoginResult>({ url: '/auth/login', method: 'POST', data })
}

export function logout() {
  return request<void>({ url: '/auth/logout', method: 'POST' })
}

export function fetchCurrentUser() {
  return request<CurrentUser>({ url: '/auth/currentUser', method: 'GET' })
}

export function fetchDictionaries() {
  return request<DictionaryItem[]>({ url: '/admin/dictionaries', method: 'GET' })
}
```

- [ ] **Step 2: 修改登录页初始化菜单和字典**

修改 `frontend/src/pages/login/index.tsx`，在登录成功后调用：

```ts
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useDictStore } from '@/stores/dict'
import { login, fetchDictionaries } from '@/api/auth'

// 在登录成功回调中：
const handleLogin = async (values: LoginRequest) => {
  const result = await login(values)
  useAuthStore.getState().setAuth(result)
  useMenuStore.getState().setMenus(result.menuTree || [])

  const dicts = await fetchDictionaries()
  const dictMap: Record<string, DictionaryItem[]> = {}
  dicts.forEach((item) => {
    if (!dictMap[item.type]) dictMap[item.type] = []
    dictMap[item.type].push(item)
  })
  useDictStore.getState().setDicts(dictMap)

  navigate('/dashboard')
}
```

注意：需根据当前 `login/index.tsx` 的实际结构做最小改动，不要破坏已有表单逻辑。

- [ ] **Step 3: 验证 lint 通过**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/api/auth.ts frontend/src/pages/login/index.tsx
git commit -m "feat: initialize menu and dict stores after login"
```

---

### Task 6: 创建业务 API 模块

**Files:**
- Create: `frontend/src/api/customer.ts`
- Create: `frontend/src/api/opportunity.ts`
- Create: `frontend/src/api/project.ts`
- Create: `frontend/src/api/contract.ts`
- Create: `frontend/src/api/dashboard.ts`
- Create: `frontend/src/api/admin/user.ts`
- Create: `frontend/src/api/admin/role.ts`
- Create: `frontend/src/api/admin/dictionary.ts`
- Create: `frontend/src/api/admin/unit.ts`

**Interfaces:**
- Produces: 各模块 API 函数，供 hooks 层调用。

- [ ] **Step 1: 创建 customer API**

创建 `frontend/src/api/customer.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type CustomerPage = paths['/api/customers']['get']['responses']['200']['content']['*/*']['data']
type CustomerVO = paths['/api/customers/{id}']['get']['responses']['200']['content']['*/*']['data']
type CustomerRequest = paths['/api/customers']['post']['requestBody']['content']['application/json']
type ContactRequest = paths['/api/customers/{id}/contacts']['post']['requestBody']['content']['application/json']

export function getCustomers(params: paths['/api/customers']['get']['parameters']['query']) {
  return request<CustomerPage>({ url: '/customers', method: 'GET', params })
}

export function getCustomer(id: number) {
  return request<CustomerVO>({ url: `/customers/${id}`, method: 'GET' })
}

export function createCustomer(data: CustomerRequest) {
  return request<CustomerVO>({ url: '/customers', method: 'POST', data })
}

export function updateCustomer(id: number, data: CustomerRequest) {
  return request<CustomerVO>({ url: `/customers/${id}`, method: 'PUT', data })
}

export function deleteCustomer(id: number) {
  return request<void>({ url: `/customers/${id}`, method: 'DELETE' })
}

export function assignCustomer(id: number, userId: number) {
  return request<void>({ url: `/customers/${id}/assign`, method: 'POST', params: { userId } })
}

export function addContact(customerId: number, data: ContactRequest) {
  return request<void>({ url: `/customers/${customerId}/contacts`, method: 'POST', data })
}

export function updateContact(id: number, data: ContactRequest) {
  return request<void>({ url: `/contacts/${id}`, method: 'PUT', data })
}

export function deleteContact(id: number) {
  return request<void>({ url: `/contacts/${id}`, method: 'DELETE' })
}
```

- [ ] **Step 2: 创建 opportunity API**

创建 `frontend/src/api/opportunity.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type OpportunityPage = paths['/api/opportunities']['get']['responses']['200']['content']['*/*']['data']
type OpportunityVO = paths['/api/opportunities/{id}']['get']['responses']['200']['content']['*/*']['data']
type OpportunityRequest = paths['/api/opportunities']['post']['requestBody']['content']['application/json']
type ApproveRequest = paths['/api/opportunities/{id}/approve']['post']['requestBody']['content']['application/json']

export function getOpportunities(params: paths['/api/opportunities']['get']['parameters']['query']) {
  return request<OpportunityPage>({ url: '/opportunities', method: 'GET', params })
}

export function getOpportunity(id: number) {
  return request<OpportunityVO>({ url: `/opportunities/${id}`, method: 'GET' })
}

export function createOpportunity(data: OpportunityRequest) {
  return request<OpportunityVO>({ url: '/opportunities', method: 'POST', data })
}

export function updateOpportunity(id: number, data: OpportunityRequest) {
  return request<OpportunityVO>({ url: `/opportunities/${id}`, method: 'PUT', data })
}

export function deleteOpportunity(id: number) {
  return request<void>({ url: `/opportunities/${id}`, method: 'DELETE' })
}

export function submitOpportunity(id: number) {
  return request<void>({ url: `/opportunities/${id}/submit`, method: 'POST' })
}

export function approveOpportunity(id: number, data: ApproveRequest) {
  return request<void>({ url: `/opportunities/${id}/approve`, method: 'POST', data })
}

export function rejectOpportunity(id: number, data: ApproveRequest) {
  return request<void>({ url: `/opportunities/${id}/reject`, method: 'POST', data })
}
```

- [ ] **Step 3: 创建 project API**

创建 `frontend/src/api/project.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type ProjectPage = paths['/api/projects']['get']['responses']['200']['content']['*/*']['data']
type ProjectVO = paths['/api/projects/{id}']['get']['responses']['200']['content']['*/*']['data']
type ProjectRequest = paths['/api/projects']['post']['requestBody']['content']['application/json']
type BiddingNodeRequest = paths['/api/projects/{id}/bidding-node']['put']['requestBody']['content']['application/json']
type ContractNodeRequest = paths['/api/projects/{id}/contract-node']['put']['requestBody']['content']['application/json']

export function getProjects(params: paths['/api/projects']['get']['parameters']['query']) {
  return request<ProjectPage>({ url: '/projects', method: 'GET', params })
}

export function getProject(id: number) {
  return request<ProjectVO>({ url: `/projects/${id}`, method: 'GET' })
}

export function createProject(data: ProjectRequest) {
  return request<ProjectVO>({ url: '/projects', method: 'POST', data })
}

export function updateProject(id: number, data: ProjectRequest) {
  return request<ProjectVO>({ url: `/projects/${id}`, method: 'PUT', data })
}

export function updateProjectPNode(id: number, pNode: number) {
  return request<void>({ url: `/projects/${id}/p-node`, method: 'PUT', params: { pNode } })
}

export function updateProjectStage6(id: number, stage6: number) {
  return request<void>({ url: `/projects/${id}/stage-6`, method: 'PUT', params: { stage6 } })
}

export function updateProjectMilestone(id: number, data: unknown) {
  return request<void>({ url: `/projects/${id}/milestone`, method: 'PUT', data })
}

export function saveProjectBiddingNode(id: number, data: BiddingNodeRequest) {
  return request<void>({ url: `/projects/${id}/bidding-node`, method: 'PUT', data })
}

export function saveProjectContractNode(id: number, data: ContractNodeRequest) {
  return request<void>({ url: `/projects/${id}/contract-node`, method: 'PUT', data })
}
```

- [ ] **Step 4: 创建 contract API**

创建 `frontend/src/api/contract.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type ContractPage = paths['/api/contracts']['get']['responses']['200']['content']['*/*']['data']
type ContractVO = paths['/api/contracts/{id}']['get']['responses']['200']['content']['*/*']['data']
type ContractRequest = paths['/api/contracts']['post']['requestBody']['content']['application/json']

export function getContracts(params: paths['/api/contracts']['get']['parameters']['query']) {
  return request<ContractPage>({ url: '/contracts', method: 'GET', params })
}

export function getContract(id: number) {
  return request<ContractVO>({ url: `/contracts/${id}`, method: 'GET' })
}

export function createContract(data: ContractRequest) {
  return request<ContractVO>({ url: '/contracts', method: 'POST', data })
}

export function updateContract(id: number, data: ContractRequest) {
  return request<ContractVO>({ url: `/contracts/${id}`, method: 'PUT', data })
}

export function updateContractStatus(id: number, status: number) {
  return request<void>({ url: `/contracts/${id}/status`, method: 'PUT', params: { status } })
}
```

- [ ] **Step 5: 创建 dashboard API**

创建 `frontend/src/api/dashboard.ts`：

```ts
import { request } from './client'
import type { paths } from '@/types/api'

type DashboardVO = paths['/api/dashboard/my']['get']['responses']['200']['content']['*/*']['data']

export function getMyDashboard() {
  return request<DashboardVO>({ url: '/dashboard/my', method: 'GET' })
}
```

- [ ] **Step 6: 创建 admin API 模块**

创建 `frontend/src/api/admin/user.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type UserPage = paths['/api/admin/users']['get']['responses']['200']['content']['*/*']['data']
type UserVO = paths['/api/admin/users/{id}']['get']['responses']['200']['content']['*/*']['data']
type UserRequest = paths['/api/admin/users']['post']['requestBody']['content']['application/json']

export function getUsers(params: paths['/api/admin/users']['get']['parameters']['query']) {
  return request<UserPage>({ url: '/admin/users', method: 'GET', params })
}

export function getUser(id: number) {
  return request<UserVO>({ url: `/admin/users/${id}`, method: 'GET' })
}

export function createUser(data: UserRequest) {
  return request<UserVO>({ url: '/admin/users', method: 'POST', data })
}

export function updateUser(id: number, data: UserRequest) {
  return request<UserVO>({ url: `/admin/users/${id}`, method: 'PUT', data })
}

export function deleteUser(id: number) {
  return request<void>({ url: `/admin/users/${id}`, method: 'DELETE' })
}

export function resetPassword(id: number) {
  return request<void>({ url: `/admin/users/${id}/reset-password`, method: 'POST' })
}
```

创建 `frontend/src/api/admin/role.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type RoleList = paths['/api/admin/roles']['get']['responses']['200']['content']['*/*']['data']
type RoleVO = paths['/api/admin/roles/{id}']['get']['responses']['200']['content']['*/*']['data']
type RoleRequest = paths['/api/admin/roles']['post']['requestBody']['content']['application/json']

export function getRoles() {
  return request<RoleList>({ url: '/admin/roles', method: 'GET' })
}

export function getRole(id: number) {
  return request<RoleVO>({ url: `/admin/roles/${id}`, method: 'GET' })
}

export function createRole(data: RoleRequest) {
  return request<RoleVO>({ url: '/admin/roles', method: 'POST', data })
}

export function updateRole(id: number, data: RoleRequest) {
  return request<RoleVO>({ url: `/admin/roles/${id}`, method: 'PUT', data })
}

export function deleteRole(id: number) {
  return request<void>({ url: `/admin/roles/${id}`, method: 'DELETE' })
}
```

创建 `frontend/src/api/admin/dictionary.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'
import type { DictionaryItem } from '@/stores/dict'

type DictionaryList = paths['/api/admin/dictionaries/{type}']['get']['responses']['200']['content']['*/*']['data']
type DictionaryRequest = paths['/api/admin/dictionaries']['post']['requestBody']['content']['application/json']

export function getDictionariesByType(type: string) {
  return request<DictionaryList>({ url: `/admin/dictionaries/${type}`, method: 'GET' })
}

export function createDictionary(data: DictionaryRequest) {
  return request<DictionaryItem>({ url: '/admin/dictionaries', method: 'POST', data })
}

export function updateDictionary(id: number, data: DictionaryRequest) {
  return request<DictionaryItem>({ url: `/admin/dictionaries/${id}`, method: 'PUT', data })
}

export function deleteDictionary(id: number) {
  return request<void>({ url: `/admin/dictionaries/${id}`, method: 'DELETE' })
}
```

创建 `frontend/src/api/admin/unit.ts`：

```ts
import { request } from '../client'
import type { paths } from '@/types/api'

type UnitPage = paths['/api/admin/units']['get']['responses']['200']['content']['*/*']['data']
type UnitVO = paths['/api/admin/units/{id}']['get']['responses']['200']['content']['*/*']['data']
type UnitRequest = paths['/api/admin/units']['post']['requestBody']['content']['application/json']
type UnitList = paths['/api/admin/units/all']['get']['responses']['200']['content']['*/*']['data']

export function getUnits(params: paths['/api/admin/units']['get']['parameters']['query']) {
  return request<UnitPage>({ url: '/admin/units', method: 'GET', params })
}

export function getAllUnits() {
  return request<UnitList>({ url: '/admin/units/all', method: 'GET' })
}

export function getUnit(id: number) {
  return request<UnitVO>({ url: `/admin/units/${id}`, method: 'GET' })
}

export function createUnit(data: UnitRequest) {
  return request<UnitVO>({ url: '/admin/units', method: 'POST', data })
}

export function updateUnit(id: number, data: UnitRequest) {
  return request<UnitVO>({ url: `/admin/units/${id}`, method: 'PUT', data })
}

export function deleteUnit(id: number) {
  return request<void>({ url: `/admin/units/${id}`, method: 'DELETE' })
}
```

- [ ] **Step 7: 验证 lint 通过**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
```

- [ ] **Step 8: Commit**

```bash
git add frontend/src/api/
git commit -m "feat: add API modules for customer, opportunity, project, contract, dashboard and admin"
```

---

### Task 7: 创建 TanStack Query Hooks

**Files:**
- Create: `frontend/src/hooks/useCustomers.ts`
- Create: `frontend/src/hooks/useOpportunities.ts`
- Create: `frontend/src/hooks/useProjects.ts`
- Create: `frontend/src/hooks/useContracts.ts`
- Create: `frontend/src/hooks/useDashboard.ts`
- Create: `frontend/src/hooks/useAdminUsers.ts`
- Create: `frontend/src/hooks/useAdminRoles.ts`
- Create: `frontend/src/hooks/useAdminDictionaries.ts`
- Create: `frontend/src/hooks/useAdminUnits.ts`

**Interfaces:**
- Consumes: 各模块 API 函数。
- Produces: Query/Mutation hooks，供页面组件使用。

- [ ] **Step 1: 创建 customer hooks**

创建 `frontend/src/hooks/useCustomers.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getCustomers,
  getCustomer,
  createCustomer,
  updateCustomer,
  deleteCustomer,
} from '@/api/customer'

export function useCustomers(params: Parameters<typeof getCustomers>[0]) {
  return useQuery({
    queryKey: ['customers', params],
    queryFn: () => getCustomers(params),
  })
}

export function useCustomer(id: number | undefined) {
  return useQuery({
    queryKey: ['customer', id],
    queryFn: () => getCustomer(id!),
    enabled: !!id,
  })
}

export function useCreateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createCustomer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateCustomer>[1] }) =>
      updateCustomer(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
      queryClient.invalidateQueries({ queryKey: ['customer', variables.id] })
    },
  })
}

export function useDeleteCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteCustomer,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] })
    },
  })
}
```

- [ ] **Step 2: 创建 opportunity hooks**

创建 `frontend/src/hooks/useOpportunities.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getOpportunities,
  getOpportunity,
  createOpportunity,
  updateOpportunity,
  deleteOpportunity,
  submitOpportunity,
  approveOpportunity,
  rejectOpportunity,
} from '@/api/opportunity'

export function useOpportunities(params: Parameters<typeof getOpportunities>[0]) {
  return useQuery({ queryKey: ['opportunities', params], queryFn: () => getOpportunities(params) })
}

export function useOpportunity(id: number | undefined) {
  return useQuery({
    queryKey: ['opportunity', id],
    queryFn: () => getOpportunity(id!),
    enabled: !!id,
  })
}

export function useCreateOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useUpdateOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateOpportunity>[1] }) =>
      updateOpportunity(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['opportunities'] })
      queryClient.invalidateQueries({ queryKey: ['opportunity', variables.id] })
    },
  })
}

export function useDeleteOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useSubmitOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: submitOpportunity,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useApproveOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof approveOpportunity>[1] }) =>
      approveOpportunity(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}

export function useRejectOpportunity() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof rejectOpportunity>[1] }) =>
      rejectOpportunity(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['opportunities'] }),
  })
}
```

- [ ] **Step 3: 创建 project hooks**

创建 `frontend/src/hooks/useProjects.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getProjects,
  getProject,
  createProject,
  updateProject,
  updateProjectPNode,
  updateProjectStage6,
  updateProjectMilestone,
  saveProjectBiddingNode,
  saveProjectContractNode,
} from '@/api/project'

export function useProjects(params: Parameters<typeof getProjects>[0]) {
  return useQuery({ queryKey: ['projects', params], queryFn: () => getProjects(params) })
}

export function useProject(id: number | undefined) {
  return useQuery({
    queryKey: ['project', id],
    queryFn: () => getProject(id!),
    enabled: !!id,
  })
}

export function useCreateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createProject,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['projects'] }),
  })
}

export function useUpdateProject() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateProject>[1] }) =>
      updateProject(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['projects'] })
      queryClient.invalidateQueries({ queryKey: ['project', variables.id] })
    },
  })
}

export function useUpdateProjectPNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, pNode }: { id: number; pNode: number }) => updateProjectPNode(id, pNode),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useUpdateProjectStage6() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, stage6 }: { id: number; stage6: number }) => updateProjectStage6(id, stage6),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useUpdateProjectMilestone() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateProjectMilestone>[1] }) =>
      updateProjectMilestone(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useSaveProjectBiddingNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectBiddingNode>[1] }) =>
      saveProjectBiddingNode(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}

export function useSaveProjectContractNode() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof saveProjectContractNode>[1] }) =>
      saveProjectContractNode(id, data),
    onSuccess: (_, variables) => queryClient.invalidateQueries({ queryKey: ['project', variables.id] }),
  })
}
```

- [ ] **Step 4: 创建 contract hooks**

创建 `frontend/src/hooks/useContracts.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getContracts,
  getContract,
  createContract,
  updateContract,
  updateContractStatus,
} from '@/api/contract'

export function useContracts(params: Parameters<typeof getContracts>[0]) {
  return useQuery({ queryKey: ['contracts', params], queryFn: () => getContracts(params) })
}

export function useContract(id: number | undefined) {
  return useQuery({
    queryKey: ['contract', id],
    queryFn: () => getContract(id!),
    enabled: !!id,
  })
}

export function useCreateContract() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createContract,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['contracts'] }),
  })
}

export function useUpdateContract() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateContract>[1] }) =>
      updateContract(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['contracts'] })
      queryClient.invalidateQueries({ queryKey: ['contract', variables.id] })
    },
  })
}

export function useUpdateContractStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: number }) => updateContractStatus(id, status),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['contracts'] })
      queryClient.invalidateQueries({ queryKey: ['contract', variables.id] })
    },
  })
}
```

- [ ] **Step 5: 创建 dashboard hooks**

创建 `frontend/src/hooks/useDashboard.ts`：

```ts
import { useQuery } from '@tanstack/react-query'

import { getMyDashboard } from '@/api/dashboard'

export function useDashboard() {
  return useQuery({ queryKey: ['dashboard', 'my'], queryFn: getMyDashboard })
}
```

- [ ] **Step 6: 创建 admin hooks**

创建 `frontend/src/hooks/useAdminUsers.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getUsers, getUser, createUser, updateUser, deleteUser, resetPassword } from '@/api/admin/user'

export function useUsers(params: Parameters<typeof getUsers>[0]) {
  return useQuery({ queryKey: ['admin', 'users', params], queryFn: () => getUsers(params) })
}

export function useUser(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'user', id],
    queryFn: () => getUser(id!),
    enabled: !!id,
  })
}

export function useCreateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useUpdateUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateUser>[1] }) =>
      updateUser(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useDeleteUser() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteUser,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  })
}

export function useResetPassword() {
  return useMutation({ mutationFn: resetPassword })
}
```

创建 `frontend/src/hooks/useAdminRoles.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getRoles, getRole, createRole, updateRole, deleteRole } from '@/api/admin/role'

export function useRoles() {
  return useQuery({ queryKey: ['admin', 'roles'], queryFn: getRoles })
}

export function useRole(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'role', id],
    queryFn: () => getRole(id!),
    enabled: !!id,
  })
}

export function useCreateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createRole,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}

export function useUpdateRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateRole>[1] }) =>
      updateRole(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}

export function useDeleteRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteRole,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'roles'] }),
  })
}
```

创建 `frontend/src/hooks/useAdminDictionaries.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import {
  getDictionariesByType,
  createDictionary,
  updateDictionary,
  deleteDictionary,
} from '@/api/admin/dictionary'

export function useDictionaries(type: string) {
  return useQuery({
    queryKey: ['admin', 'dictionaries', type],
    queryFn: () => getDictionariesByType(type),
    enabled: !!type,
  })
}

export function useCreateDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createDictionary,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}

export function useUpdateDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateDictionary>[1] }) =>
      updateDictionary(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}

export function useDeleteDictionary() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteDictionary,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'dictionaries'] }),
  })
}
```

创建 `frontend/src/hooks/useAdminUnits.ts`：

```ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { getUnits, getAllUnits, getUnit, createUnit, updateUnit, deleteUnit } from '@/api/admin/unit'

export function useUnits(params: Parameters<typeof getUnits>[0]) {
  return useQuery({ queryKey: ['admin', 'units', params], queryFn: () => getUnits(params) })
}

export function useAllUnits() {
  return useQuery({ queryKey: ['admin', 'units', 'all'], queryFn: getAllUnits })
}

export function useUnit(id: number | undefined) {
  return useQuery({
    queryKey: ['admin', 'unit', id],
    queryFn: () => getUnit(id!),
    enabled: !!id,
  })
}

export function useCreateUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createUnit,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}

export function useUpdateUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: Parameters<typeof updateUnit>[1] }) =>
      updateUnit(id, data),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}

export function useDeleteUnit() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteUnit,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'units'] }),
  })
}
```

- [ ] **Step 7: 验证 lint 通过**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
```

- [ ] **Step 8: Commit**

```bash
git add frontend/src/hooks/
git commit -m "feat: add TanStack Query hooks for all modules"
```

---

### Task 8: 创建通用 UI 组件

**Files:**
- Create: `frontend/src/components/DataTable.tsx`
- Create: `frontend/src/components/SearchForm.tsx`
- Create: `frontend/src/components/DictSelect.tsx`
- Create: `frontend/src/components/DictTag.tsx`
- Create: `frontend/src/components/PageHeader.tsx`
- Create: `frontend/src/components/AuthButton.tsx`
- Create: `frontend/src/components/AmountInput.tsx`
- Create: `frontend/src/components/DatePickerField.tsx`
- Create: `frontend/src/components/WeekPickerField.tsx`
- Create: `frontend/src/components/FormFooter.tsx`

**Interfaces:**
- Consumes: `useDictStore`, `hasPermission`，`formatMoney`。
- Produces: 可复用 UI 组件。

- [ ] **Step 1: 创建 DataTable**

创建 `frontend/src/components/DataTable.tsx`：

```tsx
import { Table, type TableProps } from 'antd'

interface DataTableProps<T> extends Omit<TableProps<T>, 'pagination' | 'loading'> {
  data?: { records?: T[]; total?: number; current?: number; size?: number }
  loading?: boolean
  onPageChange?: (page: number, pageSize: number) => void
}

export function DataTable<T extends { id?: number | string }>({
  data,
  loading,
  onPageChange,
  rowKey = 'id',
  ...rest
}: DataTableProps<T>) {
  return (
    <Table
      rowKey={rowKey}
      loading={loading}
      dataSource={data?.records || []}
      pagination={{
        current: data?.current || 1,
        pageSize: data?.size || 20,
        total: data?.total || 0,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
        onChange: onPageChange,
      }}
      {...rest}
    />
  )
}
```

- [ ] **Step 2: 创建 SearchForm**

创建 `frontend/src/components/SearchForm.tsx`：

```tsx
import { useState } from 'react'
import { Button, Form, Row, Col, Card } from 'antd'
import { DownOutlined, UpOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'

export interface SearchField {
  name: string
  label: string
  component: React.ReactNode
}

interface SearchFormProps {
  fields: SearchField[]
  onSearch: (values: Record<string, unknown>) => void
  onReset?: () => void
  initialValues?: Record<string, unknown>
}

export function SearchForm({ fields, onSearch, onReset, initialValues }: SearchFormProps) {
  const [form] = Form.useForm()
  const [expanded, setExpanded] = useState(false)

  const handleReset = () => {
    form.resetFields()
    onReset?.()
  }

  const visibleFields = expanded ? fields : fields.slice(0, 3)

  return (
    <Card style={{ marginBottom: 16 }}>
      <Form form={form} initialValues={initialValues} onFinish={onSearch}>
        <Row gutter={16}>
          {visibleFields.map((field) => (
            <Col span={8} key={field.name}>
              <Form.Item name={field.name} label={field.label}>
                {field.component}
              </Form.Item>
            </Col>
          ))}
          <Col span={8} style={{ display: 'flex', justifyContent: 'flex-end', alignItems: 'flex-start' }}>
            <Button type="primary" htmlType="submit" icon={<SearchOutlined />}>
              查询
            </Button>
            <Button style={{ marginLeft: 8 }} onClick={handleReset} icon={<ReloadOutlined />}>
              重置
            </Button>
            {fields.length > 3 && (
              <Button type="link" onClick={() => setExpanded(!expanded)}>
                {expanded ? <><UpOutlined /> 收起</> : <><DownOutlined /> 展开</>}
              </Button>
            )}
          </Col>
        </Row>
      </Form>
    </Card>
  )
}
```

- [ ] **Step 3: 创建 DictSelect 和 DictTag**

创建 `frontend/src/components/DictSelect.tsx`：

```tsx
import { Select } from 'antd'

import { useDictStore } from '@/stores/dict'

interface DictSelectProps {
  type: string
  value?: string | number
  onChange?: (value: string | number) => void
  placeholder?: string
  allowClear?: boolean
  disabled?: boolean
}

export function DictSelect({ type, value, onChange, placeholder, allowClear, disabled }: DictSelectProps) {
  const dicts = useDictStore((state) => state.getDict(type))

  return (
    <Select
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择'}
      allowClear={allowClear}
      disabled={disabled}
      style={{ minWidth: 120 }}
    >
      {dicts.map((item) => (
        <Select.Option key={item.code} value={item.code}>
          {item.name}
        </Select.Option>
      ))}
    </Select>
  )
}
```

创建 `frontend/src/components/DictTag.tsx`：

```tsx
import { Tag } from 'antd'

import { useDictStore } from '@/stores/dict'

const colorMap: Record<string, Record<string, string>> = {
  opportunity_status: {
    '1': 'default',
    '2': 'processing',
    '3': 'success',
    '4': 'error',
  },
  project_status: {
    '1': 'processing',
    '2': 'success',
    '3': 'warning',
    '4': 'error',
  },
}

interface DictTagProps {
  type: string
  value?: string | number
}

export function DictTag({ type, value }: DictTagProps) {
  const name = useDictStore((state) => state.getDictName(type, String(value)))
  const color = colorMap[type]?.[String(value)]
  return <Tag color={color}>{name}</Tag>
}
```

- [ ] **Step 4: 创建 PageHeader 和 AuthButton**

创建 `frontend/src/components/PageHeader.tsx`：

```tsx
import { Breadcrumb, Button, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import { ArrowLeftOutlined } from '@ant-design/icons'

interface PageHeaderProps {
  title: string
  breadcrumbs?: string[]
  extra?: React.ReactNode
  onBack?: () => void
  showBack?: boolean
}

export function PageHeader({ title, breadcrumbs, extra, onBack, showBack }: PageHeaderProps) {
  const navigate = useNavigate()

  return (
    <div style={{ marginBottom: 24 }}>
      {breadcrumbs && breadcrumbs.length > 0 && (
        <Breadcrumb style={{ marginBottom: 12 }}>
          {breadcrumbs.map((item, index) => (
            <Breadcrumb.Item key={index}>{item}</Breadcrumb.Item>
          ))}
        </Breadcrumb>
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Space>
          {showBack && (
            <Button icon={<ArrowLeftOutlined />} onClick={onBack || (() => navigate(-1))}>
              返回
            </Button>
          )}
          <h2 style={{ margin: 0 }}>{title}</h2>
        </Space>
        <div>{extra}</div>
      </div>
    </div>
  )
}
```

创建 `frontend/src/components/AuthButton.tsx`：

```tsx
import { Button, type ButtonProps } from 'antd'

import { useAuthStore } from '@/stores/auth'
import { hasPermission } from '@/utils/permission'

interface AuthButtonProps extends ButtonProps {
  code: string
  fallback?: 'hidden' | 'disabled'
}

export function AuthButton({ code, fallback = 'hidden', children, ...rest }: AuthButtonProps) {
  const permissionCodes = useAuthStore((state) => state.permissionCodes)
  const allowed = hasPermission(permissionCodes, code)

  if (!allowed && fallback === 'hidden') {
    return null
  }

  return (
    <Button disabled={!allowed} {...rest}>
      {children}
    </Button>
  )
}
```

- [ ] **Step 5: 创建 AmountInput、DatePickerField、WeekPickerField**

创建 `frontend/src/components/AmountInput.tsx`：

```tsx
import { InputNumber } from 'antd'

interface AmountInputProps {
  value?: number
  onChange?: (value: number | null) => void
  placeholder?: string
  disabled?: boolean
  min?: number
  max?: number
}

export function AmountInput({ value, onChange, placeholder, disabled, min = 0, max }: AmountInputProps) {
  return (
    <InputNumber
      value={value}
      onChange={onChange}
      placeholder={placeholder}
      disabled={disabled}
      min={min}
      max={max}
      precision={2}
      style={{ width: '100%' }}
      addonAfter="万元"
    />
  )
}
```

创建 `frontend/src/components/DatePickerField.tsx`：

```tsx
import { DatePicker } from 'antd'
import type { Dayjs } from 'dayjs'

interface DatePickerFieldProps {
  value?: Dayjs | null
  onChange?: (value: Dayjs | null) => void
  placeholder?: string
  disabled?: boolean
}

export function DatePickerField({ value, onChange, placeholder, disabled }: DatePickerFieldProps) {
  return (
    <DatePicker
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择日期'}
      disabled={disabled}
      style={{ width: '100%' }}
    />
  )
}
```

创建 `frontend/src/components/WeekPickerField.tsx`：

```tsx
import { DatePicker } from 'antd'
import type { Dayjs } from 'dayjs'

interface WeekPickerFieldProps {
  value?: Dayjs | null
  onChange?: (value: Dayjs | null) => void
  placeholder?: string
  disabled?: boolean
}

export function WeekPickerField({ value, onChange, placeholder, disabled }: WeekPickerFieldProps) {
  return (
    <DatePicker
      value={value}
      onChange={onChange}
      placeholder={placeholder || '请选择周'}
      disabled={disabled}
      style={{ width: '100%' }}
      picker="week"
    />
  )
}
```

- [ ] **Step 6: 创建 FormFooter**

创建 `frontend/src/components/FormFooter.tsx`：

```tsx
import { Button, Space, Card } from 'antd'
import { useNavigate } from 'react-router-dom'

interface FormFooterProps {
  loading?: boolean
  onReset?: () => void
  showReset?: boolean
  cancelPath?: string
}

export function FormFooter({ loading, onReset, showReset = true, cancelPath }: FormFooterProps) {
  const navigate = useNavigate()

  return (
    <Card style={{ position: 'sticky', bottom: 0, marginTop: 24 }}>
      <Space>
        <Button type="primary" htmlType="submit" loading={loading}>
          保存
        </Button>
        {showReset && (
          <Button onClick={onReset} disabled={loading}>
            重置
          </Button>
        )}
        <Button onClick={() => (cancelPath ? navigate(cancelPath) : navigate(-1))} disabled={loading}>
          取消
        </Button>
      </Space>
    </Card>
  )
}
```

- [ ] **Step 7: 验证 lint 和 build**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
npm run build
```

Expected: 无错误。

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/
git commit -m "feat: add shared UI components"
```

---

### Task 9: 重构 BasicLayout 支持动态菜单

**Files:**
- Modify: `frontend/src/layouts/BasicLayout.tsx`

**Interfaces:**
- Consumes: `useAuthStore`, `useMenuStore`，`useNavigate`，`@ant-design/icons`。
- Produces: 动态渲染侧边栏、顶部用户菜单、退出登录。

- [ ] **Step 1: 用完整代码替换 BasicLayout.tsx**

将 `frontend/src/layouts/BasicLayout.tsx` 替换为：

```tsx
import { useMemo, useState } from 'react'
import { Layout, Menu, Dropdown, Button, Space, Badge } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  UserOutlined,
  LogoutOutlined,
  BellOutlined,
} from '@ant-design/icons'

import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import type { MenuItem } from '@/types/app'
import * as Icons from '@ant-design/icons'

const { Header, Sider, Content } = Layout

function renderMenuItems(items: MenuItem[]): any[] {
  return items.map((item) => {
    const IconComponent = item.icon ? (Icons as Record<string, React.ComponentType>)[item.icon] : null
    if (item.children && item.children.length > 0) {
      return {
        key: item.path || String(item.id),
        icon: IconComponent ? <IconComponent /> : null,
        label: item.name,
        children: renderMenuItems(item.children),
      }
    }
    return {
      key: item.path || String(item.id),
      icon: IconComponent ? <IconComponent /> : null,
      label: item.name,
    }
  })
}

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, clearAuth } = useAuthStore()
  const { menus } = useMenuStore()
  const [collapsed, setCollapsed] = useState(false)

  const menuItems = useMemo(() => renderMenuItems(menus), [menus])

  const selectedKeys = useMemo(() => {
    return [location.pathname]
  }, [location.pathname])

  const handleMenuClick = ({ key }: { key: string }) => {
    navigate(key)
  }

  const handleLogout = () => {
    clearAuth()
    navigate('/login')
  }

  const userMenuItems = [
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} theme="light">
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 'bold',
            borderBottom: '1px solid #f0f0f0',
          }}
        >
          {collapsed ? 'CRM' : 'CRM 管理系统'}
        </div>
        <Menu
          mode="inline"
          theme="light"
          selectedKeys={selectedKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button type="text" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={() => setCollapsed(!collapsed)} />
          <Space>
            <Badge count={0}>
              <Button type="text" icon={<BellOutlined />} />
            </Badge>
            <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
              <Button type="text">
                <UserOutlined /> {user?.realName || user?.username}
              </Button>
            </Dropdown>
          </Space>
        </Header>
        <Content style={{ margin: 24, padding: 24, background: '#fff', minHeight: 280, borderRadius: 8 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
```

- [ ] **Step 2: 创建 BlankLayout**

创建 `frontend/src/layouts/BlankLayout.tsx`：

```tsx
import { Outlet } from 'react-router-dom'

export default function BlankLayout() {
  return <Outlet />
}
```

- [ ] **Step 3: 验证 lint 和 build**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
npm run build
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/layouts/BasicLayout.tsx frontend/src/layouts/BlankLayout.tsx
git commit -m "feat: dynamic menu layout and blank layout"
```

---

### Task 10: 重构路由支持动态菜单和权限守卫

**Files:**
- Modify: `frontend/src/router/index.tsx`
- Create: `frontend/src/pages/error/403.tsx`

**Interfaces:**
- Consumes: `useAuthStore`, `useMenuStore`，`hasPermission`。
- Produces: 根据权限动态生成的路由表，403 页面。

- [ ] **Step 1: 创建 403 页面**

创建 `frontend/src/pages/error/403.tsx`：

```tsx
import { Button, Result } from 'antd'
import { useNavigate } from 'react-router-dom'

export default function ForbiddenPage() {
  const navigate = useNavigate()
  return (
    <Result
      status="403"
      title="403"
      subTitle="抱歉，您没有权限访问该页面"
      extra={
        <Button type="primary" onClick={() => navigate('/dashboard')}>
          返回工作台
        </Button>
      }
    />
  )
}
```

- [ ] **Step 2: 重构 router/index.tsx**

将 `frontend/src/router/index.tsx` 替换为：

```tsx
import { Suspense, lazy, useMemo } from 'react'
import { createBrowserRouter, Navigate, Outlet, RouterProvider, type RouteObject } from 'react-router-dom'
import { Spin } from 'antd'

import BasicLayout from '@/layouts/BasicLayout'
import BlankLayout from '@/layouts/BlankLayout'
import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { hasPermission } from '@/utils/permission'
import type { MenuItem } from '@/types/app'

const LoginPage = lazy(() => import('@/pages/login'))
const DashboardPage = lazy(() => import('@/pages/dashboard'))
const CustomerPage = lazy(() => import('@/pages/customer'))
const CustomerCreatePage = lazy(() => import('@/pages/customer/create'))
const CustomerDetailPage = lazy(() => import('@/pages/customer/detail'))
const CustomerEditPage = lazy(() => import('@/pages/customer/edit'))
const OpportunityPage = lazy(() => import('@/pages/opportunity'))
const ProjectPage = lazy(() => import('@/pages/project'))
const ContractPage = lazy(() => import('@/pages/contract'))
const UsersPage = lazy(() => import('@/pages/system/users'))
const RolesPage = lazy(() => import('@/pages/system/roles'))
const DictionaryPage = lazy(() => import('@/pages/system/dictionary'))
const UnitsPage = lazy(() => import('@/pages/system/units'))
const ForbiddenPage = lazy(() => import('@/pages/error/403'))

const PageLoading = () => (
  <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 100 }}>
    <Spin size="large" />
  </div>
)

const LazyWrapper = ({ children }: { children: React.ReactNode }) => (
  <Suspense fallback={<PageLoading />}>{children}</Suspense>
)

function AuthGuard({ children }: { children: React.ReactNode }) {
  const token = useAuthStore((state) => state.token)
  return token ? children : <Navigate to="/login" replace />
}

function PermissionGuard({ requiredCode, children }: { requiredCode?: string; children: React.ReactNode }) {
  const permissionCodes = useAuthStore((state) => state.permissionCodes)
  if (requiredCode && !hasPermission(permissionCodes, requiredCode)) {
    return <Navigate to="/403" replace />
  }
  return children
}

function buildRoutesFromMenus(menus: MenuItem[]): RouteObject[] {
  const map: Record<string, React.ReactNode> = {
    '/dashboard': <DashboardPage />,
    '/customer': <CustomerPage />,
    '/customer/create': <CustomerCreatePage />,
    '/customer/:id': <CustomerDetailPage />,
    '/customer/:id/edit': <CustomerEditPage />,
    '/opportunity': <OpportunityPage />,
    '/project': <ProjectPage />,
    '/contract': <ContractPage />,
    '/system/users': <UsersPage />,
    '/system/roles': <RolesPage />,
    '/system/dictionary': <DictionaryPage />,
    '/system/units': <UnitsPage />,
  }

  const routes: RouteObject[] = []

  function walk(items: MenuItem[]) {
    items.forEach((item) => {
      if (item.children) {
        walk(item.children)
      } else if (item.path && map[item.path]) {
        routes.push({
          path: item.path,
          element: (
            <LazyWrapper>
              <PermissionGuard requiredCode={item.permission}>{map[item.path]}</PermissionGuard>
            </LazyWrapper>
          ),
        })
      }
    })
  }

  walk(menus)
  return routes
}

export function useRoutes() {
  const { token, permissionCodes } = useAuthStore()
  const { menus } = useMenuStore()

  return useMemo<RouteObject[]>(() => {
    const dynamicRoutes = buildRoutesFromMenus(menus)

    return [
      {
        path: '/login',
        element: (
          <BlankLayout>
            <LazyWrapper>
              <LoginPage />
            </LazyWrapper>
          </BlankLayout>
        ),
      },
      {
        path: '/',
        element: (
          <AuthGuard>
            <BasicLayout />
          </AuthGuard>
        ),
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          ...dynamicRoutes,
          {
            path: '403',
            element: (
              <LazyWrapper>
                <ForbiddenPage />
              </LazyWrapper>
            ),
          },
          {
            path: '*',
            element: <Navigate to="/dashboard" replace />,
          },
        ],
      },
      {
        path: '*',
        element: <Navigate to="/login" replace />,
      },
    ]
  }, [menus, token, permissionCodes])
}

export function Router() {
  const routes = useRoutes()
  const router = useMemo(() => createBrowserRouter(routes), [routes])
  return <RouterProvider router={router} />
}
```

需确保 `App.tsx` 中使用 `Router` 组件。

- [ ] **Step 3: 更新 App.tsx**

修改 `frontend/src/App.tsx`：

```tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'

import { Router } from '@/router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ConfigProvider locale={zhCN}>
        <Router />
      </ConfigProvider>
    </QueryClientProvider>
  )
}
```

- [ ] **Step 4: 验证 lint 和 build**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
npm run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/router/index.tsx frontend/src/pages/error/403.tsx frontend/src/App.tsx
git commit -m "feat: dynamic routes with permission guard"
```

---

### Task 11: 最终验证

**Files:**
- 所有新增/修改文件

- [ ] **Step 1: 运行完整 lint 和 build**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run lint
npm run build
```

Expected: 无错误，无 warning。

- [ ] **Step 2: 启动开发服务器冒烟测试**

```bash
cd /Users/xavier/Projects/Github/crm/frontend
npm run dev
```

在浏览器访问 `http://localhost:8000/login`，确认页面正常加载，无白屏或报错。

- [ ] **Step 3: Commit 验证结果**

```bash
git add .
git commit -m "chore: finalize shared infrastructure and verify build"
```

---

## Self-Review

### Spec Coverage

| Spec Section | 对应 Task |
|--------------|----------|
| OpenAPI 类型生成 | Task 1 |
| API 层封装 | Task 6 |
| TanStack Query hooks | Task 7 |
| 统一错误处理 | Task 2 |
| 动态菜单 | Task 5, Task 9 |
| 权限控制 | Task 3, Task 10 |
| 字典缓存 | Task 4, Task 5 |
| 通用组件 | Task 8 |
| 布局/路由 | Task 9, Task 10 |
| 登录初始化 | Task 5 |

### Placeholder Scan

- 无 TBD/TODO。
- 所有代码块完整，包含 exact file paths。
- 所有任务包含验证命令。

### Type Consistency

- `LoginResult` 在 `auth.ts` 和 `auth store` 中保持一致。
- `DictionaryItem` 在 `dict store` 和 `dictionary API` 中保持一致。
- 所有 hooks 的 mutation 参数类型与 API 函数一致。

### Risk Notes

- `openapi-typescript` 生成的类型路径需以实际生成为准；若后端返回 `application/json` 而非 `*/*`，需相应调整。
- `BasicLayout` 中的图标动态映射依赖后端 `menu.icon` 与 `@ant-design/icons` 导出名称一致；若不一致需建立映射表。

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-01-frontend-shared-infrastructure.md`.**

执行方式二选一：

1. **Subagent-Driven（推荐）**：我按任务逐个调度子代理执行，每完成一个任务后 Review。
2. **Inline Execution**：在当前会话中按任务逐步执行，适合你实时监控。

你选哪种？
