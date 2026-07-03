# Development Guide

A Chinese-language CRM for the "CY 渠道" sales channel. Monorepo: Spring Boot 3.2 backend + Vite/React 19 frontend, sharing PostgreSQL or H2 via Flyway. Business rules live in `CRM-渠道版-开发文档.md` — read it before touching business code.

## 1. Project Structure

```
crm/
├── backend/                                  # Spring Boot 3.2, JDK 21, MyBatis-Plus
│   ├── src/main/java/com/cy/crm/
│   │   ├── common/  config/  security/       # exception, JWT, data scope
│   │   └── module/<feature>/                 # controller, service, mapper,
│   │                                         # entity, dto, vo, converter
│   ├── src/main/resources/{application*.yml,
│   │   db/migration/V<n>__*.sql, mapper/*.xml}
│   └── src/test/java/                        # JUnit 5
├── frontend/                                 # Vite 8, React 19, TS 6, AntD 6
│   └── src/{api,components,hooks,pages,
│            router,stores,utils,types}/
├── docker-compose.yml
└── CRM-渠道版-开发文档.md                      # business rules (authoritative)
```

## 2. Quick Commands

| Task | Command |
|---|---|
| Frontend dev server | `cd frontend && npm run dev` (http://localhost:8000, proxies /api → :8080) |
| Frontend build | `cd frontend && npm run build` (type-check + production build) |
| Frontend lint | `cd frontend && npm run lint` (fails on warnings) |
| Frontend format | `cd frontend && npm run format` |
| Regenerate API types | `cd frontend && npm run gen:api` (requires backend on :8080) |
| Backend dev (H2) | `cd backend && JWT_SECRET=<32+chars> ./gradlew bootRun` |
| Backend dev (PG/Redis) | `cd backend && ./start-dev.sh` (also supports `stop`, `restart`, `status`) |
| Backend test all | `cd backend && ./gradlew test` |
| Backend test one class | `cd backend && ./gradlew test --tests <FQN>` |
| Backend build jar | `cd backend && ./gradlew bootJar -x test` |
| Full-stack Docker | `cp .env.example .env && SPRING_PROFILES_ACTIVE=prod docker-compose up --build` |

## 3. Architecture

### Backend Layer Conventions

- **Controllers**: `@RestController` + `@Validated`, return `ApiResult<T>`. Authorization via `@PreAuthorize` with role constants from `RoleConstants`.
- **Services**: extend `ServiceImpl<Mapper, Entity>`. Business exceptions use `BusinessException.*` helpers (`.resourceNotFound()`, `.forbidden()`, `.paramError()`).
- **DTOs/VOs**: mapped with MapStruct converters (`*Converter`). Lombok `@Data` is standard.
- **Entities**: extend `BaseEntity` (id, createdAt, updatedAt, isDeleted, version) or `AuditableEntity` (adds createdBy). MyBatis-Plus handles soft deletes (`is_deleted`), optimistic locking (`version`), and auto-fill fields.

### Key Runtime Behaviors

- `DataScopeInterceptor` intercepts SELECT statements for configured tables (`t_customer`, `t_opportunity`, `t_project`, `t_rebate`) and injects row-level filters based on the current user's `DataScope`. Complex queries (UNION, CTE) are rejected.
- Services also call `DataScopeValidator.validateAccess(...)` for IDOR protection on single-resource endpoints.
- Default profile is H2 with `/h2-console` enabled. Redis is optional — when disabled, login captcha is bypassed.
- Flyway migrations: add new numbered scripts in `db/migration/`; never edit already-shipped scripts.
- API docs available at `http://localhost:8080/doc.html` when backend is running.

### Frontend Structure

- `api/` — Axios wrappers. `api/client.ts` exports `request<T>()`, attaches JWT from `localStorage` (`crm-token`), maps business codes to UI behavior (401/2001/2003 → logout, 2007 → change-password).
- `hooks/` — TanStack Query wrappers (`useQuery`/`useMutation`) per feature.
- `pages/` — page components, one route per `pages/<route>/index.tsx`, lazy-loaded via `router/index.tsx`.
- `stores/` — Zustand stores: `auth` (token/user/roles/permissions), `menu`, `dict`.
- `types/api.d.ts` — generated from backend OpenAPI; regenerate with `npm run gen:api`.

## 4. Coding Conventions

### Java

- 4-space indent. Lombok (`@Data`, `@RequiredArgsConstructor`).
- Package layout: `com.cy.crm.module.<feature>.{controller,service,mapper,entity,dto,vo,converter}`.
- Error code segments: 1xxx generic, 2xxx auth, 3xxx admin/customer, 4xxx opportunity, 5xxx project, 6xxx rebate/contract.

### TypeScript / React

- 2-space indent, no semicolons, single quotes, trailing comma `es5`, 100-char lines (Prettier).
- Function components only. Routes lazy-load via `React.lazy`.
- API types from `components['schemas']['...']` in `src/types/api.d.ts`.
- API files export typed wrappers around `@/api/client.request`.
- ESLint fails on warnings; run `npm run lint && npm run build` before considering frontend work complete.

## 5. Testing

- **Backend**: JUnit 5 + Mockito. TDD-style names (`shouldXxxWhenYyy`). One test class per service. Always cover permission/data-scope behavior when modifying protected resources.
- **Frontend**: no automated tests. Verify with `npm run build` and manual smoke testing.

## 6. Git Worktree Workflow

Every multi-file change **must** be developed in a dedicated git worktree on its own branch. Do **not** commit to `main` directly.

Use `<repo-root>` as placeholder for the path to the repository root.

1. **Create** from `main`:
   ```bash
   cd <repo-root> && git fetch origin
   git worktree add <repo-root>/.worktree/<branch-name> -b <branch-name> origin/main
   ```
2. **Develop** entirely inside the worktree. Do not edit files in `<repo-root>` while the worktree is open.
3. **Commit** using Conventional Commits. Validate before committing:
   - Backend: `cd backend && ./gradlew test`
   - Frontend: `cd frontend && npm run lint && npm run build`
4. **Merge** back to `main` from the repo root:
   ```bash
   cd <repo-root> && git checkout main
   git pull --ff-only origin main
   git merge --no-ff <branch-name>
   git push origin main
   ```
   If `main` moved, rebase first, re-run validation, then merge.
5. **Clean up**:
   ```bash
   cd <repo-root>
   git worktree remove <repo-root>/.worktree/<branch-name>
   git branch -d <branch-name>
   ```

**Rules**: One worktree per task. Never commit to `main` directly. If two agents touch the same file, coordinate via PR description or serialize through branches.

## 7. Commit & PR Guidelines

- **Conventional Commits**: `<type>(<scope>): <subject>` — English, imperative mood, ≤72 chars.
- Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `style`, `test`.
- Before PR: `npm run lint && npm run build` (frontend) and `./gradlew test` (backend).
- PRs describe the change, link issues, attach screenshots for UI work, and cite relevant sections of `CRM-渠道版-开发文档.md`.

## 8. Security & Configuration

- Never commit secrets. Copy `.env.example` → `.env`. `JWT_SECRET` must be ≥32 bytes.
- Default Spring profile is `dev` (H2). Switch to PostgreSQL/Redis via `SPRING_PROFILES_ACTIVE=dev-local` or `./start-dev.sh`.
- Redis is optional — when disabled, captcha on `/api/auth/login` is bypassed.
- New DB changes need a numbered Flyway script. Never edit a script that has shipped to a shared environment.

### Default Local Credentials (H2 profile)

- `admin` / `123456`
- `sales` / `123456`
- `business` / `123456`
- `finance` / `123456`
