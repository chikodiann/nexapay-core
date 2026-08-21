package com.nexapay.account.notification.application;

import com.nexapay.account.notification.domain.NotificationMessage;
import com.nexapay.account.notification.infrastructure.RabbitMqNotificationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DeadLetterNotificationListener {

    @RabbitListener(
            queues = RabbitMqNotificationConfig.NOTIFICATIONS_DLQ,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleDeadLetterNotification(NotificationMessage deadMessage) {
        log.error("RABBITMQ_DLQ_RECEIVED: Notification failed delivery. id={} ref={} type={}",
                deadMessage.notificationId(), deadMessage.transferReference(), deadMessage.eventType());
    }
}