# CLAUDE.md

Claude Code 处理本仓库时的快速参考。

## 技术栈

- 前端：`frontend/` — Vite 8 + React 19 + TypeScript 6 + Ant Design 6 + React Router 7 + TanStack Query 5 + Zustand 5
- 后端：`backend/` — Spring Boot 3.2 + Spring Security + JWT + MyBatis-Plus + Flyway + JDK 21 + Gradle 8.7
- 数据库：生产用 PostgreSQL，开发默认用 H2（模拟 PostgreSQL 模式）
- 缓存：Redis 可选，默认未启用

## 常用命令

### 前端

```bash
cd frontend
npm install
npm run dev        # http://localhost:8000，/api 代理到 localhost:8080
npm run build      # tsc + vite build
npm run lint       # ESLint --max-warnings 0
npm run format     # Prettier
```

### 后端

```bash
cd backend
JWT_SECRET="your-32-byte-secret" ./gradlew bootRun   # 默认 dev profile，H2
./gradlew test
./gradlew bootJar -x test
```

### Docker

```bash
cp .env.example .env
docker-compose up --build
```

## 关键约定

- 后端包结构按模块组织：`com.cy.crm.module.*`
- 角色常量：`RoleConstants`（ADMIN、SALES、BUSINESS、FINANCE）
- 数据权限：`DataScope` 来自 JWT claims；列表过滤用 `DataScopeInterceptor`，单条校验用 `DataScopeValidator`
- 统一响应：`ApiResult<T>`；业务异常用 `BusinessException`
- 实体基类：`BaseEntity`（id、createdAt、updatedAt、isDeleted、version）
- Flyway 迁移：`backend/src/main/resources/db/migration/`
- 前端状态：`useAuthStore` / `useMenuStore` 均使用 `zustand/persist`，默认存 `localStorage`
- 路径别名：`@/` 映射到 `src/`

## 参考文档

- 根目录 `README.md`
- `CRM-渠道版-开发文档.md`
- 后端 API 文档：启动后访问 `http://localhost:8080/doc.html`
