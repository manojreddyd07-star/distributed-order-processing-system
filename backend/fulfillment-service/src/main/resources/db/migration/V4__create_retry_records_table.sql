-- Create retry_records table for tracking retry attempts
CREATE TABLE retry_records (
    id BIGSERIAL PRIMARY KEY,
    retry_id VARCHAR(255) NOT NULL UNIQUE,
    original_event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    retry_status VARCHAR(50) NOT NULL,
    last_retry_time TIMESTAMP NOT NULL,
    next_retry_time TIMESTAMP,
    failure_reason TEXT,
    event_payload TEXT NOT NULL,
    event_class VARCHAR(255) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    target_topic VARCHAR(100) NOT NULL,
    max_retries INTEGER NOT NULL DEFAULT 3,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on retry_id for faster lookups
CREATE INDEX idx_retry_id ON retry_records(retry_id);

-- Create index on original_event_id for tracking retries by event
CREATE INDEX idx_original_event_id ON retry_records(original_event_id);

-- Create index on retry_status for filtering by status
CREATE INDEX idx_retry_status ON retry_records(retry_status);

-- Create index on next_retry_time for scheduled retry processing
CREATE INDEX idx_next_retry_time ON retry_records(next_retry_time);
