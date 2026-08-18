package com.nexapay.payment.transfer.infrastructure.client;

import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.ServiceUnavailableException;
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
}