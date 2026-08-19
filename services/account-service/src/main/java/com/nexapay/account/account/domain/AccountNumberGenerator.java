package com.nexapay.account.account.domain;

@FunctionalInterface
public interface AccountNumberGenerator {
    String generate();
}