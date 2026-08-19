package com.nexapay.payment.transfer.api;

import com.nexapay.payment.transfer.api.dto.InitiateTransferRequest;
import com.nexapay.payment.transfer.api.dto.TransferResponse;
import com.nexapay.payment.transfer.application.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Transfer Management", description = "Endpoints for initiating and tracking fund transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    @Operation(summary = "Initiate a fund transfer", description = "Executes transfer between accounts with idempotency protection")
    @ApiResponse(responseCode = "201", description = "Transfer processed")
    @ApiResponse(responseCode = "400", description = "Invalid transfer invariants")
    @ApiResponse(responseCode = "404", description = "Account not found")
    @ApiResponse(responseCode = "409", description = "Idempotency key collision with different parameters")
    @ApiResponse(responseCode = "422", description = "Insufficient funds or inactive account")
    public ResponseEntity<TransferResponse> initiateTransfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody InitiateTransferRequest request
    ) {
        TransferResponse response = transferService.initiateTransfer(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{transferReference}")
    @Operation(summary = "Get transfer by reference", description = "Retrieves current status and details of a transfer")
    @ApiResponse(responseCode = "200", description = "Transfer found")
    @ApiResponse(responseCode = "404", description = "Transfer reference not found")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String transferReference) {
        TransferResponse response = transferService.getTransferByReference(transferReference);
        return ResponseEntity.ok(response);
    }
}