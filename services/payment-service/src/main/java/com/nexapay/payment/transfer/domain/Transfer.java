package com.nexapay.payment.transfer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "transfers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transfer {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    @Id
    private UUID id;

    @Column(name = "transfer_reference", nullable = false, unique = true, length = 32)
    private String transferReference;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 128)
    private String idempotencyKey;

    @Column(name = "source_account_number", nullable = false, length = 10)
    private String sourceAccountNumber;

    @Column(name = "destination_account_number", nullable = false, length = 10)
    private String destinationAccountNumber;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransferStatus status;

    @Column(name = "narration")
    private String narration;

    @Column(name = "failure_reason")
    private String failureReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Transfer(
            UUID id,
            String transferReference,
            String idempotencyKey,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency,
            String narration,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id, "Transfer ID must not be null");
        this.transferReference = Objects.requireNonNull(transferReference, "Transfer reference must not be null");
        this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "Idempotency key must not be null");
        this.sourceAccountNumber = Objects.requireNonNull(sourceAccountNumber, "Source account number must not be null");
        this.destinationAccountNumber = Objects.requireNonNull(destinationAccountNumber, "Destination account number must not be null");
        this.amount = Objects.requireNonNull(amount, "Amount must not be null").setScale(SCALE, ROUNDING_MODE);
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
        this.narration = narration;
        this.status = TransferStatus.PENDING;
        this.createdAt = Objects.requireNonNull(now, "CreatedAt must not be null");
        this.updatedAt = now;
        this.version = 0L;
    }

    public static Transfer create(
            String idempotencyKey,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency,
            String narration
    ) {
        if (sourceAccountNumber.equals(destinationAccountNumber)) {
            throw new IllegalArgumentException("Source and destination account numbers must be different");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be strictly greater than zero");
        }
        if (!"NGN".equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Only NGN currency is supported");
        }

        String reference = "TXF_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Instant now = Instant.now();

        return new Transfer(
                UUID.randomUUID(),
                reference,
                idempotencyKey,
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                currency.toUpperCase(),
                narration,
                now
        );
    }

    public void markProcessing() {
        if (this.status != TransferStatus.PENDING) {
            throw new IllegalStateException("Cannot transition to PROCESSING from status: " + this.status);
        }
        this.status = TransferStatus.PROCESSING;
        this.updatedAt = Instant.now();
    }

    public void markSuccessful() {
        if (this.status != TransferStatus.PROCESSING) {
            throw new IllegalStateException("Cannot transition to SUCCESSFUL from status: " + this.status);
        }
        this.status = TransferStatus.SUCCESSFUL;
        this.updatedAt = Instant.now();
    }

    public void markFailed(String reason) {
        if (this.status == TransferStatus.SUCCESSFUL || this.status == TransferStatus.REVERSED) {
            throw new IllegalStateException("Cannot fail a transfer with terminal status: " + this.status);
        }
        this.status = TransferStatus.FAILED;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }
}