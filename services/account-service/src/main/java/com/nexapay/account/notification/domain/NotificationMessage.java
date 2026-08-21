package com.nexapay.account.notification.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record NotificationMessage(
        UUID notificationId,
        String transferReference,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        String eventType,
        Instant timestamp,
        String status
) {
    public static NotificationMessage from(
            String transferReference,
            String sourceAccountNumber,
            String destinationAccountNumber,
            BigDecimal amount,
            String currency,
            String eventType
    ) {
        return new NotificationMessage(
                UUID.randomUUID(),
                transferReference,
                sourceAccountNumber,
                destinationAccountNumber,
                amount,
                currency,
                eventType,
                Instant.now(),
                "PENDING"
        );
    }
}