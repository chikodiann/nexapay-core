package com.nexapay.account.notification.application;

import com.nexapay.account.notification.domain.NotificationMessage;
import com.nexapay.account.notification.infrastructure.RabbitMqNotificationConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationListener {

    @RabbitListener(
            queues = RabbitMqNotificationConfig.NOTIFICATIONS_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleNotification(NotificationMessage message) {
        log.info("RABBITMQ_NOTIFICATION_CONSUMED: id={} ref={} type={} amount={} {}",
                message.notificationId(), message.transferReference(), message.eventType(),
                message.amount(), message.currency());

        // Simulated operational delivery logic (e.g. Email/SMS provider integration)
        deliverCustomerAlert(message);
    }

    private void deliverCustomerAlert(NotificationMessage message) {
        if (message.amount() == null || message.amount().signum() <= 0) {
            log.error("POISON_PILL_NOTIFICATION: Invalid amount on notification id={}", message.notificationId());
            throw new IllegalArgumentException("Invalid amount for notification delivery");
        }
        log.info("ALERT_DISPATCHED_TO_CUSTOMER: Transfer ref={} alert sent successfully", message.transferReference());
    }
}