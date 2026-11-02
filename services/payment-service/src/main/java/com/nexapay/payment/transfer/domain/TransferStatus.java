package com.nexapay.payment.transfer.domain;

public enum TransferStatus {
    PENDING,
    PROCESSING,
    DEBIT_COMPLETED,
    CREDIT_PENDING,
    CREDIT_COMPLETED,
    UNKNOWN_TIMEOUT,
    RECONCILIATION_REQUIRED,
    SUCCESSFUL,
    FAILED,
    REVERSED
}