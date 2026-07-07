package com.project.common.config;

/**
 * Configuration constants for Dead Letter Queue (DLQ) topic
 */
public final class DlqTopicConfig {
    
    // Topic name
    public static final String DLQ_TOPIC = "dead-letter-orders";
    
    // Consumer group
    public static final String DLQ_CONSUMER_GROUP = "dlq-consumer-group";
    
    // Private constructor to prevent instantiation
    private DlqTopicConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
