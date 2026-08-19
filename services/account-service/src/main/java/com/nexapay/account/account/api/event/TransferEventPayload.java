package com.nexapay.account.account.api.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferEventPayload(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String transferReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String reason
) {}