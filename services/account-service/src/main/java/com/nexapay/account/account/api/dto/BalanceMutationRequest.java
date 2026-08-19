package com.nexapay.account.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Payload for internal debit/credit operations")
public record BalanceMutationRequest(
        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Schema(example = "25000.00")
        BigDecimal amount,

        @Schema(example = "Transfer TXF_123456789")
        String reference
) {}