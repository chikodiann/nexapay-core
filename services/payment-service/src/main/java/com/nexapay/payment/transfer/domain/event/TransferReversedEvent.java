package com.nexapay.payment.transfer.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferReversedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String transferReference,
        String sourceAccountNumber,
        BigDecimal amount,
        String currency,
        String reason
) {
    public static TransferReversedEvent from(
            String transferReference,
            String sourceAccountNumber,
            BigDecimal amount,
            String currency,
            String reason
    ) {
        return new TransferReversedEvent(
                UUID.randomUUID(),
                "TransferReversed",
                Instant.now(),
                transferReference,
                sourceAccountNumber,
                amount,
                currency,
                reason
        );
    }
}