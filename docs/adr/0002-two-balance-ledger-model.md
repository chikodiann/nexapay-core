# ADR 0002: Dual-Balance Ledger Model (Available vs Ledger)

## Status
Accepted

## Context
Financial systems must distinguish between settled funds and pending authorizations or holds.

## Decision
Maintain two distinct `BigDecimal` fields on the `Account` aggregate:
1. `ledgerBalance`: Reflects strictly posted and settled entries.
2. `availableBalance`: Reflects funds currently spendable by the customer.

## Consequences
- Guarantees future extensibility for card authorizations, holds, and pending clearing windows without schema redesign.