package com.nexapay.payment.transfer.api;

import com.nexapay.payment.transfer.api.dto.InitiateTransferRequest;
import com.nexapay.payment.transfer.api.dto.TransferResponse;
import com.nexapay.payment.transfer.application.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer Operations", description = "Initiate and track internal account transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Initiate an internal transfer", description = "Validates accounts and creates a pending transfer")
    @ApiResponse(responseCode = "201", description = "Transfer initiated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload or currency")
    @ApiResponse(responseCode = "404", description = "Source or destination account not found")
    @ApiResponse(responseCode = "409", description = "Idempotency key payload mismatch")
    @ApiResponse(responseCode = "422", description = "Inactive account or insufficient funds")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @Parameter(description = "Client generated idempotency UUID", required = true)
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiateTransferRequest request
    ) {
        TransferResponse response = transferService.initiateTransfer(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}