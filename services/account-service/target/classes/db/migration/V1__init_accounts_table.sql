CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(10) NOT NULL,
    customer_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    available_balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    ledger_balance NUMERIC(18, 4) NOT NULL DEFAULT 0.0000,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
    CONSTRAINT uq_accounts_customer_currency UNIQUE (customer_id, currency),
    CONSTRAINT chk_accounts_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_accounts_ledger_balance_non_negative CHECK (ledger_balance >= 0)
);

CREATE INDEX idx_accounts_customer_id ON accounts(customer_id);
CREATE INDEX idx_accounts_account_number ON accounts(account_number);