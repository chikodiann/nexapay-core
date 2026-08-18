package com.nexapay.account.account.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexapay.account.account.api.dto.CreateAccountRequest;
import com.nexapay.account.account.api.dto.UpdateAccountStatusRequest;
import com.nexapay.account.account.domain.AccountStatus;
import com.nexapay.account.account.domain.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Bypass security filters for API contract integration tests
@ActiveProfiles("test")
class AccountControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/accounts - Should create account successfully")
    void shouldCreateAccountSuccessfully() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(customerId, Currency.NGN);

        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.accountNumber").isNotEmpty())
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.currency").value("NGN"))
                .andExpect(jsonPath("$.availableBalance").value(0.00))
                .andExpect(jsonPath("$.ledgerBalance").value(0.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("POST /api/v1/accounts - Should reject duplicate account for customer and currency")
    void shouldRejectDuplicateAccountForCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(customerId, Currency.NGN);

        // First creation succeeds
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate creation returns 409 Conflict ProblemDetail
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Account Conflict"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{accountNumber} - Should return 404 for unknown account")
    void shouldReturnNotFoundForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/9999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Account Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/v1/accounts/{accountNumber}/status - Should update status to FROZEN")
    void shouldUpdateAccountStatus() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateAccountRequest request = new CreateAccountRequest(customerId, Currency.NGN);

        String createResponse = mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String accountNumber = objectMapper.readTree(createResponse).get("accountNumber").asText();

        UpdateAccountStatusRequest updateRequest = new UpdateAccountStatusRequest(AccountStatus.FROZEN);

        mockMvc.perform(patch("/api/v1/accounts/" + accountNumber + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }
}