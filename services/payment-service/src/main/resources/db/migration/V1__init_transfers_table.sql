CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    transfer_reference VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    source_account_number VARCHAR(10) NOT NULL,
    destination_account_number VARCHAR(10) NOT NULL,
    amount NUMERIC(18, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    narration VARCHAR(255),
    failure_reason VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_transfers_reference UNIQUE (transfer_reference),
    CONSTRAINT uq_transfers_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT chk_transfers_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transfers_source_account ON transfers(source_account_number);
CREATE INDEX idx_transfers_destination_account ON transfers(destination_account_number);
CREATE INDEX idx_transfers_status ON transfers(status);