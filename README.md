# NexaPay Core

> A production-minded digital banking backend built with Java and Spring Boot, designed to demonstrate secure financial APIs, distributed systems, event-driven architecture, and cloud-native engineering practices.

> 🚧 **Active Development** — NexaPay Core is being built incrementally with production-oriented architecture, testing, security, observability, and deployment practices.

## Overview

NexaPay Core is a fictional digital banking platform designed to model the engineering challenges behind modern financial systems.

Rather than functioning as a simple CRUD application, the project focuses on backend concerns commonly encountered in production financial platforms, including:

- Secure account management
- RESTful API design
- Financial transaction processing
- Idempotent payment requests
- Transaction consistency
- Concurrent operations
- Event-driven communication
- Asynchronous processing
- Authentication and authorization
- Auditability
- Failure handling and recovery
- Observability
- Automated testing
- Containerized deployment

All business rules, architecture, data models, and implementations in this repository are independently designed for this project.

No proprietary code, architecture, documentation, or confidential information from any financial institution is used.

---

## Technology Stack

### Backend
- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

### Data
- PostgreSQL
- Redis *(planned)*

### Messaging
- Apache Kafka *(planned)*
- RabbitMQ *(planned)*

### Testing
- JUnit 5
- Mockito
- Testcontainers *(planned)*

### Infrastructure
- Docker
- Docker Compose
- Kubernetes *(planned)*
- GitHub Actions CI/CD *(planned)*

### Observability
- Spring Boot Actuator *(planned)*
- Micrometer *(planned)*
- Prometheus *(planned)*
- Grafana *(planned)*

---

## Architecture

NexaPay Core is being developed incrementally.

The initial implementation focuses on two core services:

### Account Service

Responsible for:

- Customer accounts
- Account balances
- Account status
- Balance validation
- Account-level transaction controls

### Payment Service

Responsible for:

- Transfer initiation
- Payment validation
- Idempotency
- Transfer lifecycle management
- Transaction processing

Additional services and asynchronous infrastructure will be introduced as the system evolves.

---

## Engineering Roadmap

### Phase 1 — Core Banking APIs
- [ ] Account Service
- [ ] Payment Service
- [ ] PostgreSQL persistence
- [ ] REST API documentation
- [ ] Authentication and authorization
- [ ] Unit and integration testing
- [ ] Dockerized local environment

### Phase 2 — Reliability & Transaction Safety
- [ ] Idempotent payment processing
- [ ] Concurrency protection
- [ ] Optimistic locking
- [ ] Redis
- [ ] Global exception handling
- [ ] Rate limiting
- [ ] Structured logging

### Phase 3 — Event-Driven Architecture
- [ ] Apache Kafka
- [ ] Transaction events
- [ ] RabbitMQ
- [ ] Asynchronous notifications
- [ ] Retry strategies
- [ ] Dead-letter queues

### Phase 4 — Cloud-Native Infrastructure
- [ ] Kubernetes
- [ ] CI/CD pipeline
- [ ] Health checks
- [ ] Metrics
- [ ] Prometheus
- [ ] Grafana

### Phase 5 — Production Engineering
- [ ] Testcontainers
- [ ] Performance testing
- [ ] Failure scenario testing
- [ ] Reconciliation
- [ ] API versioning
- [ ] Architecture Decision Records
- [ ] Security hardening

---

## Project Status

🚧 **Currently under active development.**

The project is being implemented incrementally, with architecture and engineering decisions documented as development progresses.

---

## License

This project is licensed under the MIT License.
