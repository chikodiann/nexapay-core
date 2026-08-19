package com.nexapay.payment.transfer.infrastructure.client;

import java.math.BigDecimal;

public record BalanceMutationDto(BigDecimal amount, String reference) {}