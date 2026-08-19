package com.nexapay.account.common.consumer.infrastructure;

import com.nexapay.account.common.consumer.domain.ConsumedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ConsumedMessageRepository extends JpaRepository<ConsumedMessage, UUID> {
    boolean existsByConsumerNameAndEventId(String consumerName, UUID eventId);
}