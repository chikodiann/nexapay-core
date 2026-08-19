package com.nexapay.account.account.application;

import com.nexapay.account.account.api.dto.BalanceMutationRequest;
import com.nexapay.account.account.api.dto.CreateAccountRequest;
import com.nexapay.account.account.domain.Account;
import com.nexapay.account.account.domain.Currency;
import com.nexapay.account.account.infrastructure.AccountMutationRepository;
import com.nexapay.account.account.infrastructure.AccountRepository;
import com.nexapay.account.common.exception.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AccountConcurrencyIntegrationTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountMutationRepository accountMutationRepository;

    @BeforeEach
    void setUp() {
        // Delete child mutations first to avoid foreign key violations
        accountMutationRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private String createAccountWithBalance(BigDecimal initialBalance) {
        UUID customerId = UUID.randomUUID();
        var response = accountService.createAccount(new CreateAccountRequest(customerId, Currency.NGN));
        String accountNumber = response.accountNumber();

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            accountService.credit(accountNumber, new BalanceMutationRequest(initialBalance, "INIT_" + UUID.randomUUID()));
        }
        return accountNumber;
    }

    @Test
    @DisplayName("1. Should prevent overspending under concurrent debits (5 succeed, 5 fail)")
    void shouldPreventOverspendingUnderConcurrentDebits() throws Exception {
        String accountNumber = createAccountWithBalance(new BigDecimal("100000.00"));
        int workers = 10;
        BigDecimal debitAmount = new BigDecimal("20000.00");

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger insufficientFundsCount = new AtomicInteger(0);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            String ref = "DEBIT_" + UUID.randomUUID();
            tasks.add(() -> {
                ready.countDown();
                start.await();
                try {
                    accountService.debit(accountNumber, new BalanceMutationRequest(debitAmount, ref));
                    successCount.incrementAndGet();
                    return true;
                } catch (InsufficientFundsException ex) {
                    insufficientFundsCount.incrementAndGet();
                    return false;
                }
            });
        }

        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();

        ready.await();
        start.countDown();

        for (Future<Boolean> future : futures) {
            future.get();
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(insufficientFundsCount.get()).isEqualTo(5);

        Account finalAccount = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(finalAccount.getAvailableBalance()).isEqualByComparingTo("0.0000");
        assertThat(finalAccount.getLedgerBalance()).isEqualByComparingTo("0.0000");
    }

    @Test
    @DisplayName("2. Should process concurrent debits without lost updates (20 succeed)")
    void shouldProcessConcurrentDebitsWithoutLostUpdates() throws Exception {
        String accountNumber = createAccountWithBalance(new BigDecimal("1000000.00"));
        int workers = 20;
        BigDecimal debitAmount = new BigDecimal("10000.00");

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            String ref = "VALID_DEBIT_" + UUID.randomUUID();
            tasks.add(() -> {
                ready.countDown();
                start.await();
                accountService.debit(accountNumber, new BalanceMutationRequest(debitAmount, ref));
                return true;
            });
        }

        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();

        ready.await();
        start.countDown();

        for (Future<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }
        executor.shutdown();

        Account finalAccount = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(finalAccount.getAvailableBalance()).isEqualByComparingTo("800000.0000");
        assertThat(finalAccount.getLedgerBalance()).isEqualByComparingTo("800000.0000");
    }

    @Test
    @DisplayName("3. Should process concurrent credits without lost updates (50 credits)")
    void shouldProcessConcurrentCreditsWithoutLostUpdates() throws Exception {
        String accountNumber = createAccountWithBalance(BigDecimal.ZERO);
        int workers = 50;
        BigDecimal creditAmount = new BigDecimal("1000.00");

        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            String ref = "CONCURRENT_CREDIT_" + UUID.randomUUID();
            tasks.add(() -> {
                ready.countDown();
                start.await();
                accountService.credit(accountNumber, new BalanceMutationRequest(creditAmount, ref));
                return true;
            });
        }

        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();

        ready.await();
        start.countDown();

        for (Future<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }
        executor.shutdown();

        Account finalAccount = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(finalAccount.getAvailableBalance()).isEqualByComparingTo("50000.0000");
        assertThat(finalAccount.getLedgerBalance()).isEqualByComparingTo("50000.0000");
    }

    @Test
    @DisplayName("4. Should maintain correct balance during interleaved concurrent credits and debits")
    void shouldMaintainCorrectBalanceDuringConcurrentCreditsAndDebits() throws Exception {
        String accountNumber = createAccountWithBalance(new BigDecimal("100000.00"));
        int operationsPerType = 10;
        int totalWorkers = operationsPerType * 2;
        BigDecimal amount = new BigDecimal("5000.00");

        ExecutorService executor = Executors.newFixedThreadPool(totalWorkers);
        CountDownLatch ready = new CountDownLatch(totalWorkers);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < operationsPerType; i++) {
            String debitRef = "INTERLEAVED_DEBIT_" + UUID.randomUUID();
            tasks.add(() -> {
                ready.countDown();
                start.await();
                accountService.debit(accountNumber, new BalanceMutationRequest(amount, debitRef));
                return true;
            });
        }

        for (int i = 0; i < operationsPerType; i++) {
            String creditRef = "INTERLEAVED_CREDIT_" + UUID.randomUUID();
            tasks.add(() -> {
                ready.countDown();
                start.await();
                accountService.credit(accountNumber, new BalanceMutationRequest(amount, creditRef));
                return true;
            });
        }

        Collections.shuffle(tasks);

        List<Future<Boolean>> futures = tasks.stream().map(executor::submit).toList();

        ready.await();
        start.countDown();

        for (Future<Boolean> future : futures) {
            assertThat(future.get()).isTrue();
        }
        executor.shutdown();

        Account finalAccount = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(finalAccount.getAvailableBalance()).isEqualByComparingTo("100000.0000");
        assertThat(finalAccount.getLedgerBalance()).isEqualByComparingTo("100000.0000");
    }
}