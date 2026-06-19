-- Create payments table
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL UNIQUE,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on order_id for faster lookups
CREATE INDEX idx_payments_order_id ON payments(order_id);

-- Create index on status for filtering
CREATE INDEX idx_payments_status ON payments(status);

-- Create index on created_at for sorting
CREATE INDEX idx_payments_created_at ON payments(created_at);
