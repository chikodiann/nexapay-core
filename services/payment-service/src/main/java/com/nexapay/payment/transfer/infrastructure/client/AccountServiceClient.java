package com.nexapay.payment.transfer.infrastructure.client;

import com.nexapay.payment.common.exception.AccountMutationRejectedException;
import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.AccountServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class AccountServiceClient {

    private final RestClient restClient;

    public AccountServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${services.account-service.base-url:http://localhost:8081}") String accountServiceUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(accountServiceUrl)
                .build();
    }

    public AccountDto getAccount(String accountNumber) {
        try {
            return restClient.get()
                    .uri("/api/v1/accounts/{accountNumber}", accountNumber)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        throw new AccountNotFoundException("Account " + accountNumber + " does not exist");
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new AccountServiceUnavailableException("Account service error: " + resp.getStatusCode());
                    })
                    .body(AccountDto.class);
        } catch (ResourceAccessException ex) {
            throw new AccountServiceUnavailableException("Account service unreachable: " + ex.getMessage());
        }
    }

    public void debit(String accountNumber, BigDecimal amount, String reference) {
        try {
            restClient.post()
                    .uri("/api/v1/accounts/{accountNumber}/debit", accountNumber)
                    .body(new BalanceMutationDto(amount, reference))
                    .retrieve()
                    .onStatus(status -> status.value() == 404 || status.value() == 422, (req, resp) -> {
                        throw new AccountMutationRejectedException("Debit rejected for account " + accountNumber + ": " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new AccountServiceUnavailableException("Debit call failed for account " + accountNumber + ": " + resp.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw new AccountServiceUnavailableException("Account service network failure during debit: " + ex.getMessage());
        }
    }

    public void credit(String accountNumber, BigDecimal amount, String reference) {
        try {
            restClient.post()
                    .uri("/api/v1/accounts/{accountNumber}/credit", accountNumber)
                    .body(new BalanceMutationDto(amount, reference))
                    .retrieve()
                    .onStatus(status -> status.value() == 404 || status.value() == 422, (req, resp) -> {
                        throw new AccountMutationRejectedException("Credit rejected for account " + accountNumber + ": " + resp.getStatusCode());
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new AccountServiceUnavailableException("Credit call failed for account " + accountNumber + ": " + resp.getStatusCode());
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException ex) {
            throw new AccountServiceUnavailableException("Account service network failure during credit: " + ex.getMessage());
        }
    }
}