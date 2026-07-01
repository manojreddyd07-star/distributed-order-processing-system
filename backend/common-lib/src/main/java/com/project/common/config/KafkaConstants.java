package com.project.common.config;

/**
 * Kafka topic names and constants used across all services
 */
public final class KafkaConstants {
    
    // Private constructor to prevent instantiation
    private KafkaConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    // Topic names
    public static final String ORDER_CREATED_TOPIC = "order-created";
    public static final String ORDER_VALIDATED_TOPIC = "order-validated";
    public static final String ORDER_VALIDATION_FAILED_TOPIC = "order-validation-failed";
    public static final String PAYMENT_COMPLETED_TOPIC = "payment-completed-events";
    public static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    public static final String INVENTORY_REJECTED_TOPIC = "inventory-rejected";
    public static final String INVENTORY_RESERVED_EVENTS_TOPIC = "inventory-reserved-events";
    public static final String ORDER_COMPLETED_TOPIC = "order-completed";
    
    // Consumer group IDs
    public static final String VALIDATION_SERVICE_GROUP = "validation-service-group";
    public static final String PAYMENT_SERVICE_GROUP = "payment-service-group";
    public static final String INVENTORY_SERVICE_GROUP = "inventory-service-group";
    public static final String FULFILLMENT_SERVICE_GROUP = "fulfillment-service-group";
    public static final String ORDER_SERVICE_GROUP = "order-service-group";
    
    // Event types
    public static final String ORDER_CREATED_EVENT_TYPE = "OrderCreated";
    public static final String ORDER_VALIDATED_EVENT_TYPE = "OrderValidated";
    public static final String ORDER_VALIDATION_FAILED_EVENT_TYPE = "OrderValidationFailed";
    public static final String PAYMENT_COMPLETED_EVENT_TYPE = "PaymentCompleted";
    public static final String INVENTORY_RESERVED_EVENT_TYPE = "InventoryReserved";
    public static final String INVENTORY_REJECTED_EVENT_TYPE = "InventoryRejected";
    public static final String ORDER_COMPLETED_EVENT_TYPE = "OrderCompleted";
}
