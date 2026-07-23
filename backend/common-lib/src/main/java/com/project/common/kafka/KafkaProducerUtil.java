package com.project.common.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer utility wrapper for consistent message publishing across services
 */
public class KafkaProducerUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerUtil.class);
    
    private KafkaProducerUtil() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Send message to Kafka topic with logging
     */
    public static <K, V> void sendMessage(KafkaTemplate<K, V> kafkaTemplate, String topic, V message) {
        sendMessage(kafkaTemplate, topic, null, message);
    }
    
    /**
     * Send message to Kafka topic with key and logging
     */
    public static <K, V> void sendMessage(KafkaTemplate<K, V> kafkaTemplate, String topic, K key, V message) {
        try {
            logger.info("Sending message to Kafka topic: {}", topic);
            
            CompletableFuture<SendResult<K, V>> future = kafkaTemplate.send(topic, key, message);
            
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to send message to topic {}: {}", topic, ex.getMessage(), ex);
                } else {
                    logger.info("Successfully sent message to topic {}, partition: {}, offset: {}",
                            topic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
        } catch (Exception e) {
            logger.error("Error sending message to Kafka topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
    
    /**
     * Send message synchronously with logging
     */
    public static <K, V> SendResult<K, V> sendMessageSync(KafkaTemplate<K, V> kafkaTemplate, String topic, V message) {
        return sendMessageSync(kafkaTemplate, topic, null, message);
    }
    
    /**
     * Send message synchronously with key and logging
     */
    public static <K, V> SendResult<K, V> sendMessageSync(KafkaTemplate<K, V> kafkaTemplate, String topic, K key, V message) {
        try {
            logger.info("Sending message synchronously to Kafka topic: {}", topic);
            
            CompletableFuture<SendResult<K, V>> future = kafkaTemplate.send(topic, key, message);
            SendResult<K, V> result = future.get();
            
            logger.info("Successfully sent message to topic {}, partition: {}, offset: {}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            
            return result;
        } catch (Exception e) {
            logger.error("Error sending message synchronously to Kafka topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Kafka synchronously", e);
        }
    }
}
