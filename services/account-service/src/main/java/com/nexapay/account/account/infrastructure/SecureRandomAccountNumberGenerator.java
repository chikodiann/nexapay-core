package com.nexapay.account.account.infrastructure;

import com.nexapay.account.account.domain.AccountNumberGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class SecureRandomAccountNumberGenerator implements AccountNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate() {
        // Generates a 10-digit NUBAN-compliant length string (range 1000000000 to 9999999999)
        long number = 1_000_000_000L + (long) (RANDOM.nextDouble() * 9_000_000_000L);
        return String.valueOf(number);
    }
}