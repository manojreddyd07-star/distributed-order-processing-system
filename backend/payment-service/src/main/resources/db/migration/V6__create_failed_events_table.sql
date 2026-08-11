CREATE TABLE failed_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    error_message TEXT,
    payload TEXT NOT NULL,
    original_topic VARCHAR(255),
    retry_count INTEGER,
    status VARCHAR(50),
    failed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_failed_event_type ON failed_events(event_type);
CREATE INDEX idx_failed_service_name ON failed_events(service_name);
CREATE INDEX idx_failed_at ON failed_events(failed_at DESC);
