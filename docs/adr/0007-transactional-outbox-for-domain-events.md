# ADR 0007: Transactional Outbox for Domain Event Publishing

## Status
Accepted

## Context
When a transfer completes or reverses in `payment-service`, downstream consumers (notifications, audit logging, analytics) must be notified via Kafka.

Directly invoking `kafkaTemplate.send()` within the local database transaction introduces the dual-write problem:
1. If the broker call succeeds but the database transaction fails to commit, a phantom event is published.
2. If the database transaction commits but the network partition prevents Kafka publishing or the service crashes, the event is permanently lost.

## Decision
Adopt the Transactional Outbox pattern:
1. `payment-service` maintains an `outbox_events` table within its PostgreSQL database.
2. The `Transfer` aggregate state change and the corresponding `OutboxEvent` (`TransferCompleted` or `TransferReversed`) are persisted atomically within the same local database transaction.
3. A background `OutboxPublisherService` worker polls `PENDING` events and publishes them to the `nexapay.transfer.events` Kafka topic with the transfer reference as partition key.
4. An outbox event is transitioned to `PUBLISHED` only after receiving synchronous acknowledgment from the Kafka broker.
5. If broker publication fails, the attempt counter increments and the event remains `PENDING` for subsequent retry. After 5 failed attempts, it transitions to `FAILED`.

## Consequences

### Benefits
- Guaranteed at-least-once domain event publication.
- Eliminates dual-write inconsistencies between PostgreSQL and Kafka.
- Survives service crashes and broker outages without message loss.

### Trade-offs
- Downstream consumers must be idempotent to handle potential duplicate publications.
- Small delivery latency introduced by the publisher polling interval.
- Periodic archiving or purging strategy required for `PUBLISHED` outbox records.