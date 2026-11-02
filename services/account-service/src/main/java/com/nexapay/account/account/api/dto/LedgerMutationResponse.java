package com.nexapay.account.account.api.dto;

import com.nexapay.account.account.domain.LedgerMutation;
import com.nexapay.account.account.domain.MutationStatus;
import com.nexapay.account.account.domain.MutationType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerMutationResponse(
        UUID id,
        String transferReference,
        MutationType operationType,
        UUID accountId,
        BigDecimal amount,
        MutationStatus status,
        Instant createdAt
) {
    public static LedgerMutationResponse fromDomain(LedgerMutation mutation) {
        return new LedgerMutationResponse(
                mutation.getId(),
                mutation.getTransferReference(),
                mutation.getOperationType(),
                mutation.getAccountId(),
                mutation.getAmount(),
                mutation.getStatus(),
                mutation.getCreatedAt()
        );
    }
}
