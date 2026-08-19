package com.nexapay.payment.outbox.application;

import com.nexapay.payment.outbox.domain.OutboxEvent;
import com.nexapay.payment.outbox.domain.OutboxStatus;
import com.nexapay.payment.outbox.infrastructure.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublisherTest {

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
    }

    @Test
    @DisplayName("1. shouldMarkOutboxEventPublishedAfterSuccessfulKafkaSend")
    void shouldMarkOutboxEventPublishedAfterSuccessfulKafkaSend() {
        OutboxEvent event = outboxEventRepository.save(
                OutboxEvent.pending("Transfer", "TXF_TEST_101", "TransferCompleted", "{\"amount\": 5000}")
        );

        @SuppressWarnings("unchecked")
        SendResult<String, String> sendResult = mock(SendResult.class);
        when(kafkaTemplate.send(anyString(), eq("TXF_TEST_101"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        outboxPublisherService.publishPendingEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
        assertThat(updated.getLastError()).isNull();
    }

    @Test
    @DisplayName("2. shouldLeaveEventPendingAndIncrementAttemptsWhenKafkaPublishFails")
    void shouldLeaveEventPendingWhenKafkaPublishFails() {
        OutboxEvent event = outboxEventRepository.save(
                OutboxEvent.pending("Transfer", "TXF_TEST_102", "TransferCompleted", "{\"amount\": 5000}")
        );

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka connection refused"));
        when(kafkaTemplate.send(anyString(), eq("TXF_TEST_102"), anyString())).thenReturn(failedFuture);

        outboxPublisherService.publishPendingEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(updated.getAttempts()).isEqualTo(1);
        assertThat(updated.getLastError()).contains("Kafka connection refused");
    }

    @Test
    @DisplayName("3. shouldMarkEventFailedWhenMaxAttemptsExceeded")
    void shouldMarkEventFailedWhenMaxAttemptsExceeded() {
        OutboxEvent event = OutboxEvent.pending("Transfer", "TXF_TEST_103", "TransferCompleted", "{\"amount\": 5000}");
        for (int i = 0; i < 4; i++) {
            event.markFailedAttempt("Prior attempt failure");
        }
        outboxEventRepository.save(event);

        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka still down"));
        when(kafkaTemplate.send(anyString(), eq("TXF_TEST_103"), anyString())).thenReturn(failedFuture);

        outboxPublisherService.publishPendingEvents();

        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(updated.getAttempts()).isEqualTo(5);
    }
}