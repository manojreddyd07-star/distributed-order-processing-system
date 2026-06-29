CREATE TABLE fulfillment (
    fulfillment_id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    customer_id VARCHAR(100) NOT NULL,
    fulfillment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    tracking_number VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fulfillment_order_id ON fulfillment(order_id);
CREATE INDEX idx_fulfillment_customer_id ON fulfillment(customer_id);
CREATE INDEX idx_fulfillment_status ON fulfillment(fulfillment_status);
CREATE INDEX idx_fulfillment_tracking_number ON fulfillment(tracking_number);
CREATE INDEX idx_fulfillment_created_at ON fulfillment(created_at);
