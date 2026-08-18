package com.nexapay.account.account.application;

import com.nexapay.account.account.api.dto.AccountResponse;
import com.nexapay.account.account.api.dto.CreateAccountRequest;
import com.nexapay.account.account.api.dto.UpdateAccountStatusRequest;
import com.nexapay.account.account.domain.Account;
import com.nexapay.account.account.domain.AccountNumberGenerator;
import com.nexapay.account.account.infrastructure.AccountRepository;
import com.nexapay.account.common.exception.AccountNotFoundException;
import com.nexapay.account.common.exception.DuplicateAccountException;
import com.nexapay.account.common.exception.InvalidAccountStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final int MAX_GENERATE_RETRIES = 3;

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByCustomerIdAndCurrency(request.customerId(), request.currency())) {
            throw new DuplicateAccountException(
                    "Customer %s already has an active %s account".formatted(request.customerId(), request.currency())
            );
        }

        // Retry mechanism in the rare event of an account number collision
        Account account = null;
        for (int i = 0; i < MAX_GENERATE_RETRIES; i++) {
            Account candidate = Account.create(request.customerId(), request.currency(), accountNumberGenerator);
            if (!accountRepository.existsByAccountNumber(candidate.getAccountNumber())) {
                account = candidate;
                break;
            }
        }

        if (account == null) {
            throw new IllegalStateException("Failed to generate a unique account number after retries");
        }

        Account saved = accountRepository.save(account);
        log.info("Created account id={} accountNumber={} for customerId={}", saved.getId(), saved.getAccountNumber(), saved.getCustomerId());
        return AccountResponse.fromDomain(saved);
    }

    @Transactional(readOnly = true)
    public AccountResponse getByAccountNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(AccountResponse::fromDomain)
                .orElseThrow(() -> new AccountNotFoundException("Account with number %s not found".formatted(accountNumber)));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId).stream()
                .map(AccountResponse::fromDomain)
                .toList();
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account with number %s not found".formatted(accountNumber)));

        try {
            account.updateStatus(request.status());
        } catch (IllegalStateException ex) {
            throw new InvalidAccountStateException(ex.getMessage());
        }

        Account updated = accountRepository.save(account);
        log.info("Updated account number={} status to {}", accountNumber, request.status());
        return AccountResponse.fromDomain(updated);
    }
}