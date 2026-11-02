package com.nexapay.account.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "ledger_mutations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerMutation {

    @Id
    private UUID id;

    @Column(name = "transfer_reference", nullable = false, unique = true, length = 64)
    private String transferReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 20)
    private MutationType operationType;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MutationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private LedgerMutation(
            UUID id,
            String transferReference,
            MutationType operationType,
            UUID accountId,
            BigDecimal amount,
            MutationStatus status,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Id must not be null");
        this.transferReference = Objects.requireNonNull(transferReference, "TransferReference must not be null");
        this.operationType = Objects.requireNonNull(operationType, "OperationType must not be null");
        this.accountId = Objects.requireNonNull(accountId, "AccountId must not be null");
        this.amount = Objects.requireNonNull(amount, "Amount must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt must not be null");
    }

    public static LedgerMutation create(
            String transferReference,
            MutationType operationType,
            UUID accountId,
            BigDecimal amount,
            MutationStatus status
    ) {
        return new LedgerMutation(
                UUID.randomUUID(),
                transferReference,
                operationType,
                accountId,
                amount,
                status,
                Instant.now()
        );
    }
}
