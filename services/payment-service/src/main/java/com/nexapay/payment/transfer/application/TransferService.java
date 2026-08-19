package com.nexapay.payment.transfer.application;

import com.nexapay.payment.common.exception.AccountMutationRejectedException;
import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.AccountServiceUnavailableException;
import com.nexapay.payment.common.exception.DuplicateIdempotencyKeyException;
import com.nexapay.payment.common.exception.InactiveAccountException;
import com.nexapay.payment.common.exception.InsufficientFundsException;
import com.nexapay.payment.common.exception.InvalidTransferException;
import com.nexapay.payment.common.exception.TransferNotFoundException;
import com.nexapay.payment.transfer.api.dto.InitiateTransferRequest;
import com.nexapay.payment.transfer.api.dto.TransferResponse;
import com.nexapay.payment.transfer.domain.Transfer;
import com.nexapay.payment.transfer.infrastructure.TransferRepository;
import com.nexapay.payment.transfer.infrastructure.client.AccountDto;
import com.nexapay.payment.transfer.infrastructure.client.AccountServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountServiceClient accountServiceClient;

    @Transactional
    public TransferResponse initiateTransfer(String idempotencyKey, InitiateTransferRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }

        // 1. Idempotency Check
        var existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transfer t = existing.get();
            if (t.getSourceAccountNumber().equals(request.sourceAccountNumber())
                    && t.getDestinationAccountNumber().equals(request.destinationAccountNumber())
                    && t.getAmount().compareTo(request.amount()) == 0) {
                log.info("Idempotent replay for transfer ref={}", t.getTransferReference());
                return TransferResponse.fromDomain(t);
            }
            throw new DuplicateIdempotencyKeyException("Idempotency key reused with different payload parameters");
        }

        // 2. Local Invariants Validation
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidTransferException("Source and destination accounts cannot be identical");
        }
        if (!"NGN".equalsIgnoreCase(request.currency())) {
            throw new InvalidTransferException("Only NGN currency is supported");
        }

        // 3. Pre-flight Verification over HTTP
        AccountDto source = accountServiceClient.getAccount(request.sourceAccountNumber());
        AccountDto destination = accountServiceClient.getAccount(request.destinationAccountNumber());

        if (!"ACTIVE".equalsIgnoreCase(source.status())) {
            throw new InactiveAccountException("Source account " + source.accountNumber() + " is " + source.status());
        }
        if (!"ACTIVE".equalsIgnoreCase(destination.status())) {
            throw new InactiveAccountException("Destination account " + destination.accountNumber() + " is " + destination.status());
        }
        if (!request.currency().equalsIgnoreCase(source.currency()) || !request.currency().equalsIgnoreCase(destination.currency())) {
            throw new InvalidTransferException("Account currency mismatch");
        }
        if (source.availableBalance().compareTo(request.amount()) < 0) {
            throw new InsufficientFundsException("Insufficient available balance on account " + source.accountNumber());
        }

        // 4. Persist PENDING Transfer
        Transfer transfer = Transfer.create(
                idempotencyKey,
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.currency(),
                request.narration()
        );
        Transfer saved = transferRepository.saveAndFlush(transfer);
        log.info("Persisted transfer ref={} status=PENDING", saved.getTransferReference());

        // 5. Execute Execution & Compensation Flow
        return executeTransfer(saved);
    }

    private TransferResponse executeTransfer(Transfer transfer) {
        String transferRef = transfer.getTransferReference();
        transfer.markProcessing();
        transferRepository.saveAndFlush(transfer);

        // Step 1: Debit Source Account
        try {
            accountServiceClient.debit(transfer.getSourceAccountNumber(), transfer.getAmount(), transferRef);
            transfer.markSourceDebited();
            transferRepository.saveAndFlush(transfer);
            log.info("Source account debited for transfer ref={}", transferRef);
        } catch (Exception ex) {
            log.error("Source debit failed for transfer ref={}: {}", transferRef, ex.getMessage());
            transfer.markFailed("SOURCE_DEBIT_FAILED: " + ex.getMessage());
            return TransferResponse.fromDomain(transferRepository.save(transfer));
        }

        // Step 2: Credit Destination Account
        try {
            accountServiceClient.credit(transfer.getDestinationAccountNumber(), transfer.getAmount(), transferRef);
            transfer.markDestinationCredited();
            transfer.markSuccessful();
            log.info("Destination account credited, transfer ref={} marked SUCCESSFUL", transferRef);
            return TransferResponse.fromDomain(transferRepository.save(transfer));
        } catch (Exception creditFailure) {
            log.warn("Destination credit failed for transfer ref={}. Initiating compensation: {}", transferRef, creditFailure.getMessage());
            return compensate(transfer, creditFailure);
        }
    }

    private TransferResponse compensate(Transfer transfer, Exception originalFailure) {
        String reversalRef = transfer.getTransferReference() + ":REVERSAL";

        try {
            accountServiceClient.credit(transfer.getSourceAccountNumber(), transfer.getAmount(), reversalRef);
            transfer.markCompensated();
            transfer.markReversed("DESTINATION_CREDIT_FAILED: " + originalFailure.getMessage());
            log.info("Compensation completed successfully for transfer ref={}", transfer.getTransferReference());
            return TransferResponse.fromDomain(transferRepository.save(transfer));
        } catch (Exception compensationFailure) {
            log.error("CRITICAL: Compensation failed for transfer ref={}. Marked for reconciliation: {}",
                    transfer.getTransferReference(), compensationFailure.getMessage());
            transfer.markFailed("COMPENSATION_FAILED: " + compensationFailure.getMessage());
            return TransferResponse.fromDomain(transferRepository.save(transfer));
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferByReference(String transferReference) {
        Transfer transfer = transferRepository.findByTransferReference(transferReference)
                .orElseThrow(() -> new TransferNotFoundException("Transfer with reference " + transferReference + " not found"));
        return TransferResponse.fromDomain(transfer);
    }
}