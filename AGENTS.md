# AGENTS.md - NexaPay Core Engineering Protocol

## System Context & Stack
- Architecture: Modular microservices architecture (Java 21, Spring Boot 3.3.x, Maven multi-module).
- Databases: PostgreSQL 16 (per-service schema isolation) + Redis 7 (caching & distributed locks).
- Target Quality: Production-grade financial engineering (no naive arithmetic, no missing constraints).

## Critical Implementation Rules
1. Financial Precision: Use `BigDecimal` or `NUMERIC(18,4)` for all currency values. Never use `Double` or `Float`.
2. Concurrency & Integrity:
   - Apply `@Version` optimistic locking on mutable account entities.
   - For transfer execution, sort account identifiers deterministically prior to acquisition to prevent database deadlocks.
3. Idempotency: All payment mutations must check and enforce an `Idempotency-Key` header with database/Redis locks before processing.
4. Schema Migrations: All database DDL must be managed exclusively through Flyway migrations in `src/main/resources/db/migration/`. Never use `ddl-auto: update` or `ddl-auto: create`.
5. Error Handling: Return standard RFC 7807 `ProblemDetail` responses for business and validation failures.

## Testing & Verification Standard
- Unit tests: JUnit 5 + Mockito for domain and service layers.
- Integration tests: `@SpringBootTest` with Testcontainers (real PostgreSQL/Redis instances, no H2 in-memory substitutes).
- Verification requirement: The agent must execute `./mvnw clean test` and verify that all build steps pass before marking tasks as complete.