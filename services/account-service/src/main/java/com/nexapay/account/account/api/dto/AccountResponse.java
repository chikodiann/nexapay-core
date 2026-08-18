package com.nexapay.account.account.api.dto;

import com.nexapay.account.account.domain.Account;
import com.nexapay.account.account.domain.AccountStatus;
import com.nexapay.account.account.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Account details response")
public record AccountResponse(
        UUID id,
        String accountNumber,
        UUID customerId,
        Currency currency,
        BigDecimal availableBalance,
        BigDecimal ledgerBalance,
        AccountStatus status,
        Instant createdAt
) {
    public static AccountResponse fromDomain(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomerId(),
                account.getCurrency(),
                account.getAvailableBalance().setScale(2, RoundingMode.HALF_EVEN),
                account.getLedgerBalance().setScale(2, RoundingMode.HALF_EVEN),
                account.getStatus(),
                account.getCreatedAt()
        );
    }
}