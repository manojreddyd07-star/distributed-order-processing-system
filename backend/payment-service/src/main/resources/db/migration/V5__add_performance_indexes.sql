-- Add additional performance indexes for Payment Service

-- Partial index for pending/processing payments (actively monitored)
CREATE INDEX IF NOT EXISTS idx_payments_active_status ON payments(status, created_at DESC) 
WHERE status IN ('PENDING', 'PROCESSING', 'VALIDATING');

-- Index for payment history by order
CREATE INDEX IF NOT EXISTS idx_payments_order_created ON payments(order_id, created_at DESC);

-- Composite index for payment amount queries (reporting)
CREATE INDEX IF NOT EXISTS idx_payments_status_amount ON payments(status, amount DESC, created_at DESC);

-- Index for failed payment analysis
CREATE INDEX IF NOT EXISTS idx_payments_failed ON payments(status, failure_reason, created_at DESC) 
WHERE status = 'FAILED';

-- Index for idempotency checks (critical path)
CREATE INDEX IF NOT EXISTS idx_idempotency_event_processed ON idempotency_records(event_id, processed_at DESC);

-- Index for retry queue processing
CREATE INDEX IF NOT EXISTS idx_retry_next_retry ON retry_records(next_retry_time, retry_status) 
WHERE retry_status IN ('PENDING', 'SCHEDULED');

-- Add comments for documentation
COMMENT ON INDEX idx_payments_active_status IS 'Partial index for active payments monitoring';
COMMENT ON INDEX idx_payments_order_created IS 'Optimizes payment history lookups by order';
COMMENT ON INDEX idx_payments_status_amount IS 'Optimizes financial reporting queries';
COMMENT ON INDEX idx_payments_failed IS 'Partial index for failed payment analysis';
COMMENT ON INDEX idx_idempotency_event_processed IS 'Critical for duplicate payment prevention';
COMMENT ON INDEX idx_retry_next_retry IS 'Optimizes retry queue scheduling queries';
