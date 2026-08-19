CREATE TABLE consumed_messages (
    id UUID PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    event_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    message_key VARCHAR(100) NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_consumed_messages_consumer_event UNIQUE (consumer_name, event_id)
);

CREATE INDEX idx_consumed_messages_lookup 
    ON consumed_messages (consumer_name, event_id);