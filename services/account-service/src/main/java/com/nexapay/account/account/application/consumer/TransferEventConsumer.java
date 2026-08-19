package com.nexapay.account.account.application.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexapay.account.account.api.event.TransferEventPayload;
import com.nexapay.account.common.consumer.domain.ConsumedMessage;
import com.nexapay.account.common.consumer.infrastructure.ConsumedMessageRepository;
import com.nexapay.account.notification.application.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransferEventConsumer {

    public static final String CONSUMER_NAME = "account-service-notification-consumer";

    private final ConsumedMessageRepository consumedMessageRepository;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${nexapay.kafka.topics.transfer-events:nexapay.transfer.events}",
            groupId = "${spring.kafka.consumer.group-id:account-service-group}"
    )
    @Transactional
    public void handleTransferEvent(
            @Payload String rawJson,
            @Header(KafkaHeaders.RECEIVED_KEY) String messageKey
    ) {
        try {
            TransferEventPayload payload = objectMapper.readValue(rawJson, TransferEventPayload.class);

            // 1. Idempotency Check: Drop duplicate deliveries
            if (consumedMessageRepository.existsByConsumerNameAndEventId(CONSUMER_NAME, payload.eventId())) {
                log.info("IDEMPOTENT_CONSUMER_DROP: Duplicate eventId={} ref={} on consumer={}",
                        payload.eventId(), payload.transferReference(), CONSUMER_NAME);
                return;
            }

            // 2. Business Execution
            notificationService.dispatchTransferNotification(payload);

            // 3. Mark Consumed in Deduplication Store (atomic with transaction)
            consumedMessageRepository.save(
                    ConsumedMessage.record(
                            CONSUMER_NAME,
                            payload.eventId(),
                            payload.eventType(),
                            messageKey != null ? messageKey : payload.transferReference()
                    )
            );

            log.info("CONSUMER_PROCESSED: Successfully processed eventId={} ref={}",
                    payload.eventId(), payload.transferReference());

        } catch (Exception ex) {
            log.error("CONSUMER_ERROR: Failed to process transfer event payload: {}", rawJson, ex);
            throw new RuntimeException("Consumer processing failed", ex);
        }
    }
}