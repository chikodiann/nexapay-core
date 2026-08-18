package com.nexapay.payment.transfer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Payload for initiating an internal transfer")
public record InitiateTransferRequest(
        @NotBlank(message = "sourceAccountNumber is mandatory")
        @Pattern(regexp = "^\\d{10}$", message = "sourceAccountNumber must be 10 digits")
        @Schema(example = "1023847291")
        String sourceAccountNumber,

        @NotBlank(message = "destinationAccountNumber is mandatory")
        @Pattern(regexp = "^\\d{10}$", message = "destinationAccountNumber must be 10 digits")
        @Schema(example = "1045678932")
        String destinationAccountNumber,

        @NotNull(message = "amount is mandatory")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        @Schema(example = "25000.00")
        BigDecimal amount,

        @NotBlank(message = "currency is mandatory")
        @Schema(example = "NGN")
        String currency,

        @Size(max = 255, message = "narration must not exceed 255 characters")
        @Schema(example = "Invoice payment")
        String narration
) {}