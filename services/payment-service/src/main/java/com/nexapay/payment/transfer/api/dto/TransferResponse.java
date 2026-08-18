package com.nexapay.payment.transfer.api.dto;

import com.nexapay.payment.transfer.domain.Transfer;
import com.nexapay.payment.transfer.domain.TransferStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Transfer transaction details")
public record TransferResponse(
        UUID id,
        String transferReference,
        String idempotencyKey,
        String sourceAccountNumber,
        String destinationAccountNumber,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String narration,
        String failureReason,
        Instant createdAt
) {
    public static TransferResponse fromDomain(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getTransferReference(),
                transfer.getIdempotencyKey(),
                transfer.getSourceAccountNumber(),
                transfer.getDestinationAccountNumber(),
                transfer.getAmount().setScale(2, RoundingMode.HALF_EVEN),
                transfer.getCurrency(),
                transfer.getStatus(),
                transfer.getNarration(),
                transfer.getFailureReason(),
                transfer.getCreatedAt()
        );
    }
}