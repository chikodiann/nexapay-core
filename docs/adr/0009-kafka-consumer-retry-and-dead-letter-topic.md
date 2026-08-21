# ADR 0009: Kafka Consumer Retry Policy and Dead-Letter Topic (DLT)

## Status
Accepted

## Context
When consuming domain events from Kafka (such as `TransferCompleted` or `TransferReversed`), consumers may encounter transient errors (e.g., database connection pool exhaustion, network blips) or non-transient errors (e.g., malformed payloads, poison pills).

Blocking partition consumption indefinitely degrades service availability, while discarding failed events risks silent data loss.

## Decision
1. Configure Spring Kafka `DefaultErrorHandler` with exponential backoff (1s initial, 2.0x multiplier, 3 max attempts).
2. Configure `DeadLetterPublishingRecoverer` to route exhausted failures to `<original-topic>.DLT` (e.g., `nexapay.transfer.events.DLT`).
3. Preserve message key, partition, and attach diagnostic error headers (`kafka_original-topic`, `kafka_exception-message`, `kafka_exception-stacktrace`).
4. Ensure the consumer deduplication record (`consumed_messages`) is written strictly in the same transaction as business processing—failed events are never marked consumed so retries are not dropped as duplicate events.

## Consequences

### Benefits
- Resilient recovery from transient downstream outages.
- Poison pills are isolated in the DLT without blocking the main event topic partitions.
- DLT messages preserve original headers and exception context for debugging and replay.

### Trade-offs
- Out-of-order processing can occur for a specific key if later events on the main topic are processed while an earlier event is routed to the DLT.
- Requires monitoring and dead-letter replay operational tooling.