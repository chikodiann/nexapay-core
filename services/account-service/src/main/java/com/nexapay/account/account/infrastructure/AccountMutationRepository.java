package com.nexapay.account.account.infrastructure;

import com.nexapay.account.account.domain.AccountMutation;
import com.nexapay.account.account.domain.MutationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountMutationRepository extends JpaRepository<AccountMutation, UUID> {
    Optional<AccountMutation> findByAccountNumberAndReferenceAndMutationType(
            String accountNumber,
            String reference,
            MutationType mutationType
    );
}