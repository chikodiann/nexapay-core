package com.nexapay.payment.transfer.application;

import com.nexapay.payment.common.exception.DuplicateIdempotencyKeyException;
import com.nexapay.payment.common.exception.InactiveAccountException;
import com.nexapay.payment.common.exception.InsufficientFundsException;
import com.nexapay.payment.common.exception.InvalidTransferException;
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

        var existing = transferRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            Transfer t = existing.get();
            if (t.getSourceAccountNumber().equals(request.sourceAccountNumber())
                    && t.getDestinationAccountNumber().equals(request.destinationAccountNumber())
                    && t.getAmount().compareTo(request.amount()) == 0) {
                log.info("Idempotent request returning existing transfer reference={}", t.getTransferReference());
                return TransferResponse.fromDomain(t);
            }
            throw new DuplicateIdempotencyKeyException("Idempotency key reused with different payload parameters");
        }

        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidTransferException("Source and destination accounts cannot be identical");
        }
        if (!"NGN".equalsIgnoreCase(request.currency())) {
            throw new InvalidTransferException("Only NGN currency is supported");
        }

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

        Transfer transfer = Transfer.create(
                idempotencyKey,
                request.sourceAccountNumber(),
                request.destinationAccountNumber(),
                request.amount(),
                request.currency(),
                request.narration()
        );

        Transfer saved = transferRepository.save(transfer);
        log.info("Persisted transfer reference={} status=PENDING", saved.getTransferReference());

        return TransferResponse.fromDomain(saved);
    }
}