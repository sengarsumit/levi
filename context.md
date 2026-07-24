# Levi Project Context

## Project overview

Levi is a Java 21 Spring Boot 3.3.5 REST API for a lightweight project/issue tracking domain. It uses Spring Web, Spring Data JPA, Spring Security, JWT authentication, MySQL, Lombok, and Maven.

Main package: `com.koda.levi`

Build file: `pom.xml`

Application entry point: `src/main/java/com/koda/levi/LeviApplication.java`

Runtime config: `src/main/resources/application.yaml`

## Build and runtime

### Maven coordinates

- Group: `com.koda`
- Artifact: `levi`
- Version: `0.0.1-SNAPSHOT`
- Java: `21`
- Spring Boot parent: `3.3.5`

### Important dependencies

- `spring-boot-starter-web`
- `spring-boot-starter-data-jpa`
- `spring-boot-starter-security`
- `mysql-connector-j`
- `io.jsonwebtoken:jjwt-*` version `0.12.6`
- `lombok`
- `spring-boot-starter-test`

### Configuration

`application.yaml` currently defines:

- Spring application name: `levi`
- MySQL datasource targeting local database `levi`
- JPA `ddl-auto: update`
- SQL logging enabled
- Hibernate dialect explicitly set to `org.hibernate.dialect.MySQL8Dialect`
- JWT properties:
  - `app.jwt.secret`
  - `app.jwt.expiration-ms`

Do not commit real database passwords or production JWT secrets. Move them to environment variables, local-only config, or a secret manager before using this outside local development.

## Domain model

### `Users`

Entity table: `users`

Fields:

- `UUID id`
- `name`
- `email`
- `password`
- `role`, defaulting to `ROLE_USER`
- `createdAt`
- `updatedAt`

Implements `UserDetails`, so it can be used directly with Spring Security. The service layer currently maps database users into Spring Security `User` instances through `CustomUserDetailsService`.

### `Organization`

Entity table: `organization`

Fields:

- `long id`
- `name`
- `createdAt`
- `updatedAt`
- `List<Projects> projects`

Relationship:

- One organization has many projects.

### `Projects`

Entity table: `projects`

Fields:

- `long id`
- `name`
- `deadline`
- `createdBy`
- `createdAt`
- `updatedAt`
- `List<Issue> issue`
- `Organization organization`

Relationships:

- Many projects belong to one organization.
- One project has many issues.

### `Issue`

Entity table: `issue`

Fields:

- `int id`
- `title`
- `description`
- `status`
- `issueType`
- `assignedTo`
- `assignedBy`
- `Projects projects`
- `createdAt`
- `updatedAt`

Relationship:

- Many issues belong to one project.

### `Comment`

Entity table: `comment`

Fields:

- `Long id`
- `comment`
- `issueId`
- `replyTo`
- `userId`
- `createdAt`
- `updatedAt`

There is currently no repository, service, controller, or JPA relationship wiring for comments.

## API surface

All endpoints except `/auth/**` require authentication through the configured security filter chain.

### Authentication

Controller: `AuthController`

- `POST /auth/signup`
- `POST /auth/login`

Both return `AuthResponse` containing a JWT token.

### Users

Controller: `UserController`

- `GET /users`

The service also has `getUserById(UUID id)` and `deleteUser(UUID id)`, but these are not currently exposed by the controller.

### Organizations

Controller: `OrgController`

- `POST /organization`
- `GET /organization/{id}`
- `GET /organization`
- `PUT /organization/{id}`
- `DELETE /organization/{id}`

### Projects

Controller: `ProjectController`

- `POST /projects`
- `GET /projects/{id}`
- `GET /projects`
- `GET /projects/organization/{orgId}`
- `PUT /projects/{id}`
- `DELETE /projects/{id}`

### Issues

Controller: `IssueController`

- `POST /issues`
- `GET /issues/{id}`
- `GET /issues`
- `GET /issues/filter`
- `GET /issues/org/{id}`
- `GET /issues/project/{projectId}`
- `GET /issues/assigned/{userId}`
- `PATCH /issues/{id}/status`
- `PUT /issues/{id}`
- `DELETE /issues/{id}`

`DELETE /issues/{id}` performs a soft delete by setting status to `5`, but the endpoint returns `204 No Content`.

## Security flow

Security is configured in `SecurityConfig`.

- CSRF is disabled.
- `/auth/**` is public.
- Every other route requires authentication.
- Sessions are stateless.
- `JwtFilter` runs before `UsernamePasswordAuthenticationFilter`.
- Passwords are encoded with `BCryptPasswordEncoder`.
- Authentication uses the default `AuthenticationManager` backed by `CustomUserDetailsService`.

JWT behavior is implemented in `JwtService`.

- `generateToken(email)` creates a signed JWT with subject set to email.
- `extractEmail(token)` parses the JWT subject.
- `isValid(token)` only checks whether parsing succeeds; it does not currently verify that the token subject still maps to an active user beyond the filter's user lookup.

## Validation and error handling

Custom exceptions:

- `BadRequestException`
- `ResourceNotFoundException`
- `UnauthorizedException`

`GlobalExceptionHandler` returns structured `ErrorResponse` bodies for:

- Not found
- Bad request
- Unauthorized
- Validation errors
- Illegal argument
- Generic exceptions

DTO classes currently do not use Jakarta Bean Validation annotations, so `MethodArgumentNotValidException` will rarely be triggered until request DTOs are annotated and controller parameters use `@Valid`.

## Current verification status

Command run:

```powershell
.\mvnw.cmd test
```

Result:

- Build success
- Tests run: `1`
- Failures: `0`
- Errors: `0`
- Skipped: `0`

The test suite only contains a Spring context load test, so passing tests confirm the application context can start in the current local environment. They do not validate API behavior.

## Startup issue context

The startup log showed:

```text
Could not resolve placeholder 'app.jwt.secret' in value "${app.jwt.secret}"
```

Current source config does contain `app.jwt.secret` and `app.jwt.expiration-ms` in `src/main/resources/application.yaml`.

Likely causes if the same error appears in IntelliJ:

- The app was started from stale `target/classes` before Maven copied resources.
- IntelliJ did not mark `src/main/resources` as a resources root.
- The run configuration classpath did not include compiled resources.
- The active profile or external config path overrides/defaults excluded `application.yaml`.

Practical fix path:

1. Re-run `.\mvnw.cmd clean test` or `.\mvnw.cmd spring-boot:run`.
2. In IntelliJ, reload Maven project.
3. Confirm `src/main/resources/application.yaml` is copied to `target/classes/application.yaml`.
4. If using profiles, make sure the selected profile has the same JWT properties.

## Notable code issues and risks

### High priority

- Secrets are committed in `application.yaml`. Database credentials and JWT signing secrets should not be committed for shared or production use.
- `IssueService.createIssue` stores `issueType` as the string value of a boolean expression:

  ```java
  String issueType = String.valueOf(Stream.of("BUG","TASK","STORY").anyMatch(...));
  ```

  This saves `"true"` or `"false"` instead of `"BUG"`, `"TASK"`, or `"STORY"`.

- There is no request validation on DTOs. Required fields like email, password, organization name, project ID, issue title, and issue status can be null unless downstream code fails.
- The app relies on a local MySQL database. There is no test database profile, Docker Compose file, or migration setup.

### Medium priority

- `SecurityConfig` has an unused `private JwtService jwtService;` field.
- `JwtFilter` wraps all token parse/auth errors in `ServletException`, which can turn invalid token cases into 500-level failures instead of clean 401 responses.
- `JwtService.isValid` reparses the token after `extractEmail` has already parsed it in the filter.
- `ProjectRepository` redeclares `findById(Long id)`, which is already inherited from `JpaRepository`.
- `ProjectService` enforces project name uniqueness globally, not per organization. This may or may not match product requirements.
- `IssueService.deleteIssue` is a soft delete, but read methods do not filter out deleted/status-5 issues.
- `Comment` exists as an entity but is not connected to the rest of the API.
- Hibernate warns that `MySQL8Dialect` is deprecated; prefer omitting the dialect or using `org.hibernate.dialect.MySQLDialect`.
- `spring.jpa.open-in-view` is enabled by default. Consider explicitly setting it based on desired transaction boundaries.

### Low priority

- Naming is inconsistent: `Authservice` should conventionally be `AuthService`; `Projects` entity would usually be singular `Project`.
- Some column names are inconsistent, for example `created_At` in `Users`.
- Entities are returned directly from controllers. DTO responses would give more control over API shape and prevent accidental serialization problems.
- SQL logging is enabled globally, which is useful locally but noisy and risky outside development.

## Recommended next steps

1. Externalize secrets and database credentials.
2. Fix `IssueService.createIssue` so valid issue types are stored correctly or rejected explicitly.
3. Add DTO validation with `jakarta.validation` annotations and `@Valid` in controllers.
4. Add a test profile using H2, Testcontainers, or a dedicated local test MySQL database.
5. Add service/controller tests for auth, organization, project, and issue flows.
6. Improve JWT error handling so invalid or expired tokens return 401 instead of generic server errors.
7. Decide whether issue deletion should be hard delete or soft delete; if soft delete, filter deleted issues from normal reads.
