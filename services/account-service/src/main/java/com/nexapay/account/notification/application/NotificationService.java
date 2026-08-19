package com.nexapay.account.notification.application;

import com.nexapay.account.account.api.event.TransferEventPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void dispatchTransferNotification(TransferEventPayload payload) {
        log.info("DISPATCH_NOTIFICATION: type={} ref={} amount={} {} source={} dest={}",
                payload.eventType(),
                payload.transferReference(),
                payload.amount(),
                payload.currency(),
                payload.sourceAccountNumber(),
                payload.destinationAccountNumber()
        );
    }
}