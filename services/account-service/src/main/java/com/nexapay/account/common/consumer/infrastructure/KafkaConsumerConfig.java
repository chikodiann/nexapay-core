package com.nexapay.account.common.consumer.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaOperations<Object, Object> kafkaOperations) {
        // Route exhausted failures to <original-topic>.DLT with partition preservation
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, ex) -> {
                    String dltTopic = record.topic() + ".DLT";
                    log.error("CONSUMER_EXHAUSTED: Routing event key={} from topic={} to DLT topic={} due to: {}",
                            record.key(), record.topic(), dltTopic, ex.getMessage());
                    return new TopicPartition(dltTopic, record.partition());
                }
        );

        // Bounded Exponential Backoff: initial 1000ms, multiplier 2.0, max 3 attempts
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxElapsedTime(10000L);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        
        // Log each retry attempt
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> 
            log.warn("CONSUMER_RETRY: Attempt #{} for key={} topic={} error={}", 
                    deliveryAttempt, record.key(), record.topic(), ex.getMessage())
        );

        return errorHandler;
    }
}