-- Add composite indexes for common query patterns in Fulfillment Service

-- Composite index for order fulfillments by status
CREATE INDEX IF NOT EXISTS idx_fulfillments_order_status ON fulfillments(order_id, fulfillment_status);

-- Composite index for fulfillment reporting
CREATE INDEX IF NOT EXISTS idx_fulfillments_status_created ON fulfillments(fulfillment_status, created_at DESC);

-- Composite index for audit log queries
CREATE INDEX IF NOT EXISTS idx_fulfillment_audit_order_created ON fulfillment_audit_log(fulfillment_id, created_at DESC);

-- Composite index for idempotency lookups
CREATE INDEX IF NOT EXISTS idx_idempotency_event_status ON idempotency_records(event_id, processing_status);

-- Composite index for retry records by status and next retry time
CREATE INDEX IF NOT EXISTS idx_retry_status_next_time ON retry_records(retry_status, next_retry_time);

-- Composite index for failed event tracking
CREATE INDEX IF NOT EXISTS idx_retry_event_count ON retry_records(original_event_id, retry_count);

-- Add comments
COMMENT ON INDEX idx_fulfillments_order_status IS 'Optimizes fulfillment lookup by order and status';
COMMENT ON INDEX idx_fulfillments_status_created IS 'Optimizes fulfillment reporting queries';
COMMENT ON INDEX idx_retry_status_next_time IS 'Optimizes scheduled retry processing';
