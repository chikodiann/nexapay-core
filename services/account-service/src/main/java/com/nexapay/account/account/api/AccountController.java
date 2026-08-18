package com.nexapay.account.account.api;

import com.nexapay.account.account.api.dto.AccountResponse;
import com.nexapay.account.account.api.dto.CreateAccountRequest;
import com.nexapay.account.account.api.dto.UpdateAccountStatusRequest;
import com.nexapay.account.account.application.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Endpoints for managing customer accounts and lifecycle states")
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/accounts")
    @Operation(summary = "Open a new account", description = "Creates a new zero-balance account for a registered customer")
    @ApiResponse(responseCode = "201", description = "Account created successfully")
    @ApiResponse(responseCode = "409", description = "Customer already has an account in this currency")
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/accounts/{accountNumber}")
    @Operation(summary = "Get account by account number")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found")
    public ResponseEntity<AccountResponse> getByAccountNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getByAccountNumber(accountNumber));
    }

    @GetMapping("/customers/{customerId}/accounts")
    @Operation(summary = "List customer accounts")
    @ApiResponse(responseCode = "200", description = "List of customer accounts")
    public ResponseEntity<List<AccountResponse>> getCustomerAccounts(@PathVariable UUID customerId) {
        return ResponseEntity.ok(accountService.getAccountsByCustomerId(customerId));
    }

    @PatchMapping("/accounts/{accountNumber}/status")
    @Operation(summary = "Update account status", description = "Transition status between ACTIVE, FROZEN, CLOSED")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @ApiResponse(responseCode = "422", description = "Invalid status transition")
    public ResponseEntity<AccountResponse> updateStatus(
            @PathVariable String accountNumber,
            @Valid @RequestBody UpdateAccountStatusRequest request
    ) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, request));
    }
}