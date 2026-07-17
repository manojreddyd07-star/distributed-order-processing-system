-- Add composite indexes for common query patterns in Inventory Service

-- Composite index for product inventory by status
CREATE INDEX IF NOT EXISTS idx_inventory_product_status ON inventory(product_id, status);

-- Composite index for idempotency lookups
CREATE INDEX IF NOT EXISTS idx_idempotency_event_status ON idempotency_records(event_id, processing_status);

-- Composite index for retry records by status and next retry time
CREATE INDEX IF NOT EXISTS idx_retry_status_next_time ON retry_records(retry_status, next_retry_time);

-- Composite index for failed event tracking
CREATE INDEX IF NOT EXISTS idx_retry_event_count ON retry_records(original_event_id, retry_count);

-- Add comments
COMMENT ON INDEX idx_inventory_product_status IS 'Optimizes inventory lookup by product and status';
COMMENT ON INDEX idx_retry_status_next_time IS 'Optimizes scheduled retry processing';
