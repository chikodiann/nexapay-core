package com.nexapay.payment.transfer.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.AccountServiceUnavailableException;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    private final String src = "1023847291";
    private final String dst = "1045678932";

    @BeforeEach
    void setUp() {
        transferRepository.deleteAll();
    }

    private void mockActiveAccounts() {
        when(accountServiceClient.getAccount(eq(src))).thenReturn(
                new AccountDto(UUID.randomUUID(), src, UUID.randomUUID(), "NGN", new BigDecimal("50000.00"), new BigDecimal("50000.00"), "ACTIVE")
        );
        when(accountServiceClient.getAccount(eq(dst))).thenReturn(
                new AccountDto(UUID.randomUUID(), dst, UUID.randomUUID(), "NGN", new BigDecimal("1000.00"), new BigDecimal("1000.00"), "ACTIVE")
        );
    }

    @Test
    @DisplayName("1. shouldCompleteTransferWhenDebitAndCreditSucceed")
    void shouldCompleteTransferWhenDebitAndCreditSucceed() throws Exception {
        mockActiveAccounts();
        doNothing().when(accountServiceClient).debit(anyString(), any(), anyString());
        doNothing().when(accountServiceClient).credit(anyString(), any(), anyString());

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("25000.00"), "NGN", "Normal transfer");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"))
                .andExpect(jsonPath("$.transferReference").isNotEmpty());

        verify(accountServiceClient, times(1)).debit(eq(src), any(), anyString());
        verify(accountServiceClient, times(1)).credit(eq(dst), any(), anyString());
    }

    @Test
    @DisplayName("2. shouldMarkTransferFailedWhenSourceDebitFails")
    void shouldMarkTransferFailedWhenSourceDebitFails() throws Exception {
        mockActiveAccounts();
        doThrow(new AccountServiceUnavailableException("Connection timeout on debit"))
                .when(accountServiceClient).debit(eq(src), any(), anyString());

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("10000.00"), "NGN", "Debit failure");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("SOURCE_DEBIT_FAILED: Connection timeout on debit"));

        verify(accountServiceClient, times(1)).debit(eq(src), any(), anyString());
        verify(accountServiceClient, times(0)).credit(anyString(), any(), anyString());
    }

    @Test
    @DisplayName("3. shouldReverseSourceDebitWhenDestinationCreditFails")
    void shouldReverseSourceDebitWhenDestinationCreditFails() throws Exception {
        mockActiveAccounts();
        doNothing().when(accountServiceClient).debit(eq(src), any(), anyString());
        doThrow(new AccountServiceUnavailableException("Destination ledger 500 error"))
                .when(accountServiceClient).credit(eq(dst), any(), anyString());
        doNothing().when(accountServiceClient).credit(eq(src), any(), org.mockito.ArgumentMatchers.contains(":REVERSAL"));

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("15000.00"), "NGN", "Partial failure");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-003")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REVERSED"))
                .andExpect(jsonPath("$.failureReason").value("DESTINATION_CREDIT_FAILED: Destination ledger 500 error"));

        verify(accountServiceClient, times(1)).debit(eq(src), any(), anyString());
        verify(accountServiceClient, times(1)).credit(eq(dst), any(), anyString());
        verify(accountServiceClient, times(1)).credit(eq(src), any(), org.mockito.ArgumentMatchers.contains(":REVERSAL"));
    }

    @Test
    @DisplayName("4. shouldNotDuplicateDebitWhenTransferRequestIsRetried")
    void shouldNotDuplicateDebitWhenTransferRequestIsRetried() throws Exception {
        mockActiveAccounts();
        doNothing().when(accountServiceClient).debit(anyString(), any(), anyString());
        doNothing().when(accountServiceClient).credit(anyString(), any(), anyString());

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("5000.00"), "NGN", "Retry test");

        // First attempt
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

        // Second attempt
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-004")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESSFUL"));

        verify(accountServiceClient, times(1)).debit(eq(src), any(), anyString());
        verify(accountServiceClient, times(1)).credit(eq(dst), any(), anyString());
    }

    @Test
    @DisplayName("5. shouldNotDuplicateCompensationWhenRecoveryRunsTwice")
    void shouldNotDuplicateCompensationWhenRecoveryRunsTwice() throws Exception {
        mockActiveAccounts();
        doNothing().when(accountServiceClient).debit(eq(src), any(), anyString());
        doThrow(new AccountServiceUnavailableException("Destination down"))
                .when(accountServiceClient).credit(eq(dst), any(), anyString());
        doNothing().when(accountServiceClient).credit(eq(src), any(), org.mockito.ArgumentMatchers.contains(":REVERSAL"));

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("8000.00"), "NGN", "Double compensation test");

        // Initial call resulting in REVERSED
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        // Subsequent call with same idempotency key
        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-005")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        verify(accountServiceClient, times(1)).credit(eq(src), any(), org.mockito.ArgumentMatchers.contains(":REVERSAL"));
    }

    @Test
    @DisplayName("6. shouldNotMarkTransferSuccessfulUnlessBothFinancialStepsComplete")
    void shouldNotMarkTransferSuccessfulUnlessBothFinancialStepsComplete() throws Exception {
        mockActiveAccounts();
        doNothing().when(accountServiceClient).debit(eq(src), any(), anyString());
        doThrow(new AccountServiceUnavailableException("Downstream network timeout on credit"))
                .when(accountServiceClient).credit(eq(dst), any(), anyString());
        doNothing().when(accountServiceClient).credit(eq(src), any(), org.mockito.ArgumentMatchers.contains(":REVERSAL"));

        InitiateTransferRequest request = new InitiateTransferRequest(src, dst, new BigDecimal("12000.00"), "NGN", "Incomplete steps test");

        mockMvc.perform(post("/api/v1/transfers")
                        .header("Idempotency-Key", "idemp-006")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REVERSED"));

        var transfer = transferRepository.findByIdempotencyKey("idemp-006").orElseThrow();
        org.assertj.core.api.Assertions.assertThat(transfer.isSourceDebited()).isTrue();
        org.assertj.core.api.Assertions.assertThat(transfer.isDestinationCredited()).isFalse();
        org.assertj.core.api.Assertions.assertThat(transfer.isCompensationCompleted()).isTrue();
    }
}