# CRM 管理系统

[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.7-02303A?logo=gradle)](https://gradle.org/)
[![React](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?logo=vite)](https://vitejs.dev/)
[![Ant Design](https://img.shields.io/badge/Ant%20Design-6-1677FF?logo=antdesign)](https://ant.design/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-optional-DC382D?logo=redis)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-supported-2496ED?logo=docker)](https://www.docker.com/)

前后端分离的 CRM 系统基础框架。

## 技术栈

- **前端**：React 19 + TypeScript 6 + Vite 8 + Ant Design 6 + React Router 7 + TanStack Query 5 + Zustand 5
- **后端**：Spring Boot 3.2 + Spring Security + JWT + MyBatis-Plus + Flyway + JDK 21
- **构建工具**：Gradle 8.7
- **数据库**：PostgreSQL
- **缓存**：Redis（可选，默认未启用）

## 项目结构

```
crm/
├── frontend/              # 前端工程（Vite + React + TypeScript）
│   ├── src/
│   │   ├── api/           # API 请求
│   │   ├── components/    # 公共组件
│   │   ├── layouts/       # 页面布局
│   │   ├── pages/         # 页面
│   │   ├── router/        # 路由配置
│   │   ├── stores/        # Zustand 状态管理
│   │   └── utils/         # 工具函数
│   └── package.json
├── backend/               # 后端工程（Spring Boot）
│   ├── src/main/java/     # Java 源码
│   ├── src/main/resources/db/migration/  # Flyway 迁移脚本
│   └── build.gradle
├── docker-compose.yml     # 全栈 Docker 启动
└── README.md
```

## 功能模块

- 工作台（Dashboard）
- 客户管理
- 商机管理
- 项目管理
- 商务管理（合同、返利）
- 系统管理（用户、角色、字典、单位、渠道、审计日志）

## 角色权限

| 角色 | 权限 |
|------|------|
| ADMIN | 系统管理、全部业务模块 |
| CYBD | 内部管理员、商机报备审批、全部商机查看 |
| REGION_HEAD | 大区总、业务侧 4 级单位/渠道分配 |
| CHANNEL_HEAD | 渠道负责人、渠道管理、本单位返利确认 |
| CHANNEL_BD | 渠道 BD、我的报备、客户管理、商机创建/编辑/提交 |
| ORANGE_EAGLE_SALES | 橙鹰销售、仅查看 |
| ORANGE_EAGLE_HEAD | 橙鹰负责人、单位主数据维护 |
| FINANCE | 财务管理、返利付款、报销审批 |

## 快速启动

### 前端

```bash
cd frontend
npm install
npm run dev          # 开发服务器，访问 http://localhost:8000
```

前端已配置 `/api` 代理到 `http://localhost:8080`。

### 后端

#### 方式一：默认本地开发（连接本地 PostgreSQL，无需 Redis）

确保本地已启动 PostgreSQL 并创建数据库 `crm`（默认用户/密码：`crm`/`crm`）：

```bash
psql -U postgres -c "CREATE DATABASE crm;"
psql -U postgres -c "CREATE USER crm WITH PASSWORD 'crm';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE crm TO crm;"
```

```bash
cd backend
JWT_SECRET="your-32-byte-or-longer-secret-key" ./gradlew bootRun
```

后端默认端口 `8080`，使用 `jdbc:postgresql://localhost:5432/crm`。

> 注意：`JWT_SECRET` 必须不少于 32 字节，否则应用启动会失败。

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
- 数据库迁移脚本放在 `backend/src/main/resources/db/migration/`，Flyway 启动时自动执行。

## 常用命令

### 前端

```bash
cd frontend
npm run dev        # 启动开发服务器
npm run build      # 类型检查 + 生产构建
npm run lint       # ESLint
npm run format     # Prettier 格式化
npm run preview    # 预览生产构建
```

### 后端

```bash
cd backend
./start-dev.sh                       # 使用 dev-local 配置启动
./gradlew bootRun                    # 使用默认 dev 配置启动（连接 localhost PostgreSQL）
./gradlew test                       # 运行全部测试
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest
./gradlew test --tests com.cy.crm.module.customer.controller.CustomerControllerTest.createCustomer
./gradlew bootJar -x test            # 打包，跳过测试
```

## 接口文档

后端启动后访问：`http://localhost:8080/doc.html`（Knife4j）。

## 默认账号

本地开发默认账号：

- admin / 123456
- sales / 123456
- business / 123456
- finance / 123456

> 开发环境未启用 Redis 时，验证码校验会被跳过，任意输入即可通过。
