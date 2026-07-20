-- Add additional performance indexes for Order Service

-- Partial index for active/pending orders (most frequently queried)
CREATE INDEX IF NOT EXISTS idx_orders_active_status ON orders(order_status, created_at DESC) 
WHERE order_status IN ('PENDING', 'VALIDATING', 'PROCESSING', 'PAYMENT_PENDING');

-- Index for recent order queries (covering last 30 days is most common)
CREATE INDEX IF NOT EXISTS idx_orders_recent_created ON orders(created_at DESC) 
WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '30 days';

-- Composite index for audit event queries by time range
CREATE INDEX IF NOT EXISTS idx_audit_events_created_status ON audit_events(created_at DESC, status);

-- Index to speed up count queries by status
CREATE INDEX IF NOT EXISTS idx_audit_events_status_count ON audit_events(status, event_type);

-- Add comments for documentation
COMMENT ON INDEX idx_orders_active_status IS 'Partial index for active orders - reduces index size and improves query performance';
COMMENT ON INDEX idx_orders_recent_created IS 'Partial index for recent orders within last 30 days';
COMMENT ON INDEX idx_audit_events_created_status IS 'Optimizes time-based audit queries with status filtering';
COMMENT ON INDEX idx_audit_events_status_count IS 'Optimizes count and aggregation queries by status';
