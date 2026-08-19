# ADR 0008: Idempotent Event Consumption via Deduplication Store

## Status
Accepted

## Context
NexaPay's Transactional Outbox provides at-least-once delivery guarantees. During broker rebalances, network timeouts, or service restarts, the same message may be delivered multiple times to downstream consumers.

Uncontrolled consumption of duplicate events would lead to repeated customer notifications, duplicate audit log rows, or corrupted secondary balances.

## Decision
All asynchronous Kafka consumers in NexaPay must implement the Idempotent Consumer pattern using a relational deduplication store:
1. Maintain a `consumed_messages` table with a composite unique constraint: `UNIQUE (consumer_name, event_id)`.
2. Before processing an incoming message, query `consumed_messages` for the `(consumer_name, event_id)` tuple.
3. If present, the message is acknowledged and skipped immediately.
4. If absent, execute the domain side-effects and insert the record into `consumed_messages` within the same local database transaction.

## Consequences

### Benefits
- Converts at-least-once message delivery into effectively-once business processing.
- Isolates consumers: different consumers track their deduplication state independently using `consumer_name`.
- Eliminates duplicate notifications and inconsistent projection states.

### Trade-offs
- Adds a database lookup and write for every consumed event.
- Requires periodic pruning/TTL on the `consumed_messages` table for high-volume topics.