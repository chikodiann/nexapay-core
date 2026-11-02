package com.nexapay.payment.transfer.infrastructure.client;

import com.nexapay.payment.common.exception.AccountMutationRejectedException;
import com.nexapay.payment.common.exception.AccountNotFoundException;
import com.nexapay.payment.common.exception.AccountServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
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

    public Optional<LedgerMutationDto> getMutationStatus(String reference) {
        try {
            LedgerMutationDto dto = restClient.get()
                    .uri("/api/v1/accounts/mutations/{reference}", reference)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        throw new HttpClientErrorException(org.springframework.http.HttpStatus.NOT_FOUND, "Mutation not found");
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        throw new AccountServiceUnavailableException("Mutation lookup error: " + resp.getStatusCode());
                    })
                    .body(LedgerMutationDto.class);
            return Optional.ofNullable(dto);
        } catch (HttpClientErrorException.NotFound ex) {
            return Optional.empty();
        } catch (AccountServiceUnavailableException | ResourceAccessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AccountServiceUnavailableException("Mutation lookup failed: " + ex.getMessage());
        }
    }
}