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
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

    @MockBean
    private KafkaOperations<Object, Object> kafkaOperations;

    @BeforeEach
    void setUp() {
        consumedMessageRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Should process event and record in deduplication store on first delivery")
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

        verify(notificationService, times(1)).dispatchTransferNotification(any());
        assertThat(consumedMessageRepository.existsByConsumerNameAndEventId(
                TransferEventConsumer.CONSUMER_NAME, eventId
        )).isTrue();
    }

    @Test
    @DisplayName("2. Should drop duplicate event without repeating business execution")
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

        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_CONSUMER_2");
        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_CONSUMER_2");

        verify(notificationService, times(1)).dispatchTransferNotification(any());
        assertThat(consumedMessageRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Should NOT record in consumed_messages when processing fails so retry remains possible")
    void shouldNotRecordConsumedMessageOnProcessingFailure() throws Exception {
        UUID eventId = UUID.randomUUID();
        TransferEventPayload payload = new TransferEventPayload(
                eventId,
                "TransferCompleted",
                Instant.now(),
                "TXF_TEST_FAIL_1",
                "1023847291",
                "1045678932",
                new BigDecimal("10000.00"),
                "NGN",
                null
        );

        doThrow(new RuntimeException("Downstream notification provider timeout"))
                .when(notificationService).dispatchTransferNotification(any());

        String json = objectMapper.writeValueAsString(payload);

        assertThatThrownBy(() -> transferEventConsumer.handleTransferEvent(json, "TXF_TEST_FAIL_1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Downstream notification provider timeout");

        // Invariant check: Not recorded in deduplication table
        assertThat(consumedMessageRepository.existsByConsumerNameAndEventId(
                TransferEventConsumer.CONSUMER_NAME, eventId
        )).isFalse();
    }

    @Test
    @DisplayName("4. Should succeed and record in consumed_messages when transient failure recovers on retry")
    void shouldProcessAndRecordWhenRetrySucceeds() throws Exception {
        UUID eventId = UUID.randomUUID();
        TransferEventPayload payload = new TransferEventPayload(
                eventId,
                "TransferCompleted",
                Instant.now(),
                "TXF_TEST_RECOVER_1",
                "1023847291",
                "1045678932",
                new BigDecimal("15000.00"),
                "NGN",
                null
        );

        String json = objectMapper.writeValueAsString(payload);

        // Attempt 1 fails
        doThrow(new RuntimeException("Temporary timeout"))
                .doNothing() // Attempt 2 succeeds
                .when(notificationService).dispatchTransferNotification(any());

        assertThatThrownBy(() -> transferEventConsumer.handleTransferEvent(json, "TXF_TEST_RECOVER_1"))
                .isInstanceOf(RuntimeException.class);

        assertThat(consumedMessageRepository.existsByConsumerNameAndEventId(
                TransferEventConsumer.CONSUMER_NAME, eventId
        )).isFalse();

        // Attempt 2 (Retry)
        transferEventConsumer.handleTransferEvent(json, "TXF_TEST_RECOVER_1");

        verify(notificationService, times(2)).dispatchTransferNotification(any());
        assertThat(consumedMessageRepository.existsByConsumerNameAndEventId(
                TransferEventConsumer.CONSUMER_NAME, eventId
        )).isTrue();
    }
}