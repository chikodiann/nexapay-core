# ADR 0001: Feature-Oriented Package Boundaries

## Status
Accepted

## Context
Traditional layered architectures (`controllers/`, `services/`, `repositories/`) group disparate domains together, leading to tight coupling and poor domain visibility as the codebase scales.

## Decision
Adopt a feature-oriented package layout for each service:
- `<service>/<feature>/{api, application, domain, infrastructure}`
- Shared infrastructure and cross-cutting concerns reside in `<service>/common/`.

## Consequences
- High cohesion within bounded contexts.
- Clean separation between HTTP contracts, orchestration, domain invariants, and database drivers.