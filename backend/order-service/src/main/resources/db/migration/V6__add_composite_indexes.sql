-- Add composite indexes for common query patterns in Order Service

-- Composite index for filtering by status and date (common dashboard queries)
CREATE INDEX IF NOT EXISTS idx_orders_status_created_at ON orders(order_status, created_at DESC);

-- Composite index for customer orders by status
CREATE INDEX IF NOT EXISTS idx_orders_customer_status ON orders(customer_id, order_status);

-- Add composite indexes for audit events table
CREATE INDEX IF NOT EXISTS idx_audit_events_order_service ON audit_events(order_id, service_name, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_type_status ON audit_events(event_type, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_events_service_status ON audit_events(service_name, status);

-- Add comment for documentation
COMMENT ON INDEX idx_orders_status_created_at IS 'Optimizes queries filtering by status and sorting by creation date';
COMMENT ON INDEX idx_orders_customer_status IS 'Optimizes customer order lookups filtered by status';
COMMENT ON INDEX idx_audit_events_order_service IS 'Optimizes audit trail queries by order and service';
COMMENT ON INDEX idx_audit_events_type_status IS 'Optimizes event type and status filtering';
