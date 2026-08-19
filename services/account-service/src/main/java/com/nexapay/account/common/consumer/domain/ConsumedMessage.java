package com.nexapay.account.common.consumer.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "consumed_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsumedMessage {

    @Id
    private UUID id;

    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "message_key", nullable = false, length = 100)
    private String messageKey;

    @Column(name = "consumed_at", nullable = false, updatable = false)
    private Instant consumedAt;

    public static ConsumedMessage record(String consumerName, UUID eventId, String eventType, String messageKey) {
        ConsumedMessage message = new ConsumedMessage();
        message.id = UUID.randomUUID();
        message.consumerName = Objects.requireNonNull(consumerName, "consumerName must not be null");
        message.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        message.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        message.messageKey = Objects.requireNonNull(messageKey, "messageKey must not be null");
        message.consumedAt = Instant.now();
        return message;
    }
}