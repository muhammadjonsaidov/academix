---
name: write-integration-test
description: Write an AcademiX backend integration test that crosses a real boundary — RLS-scoped queries, RabbitMQ consumers, Redis counters, HTTP auth/validation, AI vendor parsing. Trigger for "write an integration test", "test with a real database", "test the queue consumer", "test RLS", "test the endpoint".
---

# AcademiX integration tests

Stack: JUnit 5 + Testcontainers (`TestcontainersConfiguration` — Postgres pgvector, RabbitMQ, Redis) + Mockito + ArchUnit. Needs Docker and `docker compose up -d seaweedfs` in `infra/` (the S3 client connects at startup). Run on JDK 21.

What earns a real-boundary test:
- **RLS**: seed two schools, scope to A, assert B's rows never appear. The tests connect as a superuser, for which RLS is inert — use `RlsMechanismTest`/`TenantScopeIntegrationTest`'s approach (switch to the restricted role) or you'll prove nothing.
- **HTTP**: `@SpringBootTest @AutoConfigureMockMvc` with real JWTs from `JwtService` (see `RoleAccessIntegrationTest`, `RequestValidationIntegrationTest`, `PsychologyModuleDisabledIntegrationTest`). Use `@SpringBootTest(properties = …)` to flip feature flags.
- **Queues**: message → status transitions → retry ×3 → DLX, against real RabbitMQ.
- **Budget counters**: the 15/65/20 split and degradation order, against real Redis.
- **Data deletion**: approval nulls sensitive columns but keeps rows.

Unit tests are enough for pure logic: score math, XP/streak rules, prompt-response parsers (feed recorded JSON, never call the real AI APIs), the tutor bare-answer heuristic.

Conventions: arrange/act/assert, descriptive names, each test creates its own data with unique phones/ids (contexts and containers are shared across classes).
