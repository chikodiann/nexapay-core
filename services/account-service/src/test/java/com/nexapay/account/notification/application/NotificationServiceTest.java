package com.nexapay.account.notification.application;

import com.nexapay.account.account.api.event.TransferEventPayload;
import com.nexapay.account.notification.domain.NotificationMessage;
import com.nexapay.account.notification.infrastructure.RabbitMqNotificationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("1. Should publish notification to RabbitMQ exchange with transfer.completed routing key")
    void shouldPublishCompletedTransferNotification() {
        TransferEventPayload payload = new TransferEventPayload(
                UUID.randomUUID(),
                "TransferCompleted",
                Instant.now(),
                "TXF_RABBIT_001",
                "1023847291",
                "1045678932",
                new BigDecimal("25000.00"),
                "NGN",
                null
        );

        notificationService.dispatchTransferNotification(payload);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqNotificationConfig.NOTIFICATIONS_EXCHANGE),
                eq("transfer.completed"),
                any(NotificationMessage.class)
        );
    }

    @Test
    @DisplayName("2. Should publish notification to RabbitMQ exchange with transfer.reversed routing key")
    void shouldPublishReversedTransferNotification() {
        TransferEventPayload payload = new TransferEventPayload(
                UUID.randomUUID(),
                "TransferReversed",
                Instant.now(),
                "TXF_RABBIT_002",
                "1023847291",
                "1045678932",
                new BigDecimal("15000.00"),
                "NGN",
                "DESTINATION_CREDIT_FAILED"
        );

        notificationService.dispatchTransferNotification(payload);

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMqNotificationConfig.NOTIFICATIONS_EXCHANGE),
                eq("transfer.reversed"),
                any(NotificationMessage.class)
        );
    }
}