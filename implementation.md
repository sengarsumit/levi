# Levi — Implementation Status & Bug Report

_Generated 2026-08-08. Local-only notes, not tracked by git (see `.gitignore`)._

> **This file replaces the previous `implementation.md`.** The old version was an aspirational SRS/architecture blueprint (Redis caching, Kafka, a microservices gateway, full audit trails) describing a target state the code never reached — treat anything from it as a wishlist, not a status report. `context.md` is an earlier, now-stale snapshot (predates the current `Comment` controller/service, the current role/permission system, and a few other things). `FIXES_APPLIED.md` / `TEST_FIX_REPORT.md` are a real record of a Postman-based test session from 2026-07-24 — worth knowing they exist, because they independently caught one of the bugs below ("List Organizations - Tenant Scoped" returning every org) weeks before this review, which is good corroboration that it's a real, reproducible issue and not a misread.
>
> Methodology: full-file review of all 79 backend Java files and 57 frontend TS/TSX files (not grep snippets), cross-referenced client↔server wherever a claim depends on both sides agreeing, plus direct verification of the highest-stakes files (tenancy, auth, org/role/approval flow, issue time-tracking). A couple of claims below were empirically confirmed rather than just read (e.g. the frontend production build was actually run and does fail).

---

## 1. The idea, as I understand it

This is restated here so we can catch misunderstandings early — correct me on anything that's off.

- A user can register an organization. There's meant to be some cap ("max -4 in org") — **the exact meaning is unclear** (max orgs a user can create/own? max members per org?) and needs a decision; see [Open questions](#7-open-questions-for-you).
- An org can have multiple projects.
- Org members get assigned to specific projects (not just to individual issues).
- Productivity tracking, Jira-style: for each issue, know when it was assigned, when work actually started, when it was completed, and the elapsed time — computed against each user's configured shift hours/working days.
- Each org gets its own login/signup experience (a dedicated URL, eventually its own branding).
- Anyone can sign up and request to join an org; the admin who created that org reviews and approves (or rejects) the request.
- At signup, the user picks the role they want; roles are extensible (Admin plus custom roles).
- Future work: per-org theming and login-page customization.

---

## 2. Feature-by-feature status

| # | Idea | Status | Notes |
|---|------|--------|-------|
| 1 | Org registration with a max cap | ❌ Not implemented | No count check anywhere — not on orgs-per-user, not on members-per-org. Also ambiguous which one was meant. |
| 2 | Multiple projects per org | 🟡 Modeled, but broken in practice | The data model and endpoints fully support it, but right now **nobody can create a project** and **listing all projects always fails** — see Bugs #1 and #3. |
| 3 | Users assigned to specific projects | ❌ Not implemented | No project-membership table/entity/endpoint/UI exists at all. Org membership is the only access boundary; the closest thing to "assignment" is `Issue.assignedTo`, a single field on one issue at a time. |
| 4 | Productivity tracking (assigned → started → completed, elapsed time) | 🟡 Partially implemented | The backend genuinely computes and stores `startedAt`/`completedAt`/`timeTakenHours` — this part is real, not a stub. But it's fragile (Bug #6) and there is **no UI anywhere** that shows any of it — no dashboard, chart, or per-user report. |
| 5 | Time tracked according to user shift | ✅ / 🟡 | The shift-aware business-hours calculator is correctly implemented and wired in on the backend, and there's a real settings UI to configure each member's shift. It just feeds into a number nobody can see (see #4). |
| 6 | Org-specific login page | 🟡 Partially implemented | Dedicated `/login/:orgSlug` and `/register/:orgSlug` routes and pages exist and work. No branding/customization exists yet — expected, since that's explicitly future work. |
| 7 | Signup + admin approval | 🟡 Implemented, but broken at the root | The PENDING → APPROVED/REJECTED workflow and its UI are real and functional *once you're inside it*. But the person who creates an org is never added as a member of it (Bug #2) — so on a brand-new org there is no admin to do any approving. Separately, the approve/reject/invite/role-creation actions have no permission check at all (Bug #4), so this isn't just "incomplete," it's currently insecure. |
| 8 | Choose role at signup | ❌ Not implemented | Signup is name/email/password only; joining an org takes no role preference either. Role is decided entirely by whoever approves the request, after the fact. |
| 9 | Roles incl. Admin, extensible | 🟡 Partially implemented | The role/permission model is flexible and there's a real "create custom role" UI. But the two auto-seeded roles don't grant the two permissions the app actually checks anywhere (Bug #1), and the custom-role UI offers permission names that don't match what the backend seeds either (Bug #15). |
| 10 | Future: theme / login-page customization per org | ❌ Not implemented | Confirmed absent, as expected — this was flagged as future work. |

---

## 3. Bugs

Ordered by severity within each group. File paths are relative to `levi/src/main/java/com/koda/levi/` (backend) or `levi-frontend/src/` (frontend) unless stated otherwise.

### Critical — cross-tenant data exposure / access control

1. **Any authenticated user can impersonate any org via a forged header.** `tenancy/TenantContextInterceptor.java:35-45` only rejects a request when the `X-Tenant-ID` header *and* the JWT's `tenantId` claim are both present and disagree. If the JWT simply has no `tenantId` claim — true for every freshly signed-up user, every login without an `orgSlug`, and **every refreshed access token** (see Bug #16) — the header alone decides the tenant, and nothing downstream (`tenancy/TenantAccessService.java:17-22`) ever checks that the caller is actually an approved member of that org. Concretely: log in with no org context, set `X-Tenant-ID: 3`, and you now read/write org 3's projects, issues, boards, and sprints as if you belonged there.
2. **`GET /organization` leaks every organization in the system.** It's exempted from needing a tenant header (`TenantContextInterceptor.java:91-98`), and `service/OrganizationService.java:112-117` responds to "no tenant context" by returning `organizationRepository.findAll()` — every org's id/name/slug, to any logged-in user. This exact symptom was already caught once, independently, in `TEST_FIX_REPORT.md` ("List Organizations - Tenant Scoped: all orgs returned instead of filtered") on 2026-07-24 — it's a real, reproducible, still-present bug.
3. **Self-approval into Admin.** `approveMember`, `rejectMember`, `inviteMember`, and `createRole` (`controller/OrgController.java:58-100`, `service/OrganizationService.java:161-215`) have zero permission checks — not even an `ORG_ADMIN` check. A user can join an org (→ PENDING), read its role list via the equally-open `GET /organization/{orgId}/roles`, and approve *themselves* into the Admin role.
4. **Passwords leak in API responses.** `entity/Users.java:41-42`'s `password` field has no `@JsonIgnore`, and `entity/OrganizationUser.java`'s `user`/`organization`/`role` associations don't either. With `spring.jpa.open-in-view` left at its default (`true`, not set in `application.yaml`), `GET /organization/{orgId}/members/pending`, `.../members/active`, and the approve/invite responses all include every member's BCrypt password hash in the JSON body.
5. **Cross-tenant response-cache leak.** `service/ProjectService.java:68`: `@Cacheable(value = "projects", key = "#id")` caches by bare project id with no tenant component, backed by one process-wide `ConcurrentMapCacheManager` (`config/CacheConfig.java`). Once org A has viewed project `5`, org B requesting project `5` gets org A's cached project back directly — the per-request tenant check in the method body never executes on a cache hit. The same pattern (tenant-blind `@Cacheable` key) is worth checking in `WorkflowStateService`/`SprintService`, which share the same cache manager.
6. **Role IDs aren't checked against the target org.** `approveMember`/`inviteMember` (`service/OrganizationService.java:166-167,188-189`) call `roleRepository.findById(roleId)` with no check that the role belongs to the org being approved into. Role primary keys are one global sequence, so a `roleId` borrowed from org B can be handed to a member of org A, granting org B's permission set.
7. **`GET /users` is entirely unscoped.** `controller/UserController.java` isn't in the tenant-required route list and `Users` has no org relation — it returns every user's name/email/system id in the whole system to any authenticated caller.
8. **WebSocket topics aren't tenant-scoped.** The `/ws-levi` handshake does require a valid JWT, but once connected, nothing (`config/WebSocketConfig.java`, no channel interceptor exists anywhere in the codebase) stops a client from subscribing to `/topic/projects/{id}/issues` for a project id belonging to an org they're not in.

### High — breaks features the idea depends on

9. **Nobody can create a project, out of the box.** The two roles auto-seeded for every new org (`service/OrganizationService.java:60-69,88-99`) — "User": `ISSUE_READ, ISSUE_UPDATE`; "Admin": `ISSUE_READ, ISSUE_CREATE, ISSUE_UPDATE, ISSUE_DELETE, ORG_ADMIN` — contain neither `PROJECT_CREATE` nor `TASK_TRANSITION`, which are the *only two* permission strings the app checks anywhere (`controller/ProjectController.java:21`, `controller/IssueController.java:78,84`, confirmed by direct read — those three `@PreAuthorize` annotations are the entire RBAC enforcement surface of the backend). Result: a brand-new org's Admin can't create a project or drag an issue across the board until someone manually mints a role with the right strings.
10. **Org creator isn't added to the org they create.** `service/OrganizationService.java:74-102` (`createOrganization`) never creates an `OrganizationUser` row for the caller — and `controller/OrgController.java:26-29` doesn't even capture who the caller is (no `@AuthenticationPrincipal`). There is no code path anywhere that makes an org's creator a member of it, let alone Admin. Today this "works" only because of Bug #3 (self-approval); fixing #3 without also fixing this would make new orgs permanently unjoinable.
11. **`GET /projects` (list-all) always fails.** `service/ProjectService.java:75`'s cache key is `T(com.koda.levi.tenancy.TenantContext).getCurrentTenant()` — but `TenantContext` (`tenancy/TenantContext.java`) has no `getCurrentTenant()` method, only `getTenantId()`. Confirmed directly: this method doesn't exist, so the Spring EL expression throws on every call. Every request to this endpoint 500s, unconditionally.
12. **Productivity timestamps depend on exact-string matching a workflow-state's name.** `service/IssueService.java:189-214` (`moveIssue`) only sets `startedAt` when the destination state's name equals `"In Progress"` and only sets `completedAt`/computes elapsed time when it equals `"Done"` (case-insensitive). `entity/WorkflowState.java` has no semantic state-type field — states are free-text and freely renameable via `PUT /workflow-states/{id}`. Rename a column, and time-tracking silently stops with no error.
13. **`PATCH /issues/{id}/status` is completely disconnected from time-tracking.** It updates the raw `status` integer only (`service/IssueService.java:168-177`) and never touches `startedAt`/`completedAt`/`timeTakenHours`. (The shipped frontend doesn't call it — the Kanban board only calls `/move` — so this is currently dormant rather than actively causing bad data, but it's a trap for any future caller.)
14. **Frontend production build currently fails.** `pages/board/IssueDetailPage.tsx:61,64` references `issue.timeTakenHours`, a field that doesn't exist on the `Issue` type (`types/index.ts:1-15`). Empirically confirmed: `npx tsc -b --noEmit` errors (`TS2339`), and since `npm run build` runs `tsc -b && vite build`, **the app cannot currently be built for production.**
15. **Saving an edited issue silently wipes its assignee.** `service/IssueService.java:235-236` (`updateIssue`) unconditionally overwrites `assignedTo`/`assignedBy` from the request DTO with no null-guard (unlike `status`, two lines above, which correctly preserves the existing value when absent). The frontend's edit-issue save handler (`components/issue/IssueDrawer.tsx:44-52`) never includes these fields in its request. Net effect: open an issue, edit only the title, save — the assignee is gone.
16. **Refreshing an access token drops all org context — and the frontend never even uses refresh tokens.** `service/Authservice.java:105` always calls `buildAuthResponse(refreshToken.getUser(), null)` (there's a `// Need to track org in refresh token...` comment acknowledging this is incomplete), and `entity/RefreshToken.java` has no field to store which org the token was issued under even if that were fixed. Separately, on the frontend, `api/authApi.ts` never calls `POST /auth/refresh` or `POST /auth/logout` at all — so this whole mechanism, rotation and all, is currently inert client-side.
17. **"Start Sprint" / "Complete Sprint" silently no-op.** `service/SprintService.java:64-73` (`updateSprint`) updates name/dates but never calls `setStatus(...)`. The frontend's start/complete buttons (`pages/project/BacklogPage.tsx:120-121`) drive exactly this method — the request succeeds, but the sprint's status never changes, so the button just reappears.
18. **No role picker anywhere, and no project-membership UI/API anywhere** — both already covered in the feature table (rows 3 and 8), included here for completeness since they're as much "bugs against the stated intent" as they are missing features.

### Medium — robustness / correctness

19. **`@PreAuthorize` failures return 500, not 403.** There's no dedicated handler for Spring Security's `AccessDeniedException` in `exception/GlobalExceptionHandler.java`, so the generic `Exception.class` handler (`:83-94`) catches it first and reports `500 INTERNAL_SERVER_ERROR` for what should be a `403`.
20. **No Bean Validation outside the auth DTOs.** `OrganizationDto`, `ProjectDto`, `BoardDto`, `WorkflowStateDto`, `IssueDto`, `SprintDto`, `CommentDto` have no `jakarta.validation` annotations and their controllers don't use `@Valid`. E.g. `POST /organization` with a missing `name` throws a raw `NullPointerException` inside `organizationDto.getName().toLowerCase()...`, which the catch-all handler echoes back as the response message — a minor information leak on top of the bad error code.
21. **Multi-step bootstraps aren't transactional.** Org creation (org + 2 roles, `OrganizationService.java:74-102`) and board creation (board + 3 workflow states, `service/BoardService.java:30-66`) are each several unguarded `save()` calls with no `@Transactional`. A failure partway through leaves an org with no Admin role, or a board silently missing its "Done" column (which would also break Bug #12's string match).
22. **Reopening a completed issue inflates its recorded time.** `startedAt` is only ever set once and is never cleared when an issue leaves "Done" (only `completedAt`/`timeTakenHours` get cleared, `IssueService.java:207-213`). An issue completed, reopened, and completed again weeks later recomputes elapsed time from the *original* start, counting the idle gap as working time.
23. **Rate limiting is keyed by raw remote address with no eviction.** `filter/RateLimitingFilter.java` uses `request.getRemoteAddr()` with no `X-Forwarded-For` handling, so behind any reverse proxy every real visitor shares one 100 req/min bucket; the bucket map also grows unbounded for the life of the process.
24. **Workspace switcher never shows a selection.** `features/organization/organizationSlice.ts` types `currentTenantId` as a `string` (from `localStorage`), but `components/layout/WorkspaceSwitcher.tsx` compares it against `org.id`, a `number`. They never strictly equal, so the switcher shows "Select Workspace" forever after a reload even though the correct header is actually being sent on every request.
25. **`authSlice`'s `user` is always null.** `AuthResponse` (`dto/AuthResponse.java`) has no `user` field, but every login/register call site does `setCredentials({ token, user: userData.user })`. `state.auth.user` is therefore always `null` and `selectCurrentUser` is dead code.

### Low / polish

26. Custom-role UI (`pages/organization/OrganizationSettings.tsx:190`) only offers `PROJECT_CREATE`/`TASK_TRANSITION` as assignable checkboxes — a vocabulary that never overlaps with the backend's actual seeded permissions (`ISSUE_READ/CREATE/UPDATE/DELETE`, `ORG_ADMIN`).
27. A JWT bearer token is printed to the browser console on every WebSocket connect — `hooks/useWebSocket.ts:24-26`'s STOMP `debug` callback logs the raw CONNECT frame, which embeds the `Authorization` header set at line 21-22.
28. `useWebSocket.ts:20` hardcodes `http://localhost:8080/ws-levi` — won't work anywhere off localhost, and doesn't go through the same proxy pattern the rest of the app uses.
29. `OrgUserStatus.REJECTED` is defined but never assigned — `rejectMember` hard-deletes the membership row instead, so a rejected user can just request to join again immediately.
30. `IssueDetailPage.tsx` (route `/issues/:issueId`) is a sparse, unreachable duplicate of the real issue view (no comments, activity, or assignee) that also happens to be the file breaking the build (#14).
31. Dead code: `dto/UserDto.java` (zero references), several exported-but-unused RTK Query mutations (`useDeleteSprintMutation`, `useUpdateBoardMutation`, `useDeleteBoardMutation`, `useCreateWorkflowStateMutation`), duplicate JWT-decoding logic in `authSlice.ts` vs. `lib/jwt.ts`.
32. Dark-mode Tailwind classes and a `useTheme()` call exist (`components/ui/sonner.tsx:14`, throughout `components/backlog/*`) but no `ThemeProvider` is mounted anywhere — dead styling, not a real dark mode.
33. Cosmetic: naming inconsistencies (`Authservice`, `created_At`), `ProjectCard.tsx` hardcodes "0 Issues" regardless of actual count, `ProjectDetail.tsx` always renders the status pill green regardless of `ACTIVE`/`ARCHIVED`, dead `/forgot-password` links in both login pages.

---

## 4. What's solid

Worth calling out explicitly, since the bug list is long: the core mechanics are for real, not vaporware.

- JWT auth with BCrypt hashing, stateless sessions, refresh-token rotation (single-use, revoked-on-use) on the backend.
- The `TenantContext` ThreadLocal is correctly cleared on every request (both at the start of `preHandle` and in `afterCompletion`) — the one place you'd most expect a leak, and it's handled properly.
- The shift-aware business-hours elapsed-time calculator (`IssueTimeCalculatorService`) is a genuinely correct implementation, not a stub — it just needs a more reliable trigger (#12) and somewhere to be shown (#4/#5).
- The PENDING/APPROVED/REJECTED membership workflow, once reachable, has a real, fully wired UI (pending list, role select, approve/reject).
- Kanban drag-and-drop does real optimistic updates with rollback on failure, and pushes live updates over WebSocket to other connected clients.
- A new project automatically gets a default board with three workflow states, both client- and server-side.
- Comment CRUD with ownership checks is complete and wired end to end.

---

## 5. Recommended next steps

Roughly in the order I'd tackle them — early items unblock everything after them:

1. **Fix org bootstrap and approval together, as one change**: auto-add the creator as an approved Admin member when an org is created, *and* add real permission checks (`ORG_ADMIN` or equivalent) to approve/reject/invite/create-role. Doing only the first half re-opens Bug #3; doing only the second half makes new orgs unjoinable.
2. **Fix the seeded role permissions** so a default Admin can actually create a project and move an issue (add `PROJECT_CREATE`/`TASK_TRANSITION`, or redesign the permission vocabulary end-to-end across frontend and backend together — they currently don't agree on it either, see Bug #26).
3. **Fix the `GET /projects` cache bug** and add a tenant-aware component to every `@Cacheable` key (or drop caching until it's tenant-safe) — this also closes the cross-tenant cache leak (Bug #5).
4. **Stop trusting a bare `X-Tenant-ID` header**: derive the effective tenant from a verified, approved `OrganizationUser` row, not just header/claim agreement.
5. **Add `@JsonIgnore` to `Users.password`** and stop returning raw entities with nested user objects from membership endpoints.
6. **Decide what "assigned to a project" should actually mean** (a real membership/roster table vs. sufficient-as-is per-issue assignment) — this is a product decision, not just a bug fix, and it blocks a chunk of the original idea.
7. **Give productivity data a home in the UI** — even a simple "started at X, took Y business hours" line on the issue view would surface work that already exists on the backend.
8. **Fix the frontend build break, the assignee-wipe bug, and the sprint-status bug** — all small and contained, all high-visibility.
9. **Revisit "choose a role at signup"** once you've decided what it should mean given approval still happens afterward either way (a *request*, not a grant).

---

## 6. Full findings

The two research passes behind this document (backend and frontend, each read every source file directly and cross-referenced the other side of the API) went deeper than what's summarized above — additional minor findings, exact line citations, and the full evidence trail for each bug are preserved in this session if you want the unabridged version for any specific area.

## 7. Open questions for you

- **"max -4 in org"** — max organizations a user can create/own, or max members per org, or something else? Nothing enforces either interpretation right now, so this needs a decision before it's buildable.
- **"Assigned to a project"** — a real membership/roster feature (invite specific people onto a project), or is per-issue assignment actually what you want, just needing better UI?
- **Role at signup** — should this be a *request* the admin can accept or override (since approval already exists), or should that language just come out of the pitch?
