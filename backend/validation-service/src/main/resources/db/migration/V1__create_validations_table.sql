CREATE TABLE validations (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    validation_status VARCHAR(50) NOT NULL,
    validation_message VARCHAR(500),
    validated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_validations_order_id ON validations(order_id);
CREATE INDEX idx_validations_status ON validations(validation_status);
