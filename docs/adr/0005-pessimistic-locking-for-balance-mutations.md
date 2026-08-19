# ADR 0005: Pessimistic Locking for Account Balance Mutations

## Status
Accepted

## Context
Account balances may be mutated by multiple concurrent payment requests.

A simple read-check-update sequence can allow multiple transactions to observe the same balance before either update is committed, creating lost updates or allowing an account to overspend.

Financial correctness requires balance validation and mutation to behave atomically under concurrent access.

## Decision
NexaPay acquires a PostgreSQL row-level write lock (`SELECT ... FOR UPDATE`) when loading an account for balance mutation.

The balance check and subsequent debit or credit execute within the same database transaction.

Spring Data JPA uses `PESSIMISTIC_WRITE` on `findByAccountNumberForUpdate()` in `AccountRepository`.

## Consequences

### Benefits
- Prevents concurrent balance mutations from observing stale balances.
- Eliminates lost updates under high concurrency.
- Protects against overspending caused by concurrent debit race conditions.
- Provides straightforward consistency guarantees for individual account mutations.

### Trade-offs
- Concurrent requests against the same account are serialized.
- High-contention accounts may experience increased database wait times.
- Transactions must remain short to minimize lock contention.
- Distributed transactions across multiple services are not solved by this single-service mechanism.

## Alternatives Considered

### Optimistic Locking (`@Version`)
The Account model contains a version field and optimistic locking can detect concurrent modifications. However, under high write contention, this causes high transaction abort rates and requires complex application-level retry handling.

### Database Atomic Update (`UPDATE accounts SET balance = balance - ? WHERE balance >= ?`)
Moves balance validation entirely into database SQL expressions. While performant, it bypasses aggregate domain rules, validation hooks, and audit lifecycle management in the application layer.

## Decision Outcome
Pessimistic locking was selected because financial correctness and explicit serialization are prioritized over raw unbounded throughput at this core ledger layer.