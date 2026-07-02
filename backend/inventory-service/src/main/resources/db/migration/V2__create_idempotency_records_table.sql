CREATE TABLE idempotency_records (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    processing_status VARCHAR(50) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_event_id ON idempotency_records(event_id);
CREATE INDEX idx_service_name ON idempotency_records(service_name);
CREATE INDEX idx_processing_status ON idempotency_records(processing_status);
