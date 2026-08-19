package com.nexapay.payment.transfer.infrastructure.client;

import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.ServiceUnavailableException;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

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
        return restClient.get()
                .uri("/api/v1/accounts/{accountNumber}", accountNumber)
                .retrieve()
                .onStatus(status -> status.value() == 404, (req, resp) -> {
                    throw new AccountNotFoundException("Account " + accountNumber + " does not exist");
                })
                .onStatus(HttpStatusCode::isError, (req, resp) -> {
                    throw new ServiceUnavailableException("Failed to reach account-service: " + resp.getStatusCode());
                })
                .body(AccountDto.class);
    }

    public void debit(String accountNumber, BigDecimal amount, String reference) {
    restClient.post()
            .uri("/api/v1/accounts/{accountNumber}/debit", accountNumber)
            .body(new BalanceMutationDto(amount, reference))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, resp) -> {
                throw new ServiceUnavailableException("Debit failed on account " + accountNumber + ": " + resp.getStatusCode());
            })
            .toBodilessEntity();
}

public void credit(String accountNumber, BigDecimal amount, String reference) {
    restClient.post()
            .uri("/api/v1/accounts/{accountNumber}/credit", accountNumber)
            .body(new BalanceMutationDto(amount, reference))
            .retrieve()
            .onStatus(HttpStatusCode::isError, (req, resp) -> {
                throw new ServiceUnavailableException("Credit failed on account " + accountNumber + ": " + resp.getStatusCode());
            })
            .toBodilessEntity();
}
}