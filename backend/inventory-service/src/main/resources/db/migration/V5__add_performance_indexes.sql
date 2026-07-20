-- Add additional performance indexes for Inventory Service

-- Partial index for low stock items (frequently monitored)
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock ON inventory(product_id, quantity, status) 
WHERE status IN ('LOW_STOCK', 'OUT_OF_STOCK');

-- Index for product availability queries
CREATE INDEX IF NOT EXISTS idx_inventory_available ON inventory(status, quantity DESC) 
WHERE status = 'IN_STOCK' AND quantity > 0;

-- Composite index for reservation queries
CREATE INDEX IF NOT EXISTS idx_inventory_product_updated ON inventory(product_id, updated_at DESC);

-- Index for idempotency checks (most critical for performance)
CREATE INDEX IF NOT EXISTS idx_idempotency_event_processed ON idempotency_records(event_id, processed_at DESC);

-- Index for retry monitoring queries
CREATE INDEX IF NOT EXISTS idx_retry_status_attempts ON retry_records(retry_status, attempt_count, next_retry_time);

-- Add comments for documentation
COMMENT ON INDEX idx_inventory_low_stock IS 'Partial index for low stock monitoring - improves alert query performance';
COMMENT ON INDEX idx_inventory_available IS 'Partial index for available inventory queries';
COMMENT ON INDEX idx_inventory_product_updated IS 'Optimizes product update history queries';
COMMENT ON INDEX idx_idempotency_event_processed IS 'Critical for fast duplicate event detection';
COMMENT ON INDEX idx_retry_status_attempts IS 'Optimizes retry queue processing queries';
