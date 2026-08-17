# Levi — Backend

A multi-tenant, Jira-style project management API built with Spring Boot. Organizations, projects, Kanban boards, sprints, issue assignment, role-based permissions, live updates over WebSocket, and shift-aware productivity tracking.

The companion frontend lives at [levi-frontend](https://github.com/sengarsumit/levi-frontend).

## Features

- **Organizations & membership** — create an org, invite or request to join, admin approval workflow with an optional role preference from the requester
- **Projects → Boards → Workflow states → Issues**, with sprints for backlog planning
- **Project-level membership** — non-admins only see and access the projects they've been added to; org admins see everything
- **Kanban board** with live drag-and-drop updates pushed to every connected viewer over WebSocket
- **Role-based access control** — an `Admin` role plus fully custom, extensible roles with per-permission checkboxes
- **Issue assignment** with assignee-scoped visibility (non-admins see their own work by default)
- **Shift-aware time tracking** — `startedAt` → `completedAt` elapsed time computed against each member's configured working hours and days, not wall-clock time
- **Productivity dashboard data** — per-project completion counts and average time-to-complete, aggregated per teammate
- **Comments** on issues
- **JWT auth** (access + refresh tokens) with row-level tenant isolation enforced on every request

## Tech stack

- **Java 21**, **Spring Boot 3.3.5**
- Spring Web, Spring Security (stateless JWT), Spring Data JPA (Hibernate), Spring WebSocket (STOMP over SockJS)
- **MySQL** (via `mysql-connector-j`)
- **Bucket4j** for in-memory per-IP rate limiting
- **jjwt** for JWT signing/verification
- Lombok
- Packaged as a multi-stage Docker image; deployed on [Render](https://render.com)

## Architecture

Base package: `com.koda.levi`. Standard layering: `controller` → `service` → `repository` (Spring Data JPA), with `entity` for JPA models and `dto` for request/response payloads.

### Multi-tenancy

Tenant isolation is row-level via an `organization_id` column, enforced on every request, not just at the query-authoring level:

- `tenancy/TenantContext` — a `ThreadLocal<Long>` holding the current tenant (organization) id for the request.
- `tenancy/TenantContextInterceptor` — resolves the tenant id from the `X-Tenant-ID` header or the JWT's `tenantId` claim, rejects requests where the two disagree, and 400s tenant-scoped routes that supply neither. A handful of bootstrap routes (auth, org creation/listing, org join and its role-preview endpoint) are exempt.
- `tenancy/TenantHibernateFilterInterceptor` — enables a Hibernate `@Filter` on the session so JPA queries are transparently scoped to the current tenant.
- `tenancy/TenantAccessService` — a service-layer helper for code paths that need to assert or compare tenant id explicitly.

### Auth & RBAC

JWT is stateless (CSRF disabled, `/auth/**` and `/ws-levi/**` public, everything else authenticated). Access and refresh token lifetimes are configurable; refresh tokens are persisted, not just signed and trusted. A token embeds a coarse system-level role plus, when the user belongs to an org, that org's `tenantId`, slug, org-level role, and permission set — but permissions are **re-derived from the database on every tenant-scoped request** rather than trusted from the token claim, so a promotion or role change takes effect immediately instead of waiting for the token to expire.

Org-level roles (`Role`, with an arbitrary `Set<String>` of permission names) are separate from the coarse system-level Spring Security role and fully custom per organization — an org admin can define new roles with any combination of the app's permission checks.

### Domain model

`Organization` → `Project` → `Board` → `WorkflowState`, with `Issue` belonging to a `Project` and, via workflow, a `Board`/`WorkflowState`. `Sprint` groups issues within a project. `Comment` attaches to issues. `OrganizationUser` is the join entity between users and organizations, carrying role, membership status (pending/active), and per-user shift fields used for time-tracking. `ProjectMember` is a second, project-scoped membership layer on top of org membership, gating which projects a non-admin can see.

### Real-time updates

`WebSocketConfig` wires STOMP over SockJS at `/ws-levi` (simple broker on `/topic`, app prefix `/app`), paired with an async event listener so an issue move or update on one client is pushed to every other connected viewer without a poll.

### Keeping the free-tier deployment warm

Render's and Aiven's free tiers both idle down after a period of inactivity. A `@Scheduled` job pings the database every 5 minutes to stop it auto-pausing, and a GitHub Actions workflow (`.github/workflows/keep-alive.yml`) pings the backend's own health endpoint every 10 minutes to stop the web service itself from sleeping.

## API surface

All routes below except `/auth/**`, the org-bootstrap routes (`POST`/`GET /organization`, `.../join`, `.../join/roles`), and the WebSocket handshake require a valid JWT and a resolvable tenant id.

| Controller | Base path | Notable routes |
|---|---|---|
| `AuthController` | `/auth` | `GET /health`, `POST /signup`, `POST /login`, `POST /refresh`, `POST /logout` |
| `OrgController` | `/organization` | CRUD, `POST /{orgSlug}/join`, `GET /{orgSlug}/join/roles`, member approval/rejection, invites, custom roles |
| `ProjectController` | `/projects` | CRUD, `GET /organization/{orgId}`, `GET/POST/DELETE /{projectId}/members` |
| `BoardController` | `/boards` | CRUD, `GET /project/{projectId}` |
| `WorkflowStateController` | `/workflow-states` | CRUD, `GET /board/{boardId}` |
| `IssueController` | `/issues` | CRUD, filter/search, `GET /project/{projectId}/backlog`, `GET /project/{projectId}/productivity`, `PATCH /{id}/status`, `PATCH /{id}/move`, `PATCH /{id}/assign` |
| `SprintController` | `/sprints` | CRUD, `GET /project/{projectId}` |
| `CommentController` | `/comments` | `POST`, `GET /issue/{issueId}`, `PUT/{id}`, `DELETE/{id}` |
| `UserController` | `/users` | `GET` |

Errors go through a global exception handler, returning a structured error body for not-found, bad-request, unauthorized, validation, and generic failures.

## Getting started

### Prerequisites

- Java 21
- Maven (the repo includes the `mvnw`/`mvnw.cmd` wrapper, so a local Maven install isn't required)
- A local MySQL instance
- Docker, for Redis (currently provisioned but not yet wired into caching — see [Configuration](#configuration))

### Local setup

1. Start Redis:
   ```bash
   docker-compose up -d
   ```
2. Create a MySQL database named `levi`.
3. Create `src/main/resources/application.yaml` (git-ignored, so this file is local-only and never committed):
   ```yaml
   spring:
     application:
       name: levi
     datasource:
       url: jdbc:mysql://localhost:3306/levi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
       username: <your-mysql-user>
       password: <your-mysql-password>
       driver-class-name: com.mysql.cj.jdbc.Driver
     jpa:
       hibernate:
         ddl-auto: update
       show-sql: true
       properties:
         hibernate:
           dialect: org.hibernate.dialect.MySQL8Dialect
   app:
     jwt:
       secret: <a long random string — e.g. `openssl rand -hex 32`>
       expiration-ms: 900000
       refresh-expiration-ms: 604800000
   ```
4. Run the app:
   ```bash
   ./mvnw spring-boot:run       # macOS/Linux
   mvnw.cmd spring-boot:run     # Windows
   ```
   The API is served on `http://localhost:8080`. Hibernate's `ddl-auto: update` creates and evolves the schema automatically — no separate migration step.

### Commands

```bash
./mvnw clean install                        # full build
./mvnw test                                 # run all tests
./mvnw test -Dtest=LeviApplicationTests      # run a single test class
./mvnw spring-boot:run                       # run the app locally
```

## Configuration

The Docker/production profile (`application-docker.yaml`) is fully environment-variable driven:

| Variable | Purpose | Default |
|---|---|---|
| `SPRING_DATASOURCE_URL` | JDBC URL for the production database | — (required) |
| `SPRING_DATASOURCE_USERNAME` | Database username | — (required) |
| `SPRING_DATASOURCE_PASSWORD` | Database password | — (required) |
| `APP_JWT_SECRET` | HMAC signing secret for JWTs — generate with `openssl rand -hex 32` | — (required) |
| `APP_JWT_EXPIRATION_MS` | Access token lifetime, in milliseconds | `900000` (15 min) |
| `APP_JWT_REFRESH_EXPIRATION_MS` | Refresh token lifetime, in milliseconds | `604800000` (7 days) |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed frontend origins, no trailing slash | `http://localhost:5173` |
| `PORT` | Port the server listens on | `8080` |

Note: `docker-compose.yml` provisions Redis for future caching/rate-limiting use, but the app's `CacheConfig` currently uses an in-memory `ConcurrentMapCacheManager` — Redis isn't wired into caching yet.

## Testing

There is currently one automated test (`LeviApplicationTests`), a Spring context-load smoke test confirming the application boots with its full bean graph and configuration. It does not exercise API behavior.

## Deployment

The backend deploys to [Render](https://render.com) as a Docker web service, defined declaratively in `render.yaml`. See [DEPLOYMENT.md](./DEPLOYMENT.md) for the full walkthrough, including provisioning a managed MySQL instance and wiring CORS up to the deployed frontend's origin.

## Project structure

```
src/main/java/com/koda/levi/
├── config/        # Security, CORS, WebSocket, caching, async, scheduling
├── controller/     # REST endpoints
├── service/        # Business logic
├── repository/      # Spring Data JPA repositories
├── entity/          # JPA entities
├── dto/             # Request/response payloads
├── tenancy/         # Multi-tenant context resolution and enforcement
├── event/            # Application events (e.g. issue updates)
├── listener/          # Event listeners (e.g. WebSocket notification push)
├── exception/          # Custom exceptions + global exception handler
├── migration/           # One-time, self-guarded data backfills
└── scheduler/            # Scheduled jobs (e.g. database keep-alive ping)
```
