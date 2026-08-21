package com.nexapay.account.account.application.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TransferDeadLetterConsumer {

    @KafkaListener(
            topics = "${nexapay.kafka.topics.transfer-events-dlt:nexapay.transfer.events.DLT}",
            groupId = "${spring.kafka.consumer.dlt-group-id:account-service-dlt-group}"
    )
    public void handleDeadLetterMessage(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key,
            @Header(value = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage
    ) {
        log.error("DEAD_LETTER_RECEIVED: key={} originalTopic={} exceptionMessage={} payload={}",
                key, originalTopic, exceptionMessage, payload);
    }
}