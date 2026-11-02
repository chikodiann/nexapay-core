package com.nexapay.account.account.application;

import com.nexapay.account.account.api.dto.AccountResponse;
import com.nexapay.account.account.api.dto.BalanceMutationRequest;
import com.nexapay.account.account.api.dto.CreateAccountRequest;
import com.nexapay.account.account.api.dto.LedgerMutationResponse;
import com.nexapay.account.account.api.dto.UpdateAccountStatusRequest;
import com.nexapay.account.account.domain.Account;
import com.nexapay.account.account.domain.AccountNumberGenerator;
import com.nexapay.account.account.domain.AccountMutation;
import com.nexapay.account.account.domain.LedgerMutation;
import com.nexapay.account.account.domain.MutationStatus;
import com.nexapay.account.account.domain.MutationType;
import com.nexapay.account.account.infrastructure.AccountMutationRepository;
import com.nexapay.account.account.infrastructure.AccountRepository;
import com.nexapay.account.account.infrastructure.LedgerMutationRepository;
import com.nexapay.account.common.exception.AccountAlreadyExistsException;
import com.nexapay.account.common.exception.AccountNotFoundException;
import com.nexapay.account.common.exception.LedgerMutationNotFoundException;
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

    private final AccountRepository accountRepository;
    private final AccountMutationRepository accountMutationRepository;
    private final LedgerMutationRepository ledgerMutationRepository;
    private final AccountNumberGenerator accountNumberGenerator;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByCustomerIdAndCurrency(request.customerId(), request.currency())) {
            throw new AccountAlreadyExistsException(
                    "Customer " + request.customerId() + " already has an account for currency " + request.currency()
            );
        }

        String accountNumber = accountNumberGenerator.generate();
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = accountNumberGenerator.generate();
        }

        Account account = Account.create(accountNumber, request.customerId(), request.currency());
        Account savedAccount = accountRepository.save(account);
        log.info("Created account id={} accountNumber={} for customerId={}", savedAccount.getId(), accountNumber, request.customerId());
        return AccountResponse.fromDomain(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(AccountResponse::fromDomain)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found"));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCustomerId(UUID customerId) {
        return accountRepository.findByCustomerId(customerId)
                .stream()
                .map(AccountResponse::fromDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public LedgerMutationResponse getMutationByReference(String reference) {
        return ledgerMutationRepository.findByTransferReference(reference)
                .map(LedgerMutationResponse::fromDomain)
                .orElseThrow(() -> new LedgerMutationNotFoundException("Mutation with reference " + reference + " not found"));
    }

    @Transactional
    public AccountResponse updateAccountStatus(String accountNumber, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found"));

        account.updateStatus(request.status());
        Account updatedAccount = accountRepository.save(account);
        log.info("Updated account number={} status to {}", accountNumber, request.status());
        return AccountResponse.fromDomain(updatedAccount);
    }

    @Transactional
    public AccountResponse debit(String accountNumber, BalanceMutationRequest request) {
        // Idempotency check via LedgerMutation table
        if (request.reference() != null && !request.reference().isBlank()) {
            var existingLedgerMutation = ledgerMutationRepository.findByTransferReference(request.reference());
            if (existingLedgerMutation.isPresent()) {
                LedgerMutation lm = existingLedgerMutation.get();
                if (lm.getStatus() == MutationStatus.APPLIED) {
                    log.info("Idempotent debit replay for account={} ref={}", accountNumber, request.reference());
                    return getAccountByNumber(accountNumber);
                }
            }

            var existingMutation = accountMutationRepository.findByAccountNumberAndReferenceAndMutationType(
                    accountNumber, request.reference(), MutationType.DEBIT
            );
            if (existingMutation.isPresent()) {
                log.info("Idempotent debit replay via legacy mutation for account={} ref={}", accountNumber, request.reference());
                return getAccountByNumber(accountNumber);
            }
        }

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found"));

        try {
            account.debit(request.amount());
            Account updated = accountRepository.save(account);

            if (request.reference() != null && !request.reference().isBlank()) {
                accountMutationRepository.save(
                        AccountMutation.create(accountNumber, request.reference(), MutationType.DEBIT, request.amount())
                );
                ledgerMutationRepository.save(
                        LedgerMutation.create(request.reference(), MutationType.DEBIT, updated.getId(), request.amount(), MutationStatus.APPLIED)
                );
            }

            log.info("Debited {} from account={} reference={}", request.amount(), accountNumber, request.reference());
            return AccountResponse.fromDomain(updated);

        } catch (Exception ex) {
            if (request.reference() != null && !request.reference().isBlank()) {
                if (ledgerMutationRepository.findByTransferReference(request.reference()).isEmpty()) {
                    ledgerMutationRepository.save(
                            LedgerMutation.create(request.reference(), MutationType.DEBIT, account.getId(), request.amount(), MutationStatus.FAILED)
                    );
                }
            }
            throw ex;
        }
    }

    @Transactional
    public AccountResponse credit(String accountNumber, BalanceMutationRequest request) {
        // Idempotency check via LedgerMutation table
        if (request.reference() != null && !request.reference().isBlank()) {
            var existingLedgerMutation = ledgerMutationRepository.findByTransferReference(request.reference());
            if (existingLedgerMutation.isPresent()) {
                LedgerMutation lm = existingLedgerMutation.get();
                if (lm.getStatus() == MutationStatus.APPLIED) {
                    log.info("Idempotent credit replay for account={} ref={}", accountNumber, request.reference());
                    return getAccountByNumber(accountNumber);
                }
            }

            var existingMutation = accountMutationRepository.findByAccountNumberAndReferenceAndMutationType(
                    accountNumber, request.reference(), MutationType.CREDIT
            );
            if (existingMutation.isPresent()) {
                log.info("Idempotent credit replay via legacy mutation for account={} ref={}", accountNumber, request.reference());
                return getAccountByNumber(accountNumber);
            }
        }

        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account " + accountNumber + " not found"));

        try {
            account.credit(request.amount());
            Account updated = accountRepository.save(account);

            if (request.reference() != null && !request.reference().isBlank()) {
                accountMutationRepository.save(
                        AccountMutation.create(accountNumber, request.reference(), MutationType.CREDIT, request.amount())
                );
                ledgerMutationRepository.save(
                        LedgerMutation.create(request.reference(), MutationType.CREDIT, updated.getId(), request.amount(), MutationStatus.APPLIED)
                );
            }

            log.info("Credited {} to account={} reference={}", request.amount(), accountNumber, request.reference());
            return AccountResponse.fromDomain(updated);

        } catch (Exception ex) {
            if (request.reference() != null && !request.reference().isBlank()) {
                if (ledgerMutationRepository.findByTransferReference(request.reference()).isEmpty()) {
                    ledgerMutationRepository.save(
                            LedgerMutation.create(request.reference(), MutationType.CREDIT, account.getId(), request.amount(), MutationStatus.FAILED)
                    );
                }
            }
            throw ex;
        }
    }
}