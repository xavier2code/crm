# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

前后端分离的 CRM 管理系统基础框架。注意：`README.md` 中写的前端栈为 "Ant Design Pro / Umi Max"，但实际前端工程已切换为 **Vite + React 19 + TypeScript + Ant Design Pro Components**，README 此处已过时。

- 前端：`frontend/` — Vite 8 + React 19 + TypeScript 6 + Ant Design 6 + React Router 7 + TanStack Query 5 + Zustand 5
- 后端：`backend/` — Spring Boot 3.2 + Spring Security + JWT + MyBatis-Plus + Flyway + JDK 21 + Gradle 8.7
- 数据库：生产用 PostgreSQL，开发/测试用 H2（模拟 PostgreSQL 模式）
- 可选缓存：Redis（默认未启用）

## 常用命令

### 前端

```bash
cd frontend
npm install
npm run dev        # 开发服务器，端口 8000，/api 代理到 localhost:8080
npm run build      # 类型检查 + 生产构建，输出到 dist/
npm run lint       # ESLint（--max-warnings 0）
npm run format     # Prettier 格式化 src/**/*.{ts,tsx,css,json}
npm run preview    # 预览生产构建
```

### 后端

```bash
cd backend
./gradlew bootRun                # 启动服务，默认 dev  profile，H2 内存数据库
./gradlew test                   # 运行全部测试
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest.createCustomer
./gradlew bootJar -x test        # 打包，跳过测试
./gradlew dependencies           # 查看依赖树
```

### 全栈 Docker 启动

```bash
# 需要先创建 .env（复制 .env.example）
docker-compose up --build
```

生产 compose 使用 PostgreSQL，数据库名 `crm_cy`，用户 `crm`。

## 开发环境说明

- 后端 `application.yml` 默认启用 dev profile，使用 H2 内存数据库并开启 `h2-console`（`/h2-console`），本地开发不需要安装 PostgreSQL。
- 如果开发环境使用外部 PostgreSQL / Redis，已提供 `application-dev-local.yml`：
  - 默认连接 `192.168.2.222:5432/crm`，用户名 `postgres`，密码 `123456`。
  - Redis 连接 `192.168.2.222:6379`，密码 `123456`。
  - 启动方式：`cd backend && ./start-dev.sh` 或 `SPRING_PROFILES_ACTIVE=dev-local ./gradlew bootRun`。
- 前端 `vite.config.ts` 中已配置 `/api` 代理到 `http://localhost:8080`。
- JWT 密钥通过环境变量 `JWT_SECRET` 注入；本地未设置时为空字符串，测试环境使用 `application-test.yml` 中的固定测试密钥。
- 后端公开端点：`/api/auth/**`、Knife4j/Swagger 文档路径；其余接口需 JWT。

## 后端架构

### 包结构

按业务模块组织，位于 `com.cy.crm.module.*`，每个模块通常包含：

- `controller` — REST API 入口
- `service` — 业务逻辑
- `mapper` — MyBatis-Plus Mapper 接口
- `entity` — 数据实体
- `dto` — 请求/命令对象
- `vo` — 响应视图对象
- `converter` — MapStruct 转换器

公共基础设施集中在 `com.cy.crm.common.*`、`com.cy.crm.config.*`、`com.cy.crm.security.*` 和 `com.cy.crm.aspect.*`。

### 安全与认证

- `JwtAuthenticationFilter` 从 `Authorization: Bearer <token>` 解析 JWT。
- `SecurityConfig` 配置无状态会话、CORS、公开端点和基于角色的 URL 授权；`@EnableMethodSecurity(securedEnabled = true)` 支持方法级安全注解。
- 角色常量见 `RoleConstants`：ADMIN、SALES、BUSINESS、FINANCE。

### 数据权限

数据权限是核心业务约束，分两层实现：

1. **列表级过滤**：`DataScopeInterceptor`（MyBatis-Plus `InnerInterceptor`）在 SELECT 执行前，使用 JSqlParser 将当前用户的 `DataScope` 条件注入 SQL WHERE 子句，避免字符串拼接。受控表在拦截器内按表配置（`t_customer`、`t_opportunity`、`t_order`、`t_project`、`t_rebate` 等）。
2. **资源级校验**：`DataScopeValidator` 在 Service 层对单条记录访问做显式校验（`validateAccess`、`validateUnitAccess`、`validateChannelAccess`、`validateCreatorAccess`），防止 IDOR。

`DataScope` 包含 `all`、`selfOnly`、`unitIds`、`channelIds`、`regions` 五个维度，来源于 JWT claims，不在运行时重建。

### 审计日志

- `@AuditLog` 注解标记需要记录的方法，`AuditLogAspect` 环绕记录：用户、IP、类/方法、参数、执行时间、是否异常。
- 参数中敏感字段（password、token、captcha 等）会被自动脱敏为 `******`。
- 记录动作异步写入（`auditLogExecutor`），在主线程提前抽取 request 上下文避免异步丢失。

### 实体与持久化

- 所有实体继承 `BaseEntity`，统一包含 `id`（自增）、`createdAt`、`updatedAt`、`isDeleted`（逻辑删除）、`version`（乐观锁）。
- MyBatis-Plus 全局配置：逻辑删除字段 `is_deleted`，下划线转驼峰，ID 类型 `auto`。
- 数据库迁移脚本位于 `backend/src/main/resources/db/migration/`，Flyway 在启动时自动执行。

### 异常与响应

- 统一响应结构 `ApiResult<T>`（`code`/`success`/`message`/`data`）。
- `GlobalExceptionHandler` 处理业务异常、参数校验、认证授权、数据库约束冲突、乐观锁等，映射到预定义错误码（开发文档 §34）。
- 业务异常统一使用 `BusinessException`。

## 前端架构

### 路由与页面

- `src/router/index.tsx` 使用 `createBrowserRouter`，页面按路由懒加载（`React.lazy`），`AuthGuard` 在 token 缺失时重定向到 `/login`。
- 布局：`src/layouts/BasicLayout.tsx`，菜单渲染逻辑在 `src/constants/menus.tsx`。
- 页面目录：`src/pages/dashboard`、`customer`、`opportunity`、`business`、`reimbursement`、`system/*`。

### 状态与数据请求

- 全局认证状态：`src/stores/auth.ts`（Zustand + persist），token 同时写入 `localStorage` 的 `crm-token`。
- API 客户端：`src/api/client.ts`（Axios），请求时自动附加 Bearer token，响应统一解析 `ApiResult`，401 时清除 token 并跳转登录。
- 服务端状态管理：TanStack Query（`QueryClient` 在 `App.tsx` 中配置，默认不 retry、不窗口聚焦刷新）。

### 代码规范

- ESLint 配置：`frontend/eslint.config.js`，使用 `typescript-eslint` + react + react-hooks + import/order。
- Prettier 配置：`frontend/.prettierrc`。
- 路径别名 `@/` 映射到 `src/`（Vite + TypeScript 均已配置）。

## 测试

- 后端测试使用 JUnit 5 + Spring Boot Test + Testcontainers（PostgreSQL）+ H2。测试配置在 `backend/src/test/resources/application-test.yml`。
- 运行单个测试或单个方法使用 `--tests <ClassName>` 或 `--tests <ClassName.methodName>` 语法。
- 当前前端未配置测试框架。

## 文档与参考

- 根目录 `README.md`：基础启动说明，但前端技术栈描述已过时，应以 `frontend/package.json` 和 `vite.config.ts` 为准。
- `CRM-渠道版-开发文档.md`：较完整的开发规范，涉及错误码、数据库设计、数据权限、审计日志等细节，复杂改动前建议查阅。
- 后端 API 文档：启动后访问 `http://localhost:8080/doc.html`（Knife4j）。
