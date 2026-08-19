package com.nexapay.payment.transfer.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String transferReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency
) {
    public static TransferCompletedEvent from(
            String transferReference,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency
    ) {
        return new TransferCompletedEvent(
                UUID.randomUUID(),
                "TransferCompleted",
                Instant.now(),
                transferReference,
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                currency
        );
    }
}