package com.nexapay.payment.transfer.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerMutationDto(
        UUID id,
        String transferReference,
        String operationType,
        UUID accountId,
        BigDecimal amount,
        String status,
        Instant createdAt
) {}
