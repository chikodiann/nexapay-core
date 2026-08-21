# ADR 0010: RabbitMQ Topology for Operational Notifications and Dead-Lettering

## Status
Accepted

## Context
While Apache Kafka serves as NexaPay's immutable domain event stream, transient operational tasks—such as sending customer push notifications, SMS alerts, and email statements—require fine-grained AMQP routing, independent consumer scaling, and immediate dead-letter isolation without polluting domain event offsets.

## Decision
1. Declare a Topic Exchange `nexapay.notifications.exchange` for operational notification dispatch.
2. Bind `account.notifications.queue` with routing patterns `transfer.completed` and `transfer.reversed`.
3. Configure Dead-Letter Exchange `nexapay.notifications.dlx` and Dead-Letter Queue `account.notifications.dlq`.
4. Configure rejected messages (`defaultRequeueRejected = false`) to automatically route unrecoverable poison-pill messages to `account.notifications.dlq`.

## Consequences

### Benefits
- Decouples operational alert delivery from core ledger event consumption.
- Poison-pill notifications are quarantined in `account.notifications.dlq` without blocking the notification queue.
- Allows independent horizontal scaling of notification workers.

### Trade-offs
- Introduces RabbitMQ alongside Kafka, requiring management of two broker clusters in production.