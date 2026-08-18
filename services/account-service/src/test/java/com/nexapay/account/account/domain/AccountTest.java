package com.nexapay.account.account.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    @DisplayName("Should create an account with zero balance and ACTIVE status")
    void shouldCreateAccountWithZeroBalance() {
        UUID customerId = UUID.randomUUID();
        AccountNumberGenerator generator = () -> "1023847291";

        Account account = Account.create(customerId, Currency.NGN, generator);

        assertThat(account.getId()).isNotNull();
        assertThat(account.getAccountNumber()).isEqualTo("1023847291");
        assertThat(account.getCustomerId()).isEqualTo(customerId);
        assertThat(account.getCurrency()).isEqualTo(Currency.NGN);
        assertThat(account.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getLedgerBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should throw exception when attempting to alter status of CLOSED account")
    void shouldPreventStatusChangeWhenClosed() {
        Account account = Account.create(UUID.randomUUID(), Currency.NGN, () -> "1023847291");
        account.updateStatus(AccountStatus.CLOSED);

        assertThatThrownBy(() -> account.updateStatus(AccountStatus.ACTIVE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot change status of a CLOSED account");
    }
}