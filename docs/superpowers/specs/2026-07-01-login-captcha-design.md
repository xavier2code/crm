# 前端登录页验证码集成设计

## 背景

后端 `POST /api/auth/login` 要求请求体必须包含 `captchaUuid` 和 `captchaCode`，但当前前端登录页 `src/pages/login/index.tsx` 只有用户名、密码两个字段，导致登录请求被后端校验拒绝。后端已提供 `GET /api/auth/captcha` 接口返回 Base64 验证码图片和 UUID。

## 目标

在前端登录页增加验证码输入与展示，支持点击图片刷新，使登录流程与后端接口对齐。

## 方案

采用 **方案 2：提取 `CaptchaInput` 组件**，将验证码的获取、展示、刷新逻辑封装为独立受控组件，登录页仅负责组合与提交。

## 组件设计

### 新增 `src/components/CaptchaInput/index.tsx`

#### Props

```ts
interface CaptchaInputProps {
  value?: { captchaUuid: string; captchaCode: string }
  onChange?: (value: { captchaUuid: string; captchaCode: string }) => void
}
```

#### 内部状态

- `uuid`：当前验证码 UUID
- `image`：当前验证码 Base64 图片
- `loading`：是否正在刷新验证码

#### 行为

1. 组件挂载时自动调用 `fetchCaptcha()` 获取验证码。
2. 用户在输入框输入时，通过 `onChange` 回传 `{ captchaUuid: uuid, captchaCode: 输入值 }`。
3. 点击图片触发 `refresh()`，重新获取验证码并清空已输入的验证码。
4. 接口失败时显示"加载失败，点击刷新"占位，点击后重试。

#### UI 布局

Ant Design `Input` 与 `img` 在同一行并排：

- 输入框占据剩余宽度
- 验证码图片固定宽度约 100px、高度 32px，与输入框同高
- 图片左侧与输入框之间留 8-12px 间距
- 图片可点击，鼠标悬停显示手型指针

## API 层修改

### `src/api/auth.ts`

1. `LoginParams` 增加 `captchaUuid` 和 `captchaCode`：

```ts
export interface LoginParams {
  username: string
  password: string
  captchaUuid: string
  captchaCode: string
}
```

2. 新增验证码获取函数：

```ts
export interface CaptchaResult {
  image: string
  uuid: string
}

export const fetchCaptcha = () =>
  request<CaptchaResult>({ method: 'GET', url: '/auth/captcha' })
```

## 登录页修改

### `src/pages/login/index.tsx`

1. 引入 `CaptchaInput` 组件。
2. Form 字段从 `{ username, password }` 扩展为 `{ username, password, captcha }`，其中 `captcha` 是对象 `{ captchaUuid, captchaCode }`。
3. 在密码字段下方新增 Form.Item：

```tsx
<Form.Item
  name="captcha"
  rules={[{ required: true, message: '请输入验证码' }]}
>
  <CaptchaInput />
</Form.Item>
```

4. `handleLogin` 提交时解构：

```ts
const handleLogin = async (values: {
  username: string
  password: string
  captcha: { captchaUuid: string; captchaCode: string }
}) => {
  const { username, password, captcha } = values
  const result = await login({
    username,
    password,
    captchaUuid: captcha.captchaUuid,
    captchaCode: captcha.captchaCode,
  })
  // ...
}
```

5. 登录失败时刷新验证码：
   - 在 catch 中通过 ref 调用 `CaptchaInput.refresh()`，或在组件内部监听 `value` 为空时自动刷新。
   - 推荐方案：登录页维护 `captchaKey` 状态，失败时改变 key 触发 `CaptchaInput` 重新挂载并自动刷新。

## 数据流

```
用户进入登录页
  ↓
CaptchaInput 挂载 → GET /auth/captcha → 设置 uuid + image
  ↓
用户输入验证码 → onChange → Form 字段 captcha = { captchaUuid, captchaCode }
  ↓
用户点击登录 → handleLogin → 解构 captcha → POST /auth/login
  ↓
登录失败 → message.error + 刷新验证码
```

## 错误处理

| 场景 | 处理方式 |
|------|----------|
| 验证码接口失败 | 图片区域显示"加载失败，点击刷新"，点击后重试 |
| 登录失败（含验证码错误） | `message.error` 提示后端返回信息，并刷新验证码 |
| 网络错误 | 统一由 `request` 拦截器或登录页 catch 处理 |

## 边界情况

1. **组件快速卸载**：在 `useEffect` 返回清理函数中忽略已卸载组件的 setState。
2. **并发刷新**：刷新过程中设置 `loading` 防止重复点击。
3. **自动刷新导致输入丢失**：刷新验证码时通过 `onChange({ ..., captchaCode: '' })` 清空已输入内容。

## 依赖与前置条件

- 后端 `GET /api/auth/captcha` 与 `POST /api/auth/login` 已可用（当前已满足）。
- `frontend/src/types/api.d.ts` 中已定义 `CaptchaResponse` 类型，可作为参考，但组件内部使用 `src/api/auth.ts` 中定义的 `CaptchaResult`。

## 不纳入本次范围

- 后端验证码逻辑调整
- 其他页面（如重置密码、注册）的验证码复用（组件已预留复用能力）
- 验证码图片样式美化（仅保持与 Ant Design 输入框同高）

## 验收标准

- [ ] 登录页显示验证码输入框和图片，且图片可点击刷新
- [ ] 登录请求包含 `captchaUuid` 和 `captchaCode`
- [ ] 验证码错误时提示并自动刷新
- [ ] `npm run lint` 无新增告警
- [ ] 登录流程端到端可成功登录
