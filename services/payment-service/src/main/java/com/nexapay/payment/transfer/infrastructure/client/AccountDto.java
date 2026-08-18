package com.nexapay.payment.transfer.infrastructure.client;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountDto(
        UUID id,
        String accountNumber,
        UUID customerId,
        String currency,
        BigDecimal availableBalance,
        BigDecimal ledgerBalance,
        String status
) {}