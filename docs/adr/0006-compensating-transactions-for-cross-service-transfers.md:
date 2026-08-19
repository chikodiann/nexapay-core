# ADR 0006: Compensating Transactions for Cross-Service Transfers

## Status
Accepted

## Context
In a database-per-service architecture, a single transfer spans two distinct microservice persistence boundaries (`account-service` and `payment-service`).

If the source account is debited successfully but the destination account credit fails (due to network partition, service unavailability, or timeouts), a distributed inconsistency occurs. Distributed two-phase commit (2PC/XA) protocols are avoided due to lock-holding latency, blocking coordinators, and lack of cloud-native resilience.

## Decision
NexaPay uses application-level compensating transactions rather than distributed XA transactions:
1. `account-service` tracks idempotent balance mutations in an `account_mutations` ledger table (`UNIQUE(account_number, reference, mutation_type)`).
2. The transfer aggregate tracks step-level execution booleans (`sourceDebited`, `destinationCredited`, `compensationCompleted`).
3. If destination credit fails after source debit succeeds:
   - A compensating credit is issued back to the source account using deterministic reference `<transferReference>:REVERSAL`.
   - The transfer is transitioned to `REVERSED`.
   - If compensation fails, the transfer is marked `FAILED` (`COMPENSATION_FAILED`) for automated or manual reconciliation.

## Consequences

### Benefits
- Prevents cross-database deadlocks and coordinator single-points-of-failure.
- Deterministic reversal references (`<ref>:REVERSAL`) make compensation strictly idempotent.
- Explicit lifecycle statuses (`FAILED` vs `REVERSED`) preserve audit trail clarity.

### Trade-offs
- Temporary inconsistency can exist between the debit and compensation resolution.
- Edge failures during compensation require operational reconciliation mechanisms.

## Alternatives Considered

### Distributed 2PC / XA
Rejected due to holding row locks across network partitions, high latency, and database driver coupling.