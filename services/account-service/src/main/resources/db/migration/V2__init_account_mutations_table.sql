CREATE TABLE account_mutations (
    id UUID PRIMARY KEY,
    account_number VARCHAR(10) NOT NULL,
    reference VARCHAR(64) NOT NULL,
    mutation_type VARCHAR(10) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_account_mutations_acc_ref_type UNIQUE (account_number, reference, mutation_type),
    CONSTRAINT fk_account_mutations_account FOREIGN KEY (account_number) REFERENCES accounts(account_number)
);

CREATE INDEX idx_account_mutations_lookup ON account_mutations(account_number, reference, mutation_type);