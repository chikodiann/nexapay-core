package com.nexapay.account.account.infrastructure;

import com.nexapay.account.account.domain.Account;
import com.nexapay.account.account.domain.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByCustomerId(UUID customerId);

    boolean existsByCustomerIdAndCurrency(UUID customerId, Currency currency);

    boolean existsByAccountNumber(String accountNumber);
}