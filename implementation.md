PROJECT LEVI
                                 Software Requirements Specification & Architecture Blueprint
                                 Version 1.0: Monolith to Microservices Roadmap
                                 Comprehensive Engineering Guide for Spring Boot 3.x, MySQL, and Redis
                                 1
                                 Table of Contents
                                 Phase 00: Foundation & System Architecture
                                 Phase 01: Authentication & Identity Security
                                 Phase 02: Multi-Tenancy & Organization Context
                                 Phase 03: Projects, Boards & Workflows
                                 Phase 04: Issue Tracking Engine
                                 Phase 05: Collaboration & Comments
                                 Phase 06: Sprint Management & Backlogs
                                 Phase 07: Asynchronous Notifications & Events
                                 Phase 08: Caching Strategy (Redis)
                                 Phase 09: File Storage (S3/Cloudinary)
                                 Phase 10: Dockerization & CI/CD
                                 Phase 11: Microservices API Gateway Transition
                                 Phase 12: Time Tracking & Shift Management
                                 Phase 13: Analytics & Dashboards
                                 Phase 00: Foundation & System Architecture
                                 1. Objective
                                 Establish the core technological foundation for a high-performance, multi-tenant SaaS project management
                                 platform. The system will initially be built as a modular monolith to reduce deployment complexity while
                                 enforcing strict package boundaries, allowing for a seamless transition to a microservices ecosystem via an
                                 API gateway in Phase 11.
                                 2. Tech Stack
                                 Runtime: Java 17 / Java 21
                                 Framework: Spring Boot 3.x
                                 Persistence: Spring Data JPA, Hibernate, MySQL 8.0
                                 Caching: Redis (for sessions, lookups, and rate limiting)
                                 Security: Spring Security with Stateless JWT
                                 Messaging: Kafka or RabbitMQ (introduced in Phase 07)
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 2
                                 Phase 01: Authentication & Identity Security
                                 1. Design Philosophy
                                 We implement stateless authentication using JSON Web Tokens (JWT). This ensures that backend nodes can
                                 horizontally scale without sticky sessions. A secondary refresh token mechanism guarantees long-term
                                 session stability while keeping the JWT lifespan short (15 minutes) to minimize security risks.
                                 2. Business Rules & ID Generation
                                 Critical Business Constraint: System Identifiers
                                 All system-generated tracking IDs (User IDs, Workspace IDs, Lead IDs, Issue Keys) must strictly adhere
                                 to an alphanumeric format where the prefix is purely alphabetical, followed by a numeric sequence. 
                                 Numeric characters are strictly forbidden at the start of the identifier.
                                 Valid examples: USR-1049, LEAD-8842, PRJ-001.
                                 Invalid examples: 100-USR, 1LEAD-99. 
                                 3. Database Schema (Auth Subdomain)
                                 Table Name
                                 users
                                 refresh_tokens
                                 Primary Purpose
                                 Stores core credentials and profile data.
                                 Long-lived tokens mapped to users.
                                 Architectural Interview Prep:
                                 Indexes
                                 email (UNIQUE), system_id (UNIQUE)
                                 token (UNIQUE), user_id
                                 Q: Why did we separate the Auth domain from the User profile domain?
                                 A: To enforce bounded contexts. In a future microservices split, the Auth Service will handle token
                                 issuance and password hashing, while the User Service handles avatars, preferences, and organization
                                 mapping. Keeping them logically separated in the monolith prevents cyclic dependencies. 
                                 3
                                 Phase 02: Multi-Tenancy & Organization Context
                                 1. Multi-Tenancy Strategy
                                 Project Levi utilizes Row-Level Isolation via a discriminator column (organization_id). Every tenant's
                                 data lives in the same database, but Hibernate filters prevent data leakage.
                                 2. Implementation Mechanics
                                 it from the user's JWT claims). This tenant ID is stored in a ThreadLocal context. Hibernate intercepts all
                                 queries and automatically appends WHERE organization_id = ? to all outgoing SQL statements.
                                 
                                 3. Organization-Based Login & RBAC
                                 Users belonging to a specific organization will authenticate via a dedicated route (e.g., /login/{org-slug} or /login/{org-id}).
                                 Only Organization Creators/Admins possess the authority to create new projects.
                                 Role-Based Access Control (RBAC): Organizations can define custom or predefined roles. These roles dictate permissions, such as the ability to transition tasks across workflow states or assign tasks to other users within the organization.
                                 Phase 03: Projects, Boards & Workflows
                                 1. Data Structures
                                 A Project acts as a container for workflows, sprints, and issues. Projects map closely to the Agile
                                 methodology.
                                 @Entity @Table(name = "projects") public class Project { @Id @GeneratedValue(strategy =
                                 GenerationType.IDENTITY) private Long id; @Column(nullable = false, unique = true) private
                                 String projectKey; // e.g., LEVI (No starting numbers) private String name; private String
                                 description; @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "organization_id")
                                 private Organization organization; } 
                                 Phase 04: Issue Tracking Engine
                                 1. Core Features
                                 Issues are the heartbeat of the system. They represent Bugs, Tasks, or Stories. They undergo state transitions
                                 (To Do → In Progress → Done) dictated by the board's workflow.
                                 Assignee and Reporter mappings (Foreign keys to users table). • 
                                 4
                                 Priority and Complexity points.
                                 Full audit trailing (tracking exactly who changed the status and when).
                                 Phase 07: Asynchronous Notifications & Events
                                 1. Event-Driven Architecture
                                 When an Issue status changes, the HTTP thread should not block to send an email. Instead, the Issue
                                 Service publishes an IssueUpdatedEvent to an internal Spring ApplicationEvent publisher (later migrated
                                 to Kafka).
                                 A listener running on a separate thread picks up this event, formats a notification payload, and pushes it via
                                 WebSockets to connected UI clients and via SMTP for email digests.
                                 Phase 08: Caching Strategy (Redis)
                                 1. Optimization Tactics
                                 To ensure sub-100ms API response times, we introduce Redis caching for frequently accessed, rarely
                                 mutated data:
                                 Role & Permission Matrices: Cached on login to avoid DB hits on every secured endpoint.
                                 Project Metadata: Board columns and active sprint IDs are cached.
                                 Rate Limiting: A Token Bucket algorithm backed by Redis prevents API abuse.
                                 Phase 11: Microservices API Gateway Transition
                                 1. The Path Forward
                                 As the team and traffic grow, the monolithic architecture will be split. The first step is standing up a Spring
                                 Cloud Gateway.
                                 The Gateway will act as the single entry point. It will handle JWT validation and route requests to downstream
                                 services (e.g., /api/v1/auth/** goes to the Identity Service, /api/v1/issues/** goes to the Core
                                 • 
                                 • 
                                 • 
                                 • 
                                 • 
                                 5
                                 Tracker Service). Environment-based secrets (like the JWT signing key) will be injected securely via
                                 Kubernetes Secrets into the microservices.
                                 Architectural Interview Prep:
                                 Q: What are the trade-offs of moving from this monolith to microservices?
                                 A: While microservices allow independent scaling and deployments, they introduce distributed data
                                 management complexities. Network latency increases, distributed tracing (Zipkin/Jaeger) becomes
                                 mandatory, and handling distributed transactions requires patterns like Saga or Two-Phase Commit
                                 instead of a simple @Transactional annotation. 
                                 6
