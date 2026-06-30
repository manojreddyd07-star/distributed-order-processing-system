CREATE TABLE fulfillment_audit_log (
    audit_id BIGSERIAL PRIMARY KEY,
    fulfillment_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    tracking_number VARCHAR(100) NOT NULL,
    fulfillment_status VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_fulfillment_id ON fulfillment_audit_log(fulfillment_id);
CREATE INDEX idx_audit_order_id ON fulfillment_audit_log(order_id);
CREATE INDEX idx_audit_customer_id ON fulfillment_audit_log(customer_id);
CREATE INDEX idx_audit_created_at ON fulfillment_audit_log(created_at DESC);
