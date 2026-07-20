-- Add additional performance indexes for Fulfillment Service

-- Partial index for active fulfillments
CREATE INDEX IF NOT EXISTS idx_fulfillments_active ON fulfillments(fulfillment_status, created_at DESC) 
WHERE fulfillment_status IN ('PENDING', 'IN_PROGRESS', 'PACKAGING');

-- Index for fulfillment by order
CREATE INDEX IF NOT EXISTS idx_fulfillments_order_status ON fulfillments(order_id, fulfillment_status, updated_at DESC);

-- Composite index for fulfillment tracking
CREATE INDEX IF NOT EXISTS idx_fulfillments_tracking ON fulfillments(tracking_number, fulfillment_status) 
WHERE tracking_number IS NOT NULL;

-- Index for fulfillment audit trail
CREATE INDEX IF NOT EXISTS idx_fulfillment_audit_created ON fulfillment_audit_log(fulfillment_id, created_at DESC, action_type);

-- Index for idempotency checks
CREATE INDEX IF NOT EXISTS idx_idempotency_event_processed ON idempotency_records(event_id, processed_at DESC);

-- Index for retry processing
CREATE INDEX IF NOT EXISTS idx_retry_scheduled ON retry_records(next_retry_time, retry_status) 
WHERE retry_status = 'SCHEDULED' AND next_retry_time IS NOT NULL;

-- Add comments for documentation
COMMENT ON INDEX idx_fulfillments_active IS 'Partial index for active fulfillment monitoring';
COMMENT ON INDEX idx_fulfillments_order_status IS 'Optimizes order fulfillment status queries';
COMMENT ON INDEX idx_fulfillments_tracking IS 'Optimizes tracking number lookups';
COMMENT ON INDEX idx_fulfillment_audit_created IS 'Optimizes audit trail queries';
COMMENT ON INDEX idx_idempotency_event_processed IS 'Critical for duplicate fulfillment prevention';
COMMENT ON INDEX idx_retry_scheduled IS 'Optimizes scheduled retry processing';
