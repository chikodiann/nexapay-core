# ADR 0004: Database-Per-Service Architecture

## Status
Accepted

## Context
Sharing databases between microservices couples schemas, bypasses service encapsulation, and eliminates transactional boundaries.

## Decision
Enforce strict database isolation:
- `account-service` connects exclusively to `nexapay_account_db`.
- `payment-service` connects exclusively to `nexapay_payment_db`.
- Inter-service communication occurs strictly via HTTP (synchronous) or event streams (asynchronous). Direct cross-database joins/queries are forbidden.

## Consequences
- Enforces strict distributed system boundaries.
- Requires handling distributed state, idempotency, and eventual consistency explicitly at the application level.