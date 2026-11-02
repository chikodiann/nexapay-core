package com.nexapay.account.common.exception;

public class LedgerMutationNotFoundException extends RuntimeException {
    public LedgerMutationNotFoundException(String message) {
        super(message);
    }
}
