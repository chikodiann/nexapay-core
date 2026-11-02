CREATE TABLE ledger_mutations (
    id UUID PRIMARY KEY,
    transfer_reference VARCHAR(64) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    account_id UUID NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ledger_mutations_account FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE UNIQUE INDEX uq_ledger_mutations_transfer_ref ON ledger_mutations(transfer_reference);
CREATE INDEX idx_ledger_mutations_account_id ON ledger_mutations(account_id);
