# Repository Guidelines

A Chinese-language CRM for the "CY 渠道" sales channel. Monorepo: Spring
Boot backend + Vite/React frontend, sharing PostgreSQL or H2 via Flyway.
Business rules live in `CRM-渠道版-开发文档.md` — read it before touching
business code.

## Project Structure & Module Organization

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
└── CRM-渠道版-开发文档.md                      # 业务规则权威来源
```

## Build, Test, and Development Commands

**Backend** (`backend/`): `./start-dev.sh` for the `dev-local` profile
(external PG/Redis); `./gradlew bootRun` for H2 (needs `JWT_SECRET` env,
≥32 bytes); `./gradlew test` for all tests; `./gradlew test --tests
<FQN>` for one class; `./gradlew bootJar -x test` to build a jar.

**Frontend** (`frontend/`): `npm run dev` for the Vite dev server
(`http://localhost:8000`, proxies `/api` → `:8080`); `npm run build` for
type-check + production build; `npm run lint` (fails on warnings,
`--max-warnings 0`); `npm run format` for Prettier; `npm run gen:api` to
regenerate `src/types/api.d.ts` from backend OpenAPI.

## Coding Style & Naming Conventions

- **Java**: 4-space indent, Lombok (`@Data`, `@RequiredArgsConstructor`),
  MapStruct for DTO ↔ entity ↔ VO. Package layout
  `com.cy.crm.module.<feature>.{controller,service,…}`. Use the
  `BusinessException` helpers (`.resourceNotFound()`, `.forbidden()`,
  `.paramError()`) and segment-scoped error codes (1xxx generic, 2xxx
  auth, 3xxx admin/customer, 4xxx opportunity, 5xxx project, 6xxx
  rebate/contract).
- **TypeScript/React**: 2-space indent, no semicolons, single quotes,
  trailing comma `es5`, 100-char lines (Prettier). Function components
  only. Pages live in `src/pages/<route>/index.tsx` and lazy-load via
  `src/router/index.tsx`. API files export typed wrappers around
  `@/api/client.request`. Linting must pass with zero warnings.

## Testing Guidelines

- **Backend**: JUnit 5 + Mockito, TDD-style names (`shouldXxxWhenYyy`).
  One test class per service. Always cover permission checks — the
  codebase relies on `DataScopeValidator` and `DataScopeInterceptor` for
  IDOR protection.
- **Frontend**: no automated tests configured. Verify with `npm run
  build` and manual smoke testing.

## Commit & Pull Request Guidelines

- **Conventional Commits**: `<type>(<scope>): <subject>` — types in
  history: `fix`, `feat`, `chore`, `docs`, `refactor`, `style`, `test`.
  English, imperative mood, ≤72-char subject.
- Before opening a PR: `npm run lint && npm run build` (frontend) and
  `./gradlew test` (backend). PRs describe the change, link any issue,
  attach screenshots for UI work, and cite the relevant section of
  `CRM-渠道版-开发文档.md` when adding features.

## Security & Configuration Tips

- Never commit secrets; copy `.env.example` → `.env`. `JWT_SECRET` must
  be ≥32 bytes. Default profile is H2; switch to PostgreSQL via
  `SPRING_PROFILES_ACTIVE=dev-local`. Redis is optional — when disabled,
  the captcha on `/api/auth/login` is bypassed.
- New DB changes need a numbered Flyway script in
  `backend/src/main/resources/db/migration/`. Never edit a script that
  has already shipped to a shared environment.
