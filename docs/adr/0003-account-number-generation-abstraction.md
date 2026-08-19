# ADR 0003: Decoupled Account Number Generation

## Status
Accepted

## Context
Database primary keys (`UUID` / surrogate IDs) must never leak externally as customer-facing account identifiers. Domain entities must not be tightly coupled to random number generators or external algorithms.

## Decision
Introduce the `AccountNumberGenerator` domain interface. The infrastructure layer provides a cryptographically strong implementation (`SecureRandomAccountNumberGenerator`) generating unique 10-digit account numbers.

## Consequences
- Keeps the `Account` domain pure and testable.
- Permits replacing or augmenting account number generation schemes (e.g., standard check-digit algorithms) without altering core entity invariants.