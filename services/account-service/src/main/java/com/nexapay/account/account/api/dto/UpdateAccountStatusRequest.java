package com.nexapay.account.account.api.dto;

import com.nexapay.account.account.domain.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload for transitioning account status")
public record UpdateAccountStatusRequest(
        @NotNull(message = "status is mandatory")
        @Schema(description = "Target status", example = "FROZEN")
        AccountStatus status
) {}