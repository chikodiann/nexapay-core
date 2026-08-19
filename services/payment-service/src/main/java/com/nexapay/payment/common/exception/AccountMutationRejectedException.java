package com.nexapay.payment.common.exception;

public class AccountMutationRejectedException extends RuntimeException {
    public AccountMutationRejectedException(String message) {
        super(message);
    }
}