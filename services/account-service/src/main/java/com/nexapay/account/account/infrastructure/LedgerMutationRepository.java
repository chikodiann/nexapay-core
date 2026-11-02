package com.nexapay.account.account.infrastructure;

import com.nexapay.account.account.domain.LedgerMutation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LedgerMutationRepository extends JpaRepository<LedgerMutation, UUID> {
    Optional<LedgerMutation> findByTransferReference(String transferReference);
}
