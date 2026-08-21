
# NexaPay Core Platform

[![Java](https://img.shields.io/badge/Java-21-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen)]()
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)]()
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

A high-reliability, distributed core banking and transaction processing platform built with Java 21, Spring Boot 3, and domain-driven design principles.

NexaPay is a portfolio engineering project focused on the problems that make financial systems difficult to build correctly: transaction consistency, concurrency, idempotency, service isolation, failure recovery, event-driven processing, observability, and secure API design.

> **Note:** NexaPay is an independently designed portfolio project. It contains no proprietary source code, API contracts, schemas, documentation, or confidential implementation details from any financial institution.

---

## Current Architecture

```mermaid
graph TD
    Client[API Client / Frontend] -->|HTTP REST| PaySvc[Payment Service :8082]
    Client -->|HTTP REST| AccSvc[Account Service :8081]

    PaySvc -->|Synchronous Account Validation| AccSvc

    subgraph Storage Isolation
        AccSvc --> AccDB[(PostgreSQL: nexapay_account_db)]
        PaySvc --> PayDB[(PostgreSQL: nexapay_payment_db)]
    end
````

NexaPay currently follows a **database-per-service architecture**. Payment Service communicates with Account Service through an explicit HTTP boundary and does not access Account Service persistence directly.

---

## Current Implementation

### 1. Account Service (`services/account-service`)

* ✅ Feature-oriented clean package layout (`account/{api, application, domain, infrastructure}`)
* ✅ PostgreSQL persistence with Flyway migrations (`V1__init_accounts_table.sql`)
* ✅ Invariant-enforcing `Account` aggregate with dual-balance tracking (`availableBalance`, `ledgerBalance`)
* ✅ Decoupled account number generation through `AccountNumberGenerator`
* ✅ Row-level pessimistic locking (`PESSIMISTIC_WRITE`) for atomic balance debit and credit mutations
* ✅ RESTful account management, lookup, status, debit, and credit operations
* ✅ RFC 7807 `ProblemDetail` global error responses
* ✅ Unit and PostgreSQL-backed integration tests

### 2. Payment Service (`services/payment-service`)

* ✅ Isolated PostgreSQL persistence and Flyway migration (`V1__init_transfers_table.sql`)
* ✅ `Transfer` aggregate root with explicit lifecycle states (`PENDING`, `PROCESSING`, `SUCCESSFUL`, `FAILED`, `REVERSED`)
* ✅ Synchronous `AccountServiceClient` over HTTP with strict database boundary isolation
* ✅ Pre-mutation transfer persistence
* ✅ Mandatory `Idempotency-Key` support
* ✅ Transfer execution endpoint (`POST /api/v1/transfers`)
* ✅ Transfer lookup endpoint (`GET /api/v1/transfers/{transferReference}`)
* ✅ Unit and MockMvc integration tests covering idempotency replay, insufficient funds, unknown accounts, and currency validation

---

## Engineering Roadmap

### Phase 1 — Foundations & Domain Modeling ✅

* ✅ Multi-module Maven setup with Java 21 and Spring Boot 3
* ✅ PostgreSQL containerization and Flyway migration baseline
* ✅ Account aggregate and domain invariants
* ✅ Database-per-service isolation
* ✅ Integration testing against PostgreSQL from day one

### Phase 2 — Payment Execution, Consistency & Concurrency ✅

* ✅ Synchronous payment initiation over HTTP boundary
* ✅ Transfer lifecycle persistence
* ✅ Idempotency-key handling
* ✅ Transfer lookup endpoint(`GET /api/v1/transfers/{reference}`)
* ✅ Pessimistic row-level locking (`PESSIMISTIC_WRITE`) for atomic ledger balance  mutations
* ✅ Partial transfer failure compensation and idempotent mutation tracking
* ✅ Transactional outbox table schema and domain-event publishing contract (`TransferCompleted`, `TransferReversed`)

### Phase 3 — Event-Driven Processing & Messaging 🚧

* ✅ Apache Kafka integration for durable domain events

  * `TransferCompleted`
  * `TransferFailed`
* ✅ Transactional outbox relay worker with retry thresholds
* ✅ Idempotent event consumers with relational deduplication store (`consumed_messages`)
* ✅ Kafka consumer retry policies and Dead-Letter Topic (`nexapay.transfer.events.DLT`)
* ✅ RabbitMQ integration for operational workflows

  * Exchange & queue topologies (`nexapay.notifications.exchange`)
  * Dead-Letter Exchange (`nexapay.notifications.dlx`) & DLQ routing
* [ ] End-to-end multi-service test suite against real Kafka & RabbitMQ brokers via Testcontainers

### Phase 4 — Cloud-Native Infrastructure & Observability

* ✅ Spring Boot Actuator health and metrics endpoints
* [ ] Multi-stage Docker images
* [ ] Docker Compose full-platform environment
* [ ] Prometheus metrics collection
* [ ] Grafana dashboards
* [ ] Structured application logging
* [ ] Kubernetes Deployments and Services
* [ ] Kubernetes ConfigMaps and Secrets
* [ ] Readiness and liveness probes
* [ ] GitHub Actions CI pipeline

### Phase 5 — Customer Domain, Risk & Compliance

* [ ] Customer profile management
* [ ] KYC tiers
* [ ] Account transaction limits
* [ ] Transaction velocity controls
* [ ] Risk/fraud event hooks
* [ ] Audit trail enhancements

### Phase 6 — Production Engineering

* [ ] Testcontainers-based cross-service integration testing
* [ ] Performance and load testing
* [ ] Failure-injection scenarios
* [ ] Reconciliation workflow
* [ ] API versioning strategy
* [ ] Security hardening
* [ ] Architecture and operational documentation

---

## Architecture Decision Records

Important architectural decisions are documented as ADRs rather than being hidden inside implementation details.

* [ADR 0001: Feature-Oriented Package Structure](docs/adr/0001-feature-oriented-package-structure.md)
* [ADR 0002: Dual-Balance Ledger Model](docs/adr/0002-two-balance-ledger-model.md)
* [ADR 0003: Decoupled Account Number Generation](docs/adr/0003-account-number-generation-abstraction.md)
* [ADR 0004: Database-Per-Service Isolation](docs/adr/0004-database-per-service-isolation.md)
* [ADR 0005: Pessimistic Locking for Balance Mutations](docs/adr/0005-pessimistic-locking-for-balance-mutations.md)
* [ADR 0006: Compensating Transactions for Cross-Service Transfers](docs/adr/0006-compensating-transactions-for-cross-service-transfers.md)
* [ADR 0007: Transactional Outbox for Domain Events](docs/adr/0007-transactional-outbox-for-domain-events.md)
* [ADR 0008: Idempotent Consumer Deduplication](docs/adr/0008-idempotent-consumer-deduplication.md)
* [ADR 0009: Idempotent Message Consumption](docs/adr/0009-idempotent-message-consumption.md)
* [ADR 0010: Idempotent Message Consumption](docs/adr/0010-idempotent-message-consumption.md)

---

## Technology Stack

**Backend:** Java 21, Spring Boot 3, Spring Data JPA, Hibernate, Maven

**Persistence:** PostgreSQL 16, Flyway

**Security & API:** Spring Security, Bean Validation, RFC 7807 Problem Details, OpenAPI/Swagger

**Testing:** JUnit 5, Mockito, MockMvc, Testcontainers

**Messaging — Roadmap:** Apache Kafka, RabbitMQ

**Infrastructure — Roadmap:** Docker, Kubernetes, GitHub Actions

**Observability — Roadmap:** Spring Boot Actuator, Prometheus, Grafana

---

## Getting Started

### Prerequisites

* Java 21
* Maven 3.9+
* Docker & Docker Compose

### Running Locally

```bash
# Start infrastructure
docker compose up -d

# Run the test suites
mvn clean test

# Start Account Service
mvn spring-boot:run -pl services/account-service

# Start Payment Service
mvn spring-boot:run -pl services/payment-service
```

Account Service:

```text
http://localhost:8081
```

Payment Service:

```text
http://localhost:8082
```

---

## Engineering Principles

NexaPay is being developed around several core principles:

* **Service ownership:** each service owns its domain and persistence.
* **Financial correctness over convenience:** balance mutation is controlled and concurrency-aware.
* **Idempotency:** retrying a financial request must not create duplicate financial effects.
* **Explicit failure handling:** distributed failures are treated as normal system behaviour.
* **Testability:** domain behaviour and infrastructure boundaries are tested from the beginning.
* **Observability:** production behaviour should be measurable rather than guessed.
* **Security by design:** authentication, authorization, validation, and data protection are architectural concerns.
* **No shared database shortcuts:** services communicate through explicit contracts.

---

## License

This project is licensed under the MIT License.