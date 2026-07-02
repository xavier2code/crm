# 前端登录页验证码集成实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在前端登录页增加验证码输入框与图片展示，使登录请求携带 `captchaUuid` 和 `captchaCode`，支持点击图片刷新验证码。

**Architecture:** 新增受控组件 `CaptchaInput` 封装验证码获取、展示、刷新逻辑；`src/api/auth.ts` 补充验证码类型与请求函数；`src/pages/login/index.tsx` 引入组件并在登录失败时主动刷新验证码。

**Tech Stack:** React 19, TypeScript 6, Ant Design 6, Vite 8, Axios（通过项目封装的 `request`）

## Global Constraints

- 所有新增代码使用 TypeScript，类型显式声明
- 遵循项目 ESLint 与 Prettier 配置
- 组件使用函数式组件 + Hooks
- API 请求统一通过 `src/api/client.ts` 的 `request` 函数
- 登录接口路径统一无前缀 `/api`（`request` 已配置 baseURL 或拦截器处理）
- 不修改后端代码
- 验证码图片使用后端返回的 Base64 字符串直接渲染

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `frontend/src/api/auth.ts` | 修改 | 扩展 `LoginParams`，新增 `CaptchaResult` 与 `fetchCaptcha` |
| `frontend/src/components/CaptchaInput/index.tsx` | 创建 | 验证码输入+图片受控组件，负责请求、展示、刷新 |
| `frontend/src/pages/login/index.tsx` | 修改 | 引入 `CaptchaInput`，组合进登录表单，登录失败刷新验证码 |

---

### Task 1: 扩展认证 API 类型与请求

**Files:**
- Modify: `frontend/src/api/auth.ts`

**Interfaces:**
- Consumes: 无
- Produces:
  - `LoginParams` 增加 `captchaUuid: string` 和 `captchaCode: string`
  - `CaptchaResult` 接口：`{ image: string; uuid: string }`
  - `fetchCaptcha()` 函数：返回 `Promise<CaptchaResult>`

- [ ] **Step 1: 修改 `LoginParams` 并新增验证码相关类型与函数**

将 `frontend/src/api/auth.ts` 完整替换为以下内容：

```ts
import type { CurrentUser, MenuItem } from '@/types/app'
import type { DictionaryItem } from '@/stores/dict'

import { request } from './client'

export interface LoginParams {
  username: string
  password: string
  captchaUuid: string
  captchaCode: string
}

export interface LoginResult {
  accessToken: string
  refreshToken?: string
  tokenType?: string
  userInfo: CurrentUser
  roles: string[]
  menuTree?: MenuItem[]
  permissionCodes: string[]
}

export interface CaptchaResult {
  image: string
  uuid: string
}

export const login = (params: LoginParams) =>
  request<LoginResult>({ method: 'POST', url: '/auth/login', data: params })

export const fetchCaptcha = () =>
  request<CaptchaResult>({ method: 'GET', url: '/auth/captcha' })

export const fetchCurrentUser = () => request<CurrentUser>({ method: 'GET', url: '/auth/currentUser' })

export const logout = () => request<void>({ method: 'POST', url: '/auth/logout' })

export function fetchDictionaries() {
  return request<DictionaryItem[]>({ url: '/admin/dictionaries', method: 'GET' })
}
```

- [ ] **Step 2: 验证类型无语法错误**

Run:
```bash
cd frontend
npx tsc --noEmit
```

Expected: 无新增类型错误（允许项目已有的错误，但 `auth.ts` 本身不能引入新错误）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/api/auth.ts
git commit -m "feat(auth): extend LoginParams and add fetchCaptcha API"
```

---

### Task 2: 创建 `CaptchaInput` 组件

**Files:**
- Create: `frontend/src/components/CaptchaInput/index.tsx`

**Interfaces:**
- Consumes:
  - `fetchCaptcha()` from `frontend/src/api/auth.ts`
  - `request()` error handling from `frontend/src/api/client.ts`
- Produces:
  - `CaptchaInput` 默认导出组件
  - `CaptchaInputProps`：`{ value?: CaptchaInputValue; onChange?: (value: CaptchaInputValue) => void }`
  - `CaptchaInputValue`：`{ captchaUuid: string; captchaCode: string }`
  - 组件命令式句柄：`{ refresh: () => void }`

- [ ] **Step 1: 创建组件文件**

创建 `frontend/src/components/CaptchaInput/index.tsx`，内容如下：

```tsx
import { forwardRef, useEffect, useImperativeHandle, useState } from 'react'
import { Input, Spin } from 'antd'
import { fetchCaptcha } from '@/api/auth'

export interface CaptchaInputValue {
  captchaUuid: string
  captchaCode: string
}

interface CaptchaInputProps {
  value?: CaptchaInputValue
  onChange?: (value: CaptchaInputValue) => void
}

export interface CaptchaInputRef {
  refresh: () => void
}

const CaptchaInput = forwardRef<CaptchaInputRef, CaptchaInputProps>(
  ({ value, onChange }, ref) => {
    const [uuid, setUuid] = useState('')
    const [image, setImage] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(false)

    const refresh = async () => {
      setLoading(true)
      setError(false)
      try {
        const res = await fetchCaptcha()
        setUuid(res.uuid)
        setImage(res.image)
        onChange?.({ captchaUuid: res.uuid, captchaCode: '' })
      } catch {
        setError(true)
        setImage('')
        setUuid('')
      } finally {
        setLoading(false)
      }
    }

    useImperativeHandle(ref, () => ({ refresh }))

    useEffect(() => {
      refresh()
      // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    const handleCodeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      onChange?.({ captchaUuid: uuid, captchaCode: e.target.value })
    }

    return (
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <Input
          value={value?.captchaCode || ''}
          onChange={handleCodeChange}
          placeholder="验证码"
          maxLength={6}
        />
        <div
          onClick={refresh}
          style={{
            width: 100,
            height: 32,
            flexShrink: 0,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            background: '#f5f5f5',
            border: '1px solid #d9d9d9',
            borderRadius: 6,
            overflow: 'hidden',
          }}
        >
          {loading ? (
            <Spin size="small" />
          ) : error ? (
            <span style={{ fontSize: 12, color: '#999' }}>点击刷新</span>
          ) : (
            image && (
              <img
                src={image}
                alt="验证码"
                style={{ width: '100%', height: '100%', objectFit: 'contain' }}
              />
            )
          )}
        </div>
      </div>
    )
  }
)

CaptchaInput.displayName = 'CaptchaInput'

export default CaptchaInput
```

- [ ] **Step 2: 运行 lint 检查新组件**

Run:
```bash
cd frontend
npm run lint
```

Expected: 无新增 lint 错误（允许项目已有的告警，但 `CaptchaInput/index.tsx` 不能引入新告警）

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/CaptchaInput/index.tsx
git commit -m "feat(components): add CaptchaInput for login verification"
```

---

### Task 3: 登录页集成验证码

**Files:**
- Modify: `frontend/src/pages/login/index.tsx`

**Interfaces:**
- Consumes:
  - `CaptchaInput` 组件 from `frontend/src/components/CaptchaInput/index.tsx`
  - `CaptchaInputRef` from `frontend/src/components/CaptchaInput/index.tsx`
  - `login()` from `frontend/src/api/auth.ts`
- Produces:
  - 登录表单字段扩展为 `{ username: string; password: string; captcha: CaptchaInputValue }`
  - 登录失败时调用 `captchaRef.current.refresh()` 刷新验证码

- [ ] **Step 1: 修改登录页**

将 `frontend/src/pages/login/index.tsx` 完整替换为以下内容：

```tsx
import { useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, message } from 'antd'

import { useAuthStore } from '@/stores/auth'
import { useMenuStore } from '@/stores/menu'
import { useDictStore } from '@/stores/dict'
import { login, fetchCurrentUser, fetchDictionaries } from '@/api/auth'
import CaptchaInput from '@/components/CaptchaInput'
import type { CaptchaInputRef, CaptchaInputValue } from '@/components/CaptchaInput'
import type { DictionaryItem } from '@/stores/dict'

export default function LoginPage() {
  const navigate = useNavigate()
  const { setAuth, setUser } = useAuthStore()
  const [loading, setLoading] = useState(false)
  const captchaRef = useRef<CaptchaInputRef>(null)

  const handleLogin = async (values: {
    username: string
    password: string
    captcha: CaptchaInputValue
  }) => {
    setLoading(true)
    try {
      const { username, password, captcha } = values
      const result = await login({
        username,
        password,
        captchaUuid: captcha.captchaUuid,
        captchaCode: captcha.captchaCode,
      })
      setAuth(result)
      useMenuStore.getState().setMenus(result.menuTree || [])

      const dicts = await fetchDictionaries()
      const dictMap: Record<string, DictionaryItem[]> = {}
      dicts.forEach((item) => {
        if (!dictMap[item.type]) dictMap[item.type] = []
        dictMap[item.type].push(item)
      })
      useDictStore.getState().setDicts(dictMap)

      const user = await fetchCurrentUser()
      setUser(user)
      message.success('登录成功')
      navigate('/dashboard')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败')
      captchaRef.current?.refresh()
    } finally {
      setLoading(false)
    }
  }

  return (
    <div
      style={{
        height: '100vh',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        background: '#f0f2f5',
      }}
    >
      <Card title="CRM 管理系统" style={{ width: 360 }}>
        <Form onFinish={handleLogin} autoComplete="off">
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="密码" />
          </Form.Item>
          <Form.Item
            name="captcha"
            rules={[{ required: true, message: '请输入验证码' }]}
          >
            <CaptchaInput ref={captchaRef} />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading} block>
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
```

- [ ] **Step 2: 运行 lint 检查登录页**

Run:
```bash
cd frontend
npm run lint
```

Expected: 无新增 lint 错误

- [ ] **Step 3: Commit**

```bash
git add frontend/src/pages/login/index.tsx
git commit -m "feat(login): integrate CaptchaInput into login form"
```

---

### Task 4: 端到端验证

**Files:**
- 无新增或修改文件，仅运行验证命令

**Interfaces:**
- Consumes: 前述任务完成后的完整登录流程
- Produces: 验证结果

- [ ] **Step 1: 确认后端服务已启动**

Run:
```bash
curl http://localhost:8080/api/auth/captcha
```

Expected: 返回 JSON，包含 `data.image`（Base64 字符串）和 `data.uuid`

- [ ] **Step 2: 启动前端开发服务器**

Run:
```bash
cd frontend
npm run dev
```

Expected: 控制台显示开发服务器运行在 `http://localhost:8000`

- [ ] **Step 3: 浏览器验证登录页**

打开 `http://localhost:8000/login`，确认：

1. 密码输入框下方出现验证码输入框和验证码图片
2. 验证码图片可以点击刷新
3. 输入正确的用户名、密码、验证码后点击登录，能成功跳转 `/dashboard`
4. 输入错误的验证码后点击登录，提示验证码错误，且验证码图片自动刷新

- [ ] **Step 4: 运行前端构建**

Run:
```bash
cd frontend
npm run build
```

Expected: 构建成功，无类型错误与 lint 错误

- [ ] **Step 5: Commit 验证结果或修复**

如果验证过程中发现小修复，单独 commit：

```bash
git add <fixed-files>
git commit -m "fix(login): resolve captcha integration issues"
```

---

## Self-Review

### Spec Coverage

| 设计文档要求 | 对应任务 |
|--------------|----------|
| 新增 `CaptchaInput` 组件，内部管理 uuid/image/loading | Task 2 |
| 组件挂载时自动请求验证码 | Task 2 Step 1 `useEffect` |
| 用户输入验证码时回传 `{ captchaUuid, captchaCode }` | Task 2 Step 1 `handleCodeChange` |
| 点击图片刷新验证码 | Task 2 Step 1 `onClick={refresh}` |
| 接口失败显示"点击刷新"占位 | Task 2 Step 1 error 分支 |
| `LoginParams` 增加验证码字段 | Task 1 Step 1 |
| 新增 `fetchCaptcha` 函数 | Task 1 Step 1 |
| 登录页引入组件并组合进表单 | Task 3 Step 1 |
| 登录失败刷新验证码 | Task 3 Step 1 `catch` 块 |
| 登录请求携带 `captchaUuid` 和 `captchaCode` | Task 3 Step 1 `handleLogin` |

### Placeholder Scan

- 无 "TBD"、"TODO"、"implement later" 等占位符
- 每个步骤包含完整代码或具体命令
- 无 "适当处理错误" 等模糊描述

### Type Consistency

- `LoginParams` 中的字段名 `captchaUuid`、`captchaCode` 与后端 `LoginRequest` 一致
- `CaptchaInputValue` 中字段名与 `LoginParams` 一致
- `fetchCaptcha` 返回类型 `CaptchaResult` 与后端 `CaptchaResponse` 结构一致
- `CaptchaInputRef.refresh` 在 Task 2 定义，Task 3 通过 `captchaRef.current?.refresh()` 调用，签名一致

---

## Execution Handoff

**Plan complete and saved to `docs/superpowers/plans/2026-07-01-login-captcha-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**
