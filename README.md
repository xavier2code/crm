# CRM 管理系统

前后端分离的 CRM 系统基础框架。

## 技术栈

- 前端：React 19 + TypeScript 6 + Vite 8 + Ant Design 6 + TanStack Query 5 + Zustand 5
- 后端：Spring Boot 3.2 + Spring Security + JWT + MyBatis-Plus + Flyway + JDK 21
- 构建工具：Gradle 8.7
- 数据库：PostgreSQL（生产/外部开发环境），H2（默认本地开发）
- 缓存：Redis（可选）

## 项目结构

```
crm/
├── frontend/      # 前端工程（Vite + React）
├── backend/       # 后端工程（Spring Boot）
└── docker-compose.yml
```

## 模块

- 工作台
- 客户管理
- 商机管理
- 商务管理（合同）
- 后台管理（用户、角色、部门、字典）

## 角色权限

| 角色 | 权限 |
|------|------|
| ADMIN | 全部模块 |
| SALES | 工作台、客户管理、商机管理 |
| BUSINESS | 工作台、商务管理 |
| FINANCE | 工作台、报销管理 |

## 快速启动

### 前端

```bash
cd frontend
npm install
npm run dev          # 开发服务器，访问 http://localhost:8000
```

前端已配置 `/api` 代理到 `http://localhost:8080`。

### 后端

#### 方式一：默认本地开发（H2 + 无需 Redis）

```bash
cd backend
./gradlew bootRun
```

后端默认端口 `8080`，使用 H2 内存数据库并开启 `/h2-console`。

#### 方式二：连接外部 PostgreSQL + Redis

项目已提供 `application-dev-local.yml`，默认连接 `192.168.2.222`：

```bash
cd backend
./start-dev.sh
```

脚本会自动检查 PostgreSQL 连接、创建 `crm` 数据库（如不存在），然后启动服务。

如需自定义连接信息，可修改 `backend/src/main/resources/application-dev-local.yml` 或设置环境变量：

```bash
export DB_HOST=192.168.2.222
export DB_PORT=5432
export DB_NAME=crm
export DB_USERNAME=postgres
export DB_PASSWORD=123456
export REDIS_HOST=192.168.2.222
export REDIS_PORT=6379
export REDIS_PASSWORD=123456
export JWT_SECRET=your-32-byte-or-longer-secret-key

SPRING_PROFILES_ACTIVE=dev-local ./gradlew bootRun
```

### 全栈 Docker 启动

```bash
# 复制 .env.example 为 .env 并修改实际值
cp .env.example .env

docker-compose up --build
```

生产 compose 使用 PostgreSQL，数据库名 `crm_cy`，用户 `crm`。

## 开发规范

- 前端代码规范使用 ESLint + Prettier，提交前运行 `npm run lint`。
- 后端使用 Lombok，JDK 21。
- 前后端均使用 TypeScript/Java 类型生成与校验。

## 常用命令

### 前端

```bash
cd frontend
npm run dev        # 启动开发服务器
npm run build      # 类型检查 + 生产构建
npm run lint       # ESLint
npm run format     # Prettier 格式化
npm run preview    # 预览生产构建
npm run gen:api    # 从后端 /v3/api-docs 生成 TypeScript 类型
```

### 后端

```bash
cd backend
./start-dev.sh                       # 使用 dev-local 配置启动
./gradlew bootRun                    # 使用默认 dev 配置启动（H2）
./gradlew test                       # 运行全部测试
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest.createCustomer
./gradlew bootJar -x test            # 打包，跳过测试
```

## 接口文档

后端启动后访问：`http://localhost:8080/doc.html`（Knife4j）。

## 账号

本地开发默认账号：

- admin / 123456
- sales / 123456
- business / 123456
- finance / 123456
