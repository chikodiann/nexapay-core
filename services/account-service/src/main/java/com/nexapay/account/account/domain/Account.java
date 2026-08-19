package com.nexapay.account.account.domain;

import com.nexapay.account.common.exception.InsufficientFundsException;
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
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_EVEN;

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, unique = true, length = 10)
    private String accountNumber;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", nullable = false, length = 3)
    private Currency currency;

    @Column(name = "available_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal availableBalance;

    @Column(name = "ledger_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal ledgerBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Account(
            UUID id,
            String accountNumber,
            UUID customerId,
            Currency currency,
            AccountStatus status,
            Instant now
    ) {
        this.id = Objects.requireNonNull(id, "Account ID must not be null");
        this.accountNumber = Objects.requireNonNull(accountNumber, "Account number must not be null");
        this.customerId = Objects.requireNonNull(customerId, "Customer ID must not be null");
        this.currency = Objects.requireNonNull(currency, "Currency must not be null");
        this.status = Objects.requireNonNull(status, "Status must not be null");
        this.availableBalance = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        this.ledgerBalance = BigDecimal.ZERO.setScale(SCALE, ROUNDING_MODE);
        this.createdAt = Objects.requireNonNull(now, "CreatedAt timestamp must not be null");
        this.updatedAt = now;
        this.version = 0L;
    }

    /**
     * Domain factory method accepting an AccountNumberGenerator (used in unit tests).
     */
    public static Account create(UUID customerId, Currency currency, AccountNumberGenerator generator) {
        Objects.requireNonNull(generator, "AccountNumberGenerator cannot be null");
        return create(generator.generate(), customerId, currency);
    }

    /**
     * Domain factory method accepting a resolved account number (used in application services).
     */
    public static Account create(String accountNumber, UUID customerId, Currency currency) {
        return new Account(
                UUID.randomUUID(),
                accountNumber,
                customerId,
                currency,
                AccountStatus.ACTIVE,
                Instant.now()
        );
    }

    public void updateStatus(AccountStatus newStatus) {
        Objects.requireNonNull(newStatus, "New status must not be null");
        if (this.status == AccountStatus.CLOSED) {
            throw new IllegalStateException("Cannot change status of a CLOSED account");
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void debit(BigDecimal amount) {
        Objects.requireNonNull(amount, "Debit amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be strictly greater than zero");
        }
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot debit an account with status: " + this.status);
        }
        if (this.availableBalance.compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds on account " + this.accountNumber + ". Available: " + this.availableBalance
            );
        }

        this.availableBalance = this.availableBalance.subtract(amount);
        this.ledgerBalance = this.ledgerBalance.subtract(amount);
        this.updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        Objects.requireNonNull(amount, "Credit amount cannot be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be strictly greater than zero");
        }
        if (this.status != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Cannot credit an account with status: " + this.status);
        }

        this.availableBalance = this.availableBalance.add(amount);
        this.ledgerBalance = this.ledgerBalance.add(amount);
        this.updatedAt = Instant.now();
    }
}