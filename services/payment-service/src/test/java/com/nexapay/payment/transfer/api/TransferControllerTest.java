package com.nexapay.payment.transfer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.transfer.api.dto.InitiateTransferRequest;
import com.nexapay.payment.transfer.infrastructure.TransferRepository;
import com.nexapay.payment.transfer.infrastructure.client.AccountDto;
import com.nexapay.payment.transfer.infrastructure.client.AccountServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TransferRepository transferRepository;

    @MockBean
    private AccountServiceClient accountServiceClient;

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
        // Method calls must live inside method blocks, not at the class level
        doNothing().when(accountServiceClient).debit(anyString(), any(), anyString());
        doNothing().when(accountServiceClient).credit(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("Should successfully initiate transfer when accounts are active and balance is sufficient")
    void shouldInitiateTransferSuccessfully() throws Exception {
        String src = "1023847291";
        String dst = "1045678932";

        when(accountServiceClient.getAccount(eq(src))).thenReturn(
                new AccountDto(UUID.randomUUID(), src, UUID.randomUUID(), "NGN", new BigDecimal("50000.00"), new BigDecimal("50000.00"), "ACTIVE")
        );
        when(accountServiceClient.getAccount(eq(dst))).thenReturn(
                new AccountDto(UUID.randomUUID(), dst, UUID.randomUUID(), "NGN", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "ACTIVE")
        );

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("25000.00"), "NGN", "Invoice payment");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-key-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transferReference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.amount").value(25000.00));
    }

    @Test
    @DisplayName("Should return 422 when source account has insufficient funds")
    void shouldRejectWhenInsufficientFunds() throws Exception {
        String src = "1023847291";
        String dst = "1045678932";

        when(accountServiceClient.getAccount(eq(src))).thenReturn(
                new AccountDto(UUID.randomUUID(), src, UUID.randomUUID(), "NGN", new BigDecimal("5000.00"), new BigDecimal("5000.00"), "ACTIVE")
        );
        when(accountServiceClient.getAccount(eq(dst))).thenReturn(
                new AccountDto(UUID.randomUUID(), dst, UUID.randomUUID(), "NGN", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "ACTIVE")
        );

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("25000.00"), "NGN", "Invoice payment");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-key-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Should return 404 when destination account is not found")
    void shouldRejectWhenDestinationNotFound() throws Exception {
        String src = "1023847291";
        String dst = "9999999999";

        when(accountServiceClient.getAccount(eq(src))).thenReturn(
                new AccountDto(UUID.randomUUID(), src, UUID.randomUUID(), "NGN", new BigDecimal("50000.00"), new BigDecimal("50000.00"), "ACTIVE")
        );
        when(accountServiceClient.getAccount(eq(dst))).thenThrow(new AccountNotFoundException("Account not found"));

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("25000.00"), "NGN", "Invoice payment");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-key-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return existing transfer on identical idempotent retry")
    void shouldReturnExistingTransferOnDuplicateIdempotencyKey() throws Exception {
        String src = "1023847291";
        String dst = "1045678932";

        when(accountServiceClient.getAccount(eq(src))).thenReturn(
                new AccountDto(UUID.randomUUID(), src, UUID.randomUUID(), "NGN", new BigDecimal("50000.00"), new BigDecimal("50000.00"), "ACTIVE")
        );
        when(accountServiceClient.getAccount(eq(dst))).thenReturn(
                new AccountDto(UUID.randomUUID(), dst, UUID.randomUUID(), "NGN", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "ACTIVE")
        );

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("25000.00"), "NGN", "Invoice payment");

        // First call
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-key-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

        // Second call with same idempotency key
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-key-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));
    }
}