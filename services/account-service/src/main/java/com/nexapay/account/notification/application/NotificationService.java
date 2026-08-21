package com.nexapay.account.notification.application;

import com.nexapay.account.account.api.event.TransferEventPayload;
import com.nexapay.account.notification.domain.NotificationMessage;
import com.nexapay.account.notification.infrastructure.RabbitMqNotificationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;

    public void dispatchTransferNotification(TransferEventPayload payload) {
        String routingKey = "TransferReversed".equalsIgnoreCase(payload.eventType())
                ? RabbitMqNotificationConfig.ROUTING_KEY_REVERSED
                : RabbitMqNotificationConfig.ROUTING_KEY_COMPLETED;

        NotificationMessage message = NotificationMessage.from(
                payload.transferReference(),
                payload.sourceAccountNumber(),
                payload.destinationAccountNumber(),
                payload.amount(),
                payload.currency(),
                payload.eventType()
        );

        log.info("DISPATCHING_RABBITMQ_NOTIFICATION: ref={} type={} routingKey={}",
                payload.transferReference(), payload.eventType(), routingKey);

        rabbitTemplate.convertAndSend(
                RabbitMqNotificationConfig.NOTIFICATIONS_EXCHANGE,
                routingKey,
                message
        );
    }
}