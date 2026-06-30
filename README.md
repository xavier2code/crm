# CRM 管理系统

前后端分离的 CRM 系统基础框架。

## 技术栈

- 前端：React 19 + TypeScript + Ant Design Pro / Umi Max
- 后端：Spring Boot 3.2 + Spring Security + JWT + JPA + PostgreSQL
- 构建工具：Gradle 8.7
- JDK：21

## 项目结构

```
crm/
├── frontend/      # 前端工程
└── backend/       # 后端工程
```

## 模块

- 工作台
- 客户管理
- 商机管理
- 商务管理（合同、开票）
- 报销管理
- 后台管理（用户、角色、部门）

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
npm start          # 默认启用 mock，访问 http://localhost:8000
```

Mock 账号：
- admin / 123456
- sales / 123456
- business / 123456
- finance / 123456

### 后端

环境要求：JDK 21、PostgreSQL。

```bash
cd backend

# 创建数据库（首次）
createdb -U postgres crm

# 启动服务
./gradlew bootRun
```

后端默认端口 `8080`，数据库连接配置在 `src/main/resources/application.yml`，可通过环境变量覆盖：

```bash
export DB_USERNAME=crm
export DB_PASSWORD=crm
export JWT_SECRET=your-secret-key
./gradlew bootRun
```

## 开发规范

- 前端代码规范使用 Biome，提交前运行 `npm run lint`。
- 后端使用 Lombok，JDK 17+。
