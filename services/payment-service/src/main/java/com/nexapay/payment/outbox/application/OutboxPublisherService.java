package com.nexapay.payment.outbox.application;

import com.nexapay.payment.outbox.domain.OutboxEvent;
import com.nexapay.payment.outbox.domain.OutboxStatus;
import com.nexapay.payment.outbox.infrastructure.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${nexapay.kafka.topics.transfer-events:nexapay.transfer.events}")
    private String transferEventsTopic;

    @Scheduled(fixedDelayString = "${nexapay.outbox.publisher.delay-ms:3000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 50)
        );

        for (OutboxEvent event : pendingEvents) {
            publishSingleEvent(event);
        }
    }

    public void publishSingleEvent(OutboxEvent event) {
        try {
            // Synchronously wait for broker acknowledgment before updating outbox status
            kafkaTemplate.send(transferEventsTopic, event.getAggregateId(), event.getPayload())
                    .get(5, TimeUnit.SECONDS);

            event.markPublished();
            log.info("Outbox event id={} aggregateId={} published to topic={}",
                    event.getId(), event.getAggregateId(), transferEventsTopic);
        } catch (Exception ex) {
            log.warn("Failed to publish outbox event id={} attempt={}: {}",
                    event.getId(), event.getAttempts() + 1, ex.getMessage());
            event.markFailedAttempt(ex.getMessage());
        }
        outboxEventRepository.save(event);
    }
}