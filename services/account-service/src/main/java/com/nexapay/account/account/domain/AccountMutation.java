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
@Table(name = "account_mutations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AccountMutation {

    @Id
    private UUID id;

    @Column(name = "account_number", nullable = false, length = 10)
    private String accountNumber;

    @Column(name = "reference", nullable = false, length = 64)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "mutation_type", nullable = false, length = 10)
    private MutationType mutationType;

    @Column(name = "amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static AccountMutation create(String accountNumber, String reference, MutationType mutationType, BigDecimal amount) {
        AccountMutation mutation = new AccountMutation();
        mutation.id = UUID.randomUUID();
        mutation.accountNumber = Objects.requireNonNull(accountNumber, "Account number is required");
        mutation.reference = Objects.requireNonNull(reference, "Reference is required");
        mutation.mutationType = Objects.requireNonNull(mutationType, "Mutation type is required");
        mutation.amount = Objects.requireNonNull(amount, "Amount is required");
        mutation.createdAt = Instant.now();
        return mutation;
    }
}