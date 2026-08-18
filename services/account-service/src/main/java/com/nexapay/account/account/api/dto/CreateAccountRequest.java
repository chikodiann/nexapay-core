package com.nexapay.account.account.api.dto;

import com.nexapay.account.account.domain.Currency;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Payload for opening a new customer account")
public record CreateAccountRequest(
        @NotNull(message = "customerId is mandatory")
        @Schema(description = "Unique identifier of the customer", example = "0d168787-f22e-4ad1-b4b4-9fb4cf2d4561")
        UUID customerId,

        @NotNull(message = "currency is mandatory")
        @Schema(description = "ISO-4217 Currency code", example = "NGN")
        Currency currency
) {}