CREATE TABLE fulfillments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    fulfillment_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fulfillments_order_id ON fulfillments(order_id);
CREATE INDEX idx_fulfillments_status ON fulfillments(fulfillment_status);
CREATE INDEX idx_fulfillments_created_at ON fulfillments(created_at);
