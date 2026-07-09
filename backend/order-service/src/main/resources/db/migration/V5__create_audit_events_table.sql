-- Create audit_events table for tracking all system events
CREATE TABLE IF NOT EXISTS audit_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(100) NOT NULL,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for efficient querying
CREATE INDEX IF NOT EXISTS idx_event_id ON audit_events(event_id);
CREATE INDEX IF NOT EXISTS idx_order_id ON audit_events(order_id);
CREATE INDEX IF NOT EXISTS idx_event_type ON audit_events(event_type);
CREATE INDEX IF NOT EXISTS idx_service_name ON audit_events(service_name);
CREATE INDEX IF NOT EXISTS idx_created_at ON audit_events(created_at DESC);

-- Add comments for documentation
COMMENT ON TABLE audit_events IS 'Audit trail for all system events across services';
COMMENT ON COLUMN audit_events.event_id IS 'Unique identifier for the event';
COMMENT ON COLUMN audit_events.event_type IS 'Type of event (e.g., ORDER_CREATED, PAYMENT_SUCCESS)';
COMMENT ON COLUMN audit_events.service_name IS 'Name of the service that generated the event';
COMMENT ON COLUMN audit_events.order_id IS 'Associated order ID';
COMMENT ON COLUMN audit_events.status IS 'Event status (e.g., SUCCESS, FAILED, PENDING)';
COMMENT ON COLUMN audit_events.message IS 'Additional event details or error messages';
COMMENT ON COLUMN audit_events.created_at IS 'Timestamp when the audit event was created';
