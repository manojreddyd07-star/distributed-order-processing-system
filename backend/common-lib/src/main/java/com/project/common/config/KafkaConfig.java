package com.project.common.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Common Kafka configuration utility for producer and consumer settings
 */
public final class KafkaConfig {
    
    // Private constructor to prevent instantiation
    private KafkaConfig() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Get common Kafka producer configuration properties
     * @param bootstrapServers Kafka bootstrap servers
     * @return Map of producer configuration properties
     */
    public static Map<String, Object> getProducerConfig(String bootstrapServers) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return configProps;
    }
    
    /**
     * Get common Kafka consumer configuration properties
     * @param bootstrapServers Kafka bootstrap servers
     * @param groupId Consumer group ID
     * @return Map of consumer configuration properties
     */
    public static Map<String, Object> getConsumerConfig(String bootstrapServers, String groupId) {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.project.*");
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return configProps;
    }
}
