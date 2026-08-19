package com.nexapay.account.account.application.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexapay.account.account.api.event.TransferEventPayload;
import com.nexapay.account.common.consumer.infrastructure.ConsumedMessageRepository;
import com.nexapay.account.notification.application.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class TransferEventConsumerTest {

    @Autowired
    private TransferEventConsumer transferEventConsumer;

    @Autowired
    private ConsumedMessageRepository consumedMessageRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        consumedMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Should process event and record in deduplication store when delivered first time")
    void shouldProcessEventOnFirstDelivery() throws Exception {
        UUID eventId = UUID.randomUUID();
        TransferEventPayload payload = new TransferEventPayload(
                eventId,
                "TransferCompleted",
                Instant.now(),
                "TXF_TEST_CONSUMER_1",
                "1023847291",
                "1045678932",
                new BigDecimal("50000.00"),
                "NGN",
                null
        );

        String json = objectMapper.writeValueAsString(payload);
        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_CONSUMER_1");

        // Assert notification dispatched exactly once
        verify(notificationService, times(1)).dispatchTransferNotification(any());

        // Assert recorded in consumed_messages table
        assertThat(consumedMessageRepository.existsByConsumerNameAndEventId(
                TransferEventConsumer.CONSUMER_NAME, eventId
        )).isTrue();
    }

    @Test
    @DisplayName("2. Should drop duplicate event without repeating business execution (At-least-once guard)")
    void shouldDropDuplicateEventWithoutReexecution() throws Exception {
        UUID eventId = UUID.randomUUID();
        TransferEventPayload payload = new TransferEventPayload(
                eventId,
                "TransferCompleted",
                Instant.now(),
                "TXF_TEST_CONSUMER_2",
                "1023847291",
                "1045678932",
                new BigDecimal("30000.00"),
                "NGN",
                null
        );

        String json = objectMapper.writeValueAsString(payload);

        // First delivery
        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_CONSUMER_2");
        // Replayed delivery (e.g., Kafka retry or rebalance)
        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_CONSUMER_2");

        // Assert notification dispatched only once across both invocations
        verify(notificationService, times(1)).dispatchTransferNotification(any());
        assertThat(consumedMessageRepository.count()).isEqualTo(1);
    }
}