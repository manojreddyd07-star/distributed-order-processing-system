package com.project.validation.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ValidationEventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationEventProducer.class);
    
    private static final String VALIDATION_SUCCESS_TOPIC = "order-validated";
    private static final String VALIDATION_FAILED_TOPIC = "order-validation-failed";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    public ValidationEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publishes a validation success event to the validation-success topic
     * @param orderId The order ID that was validated
     * @param validationStatus The validation status (e.g., "VALID")
     * @param validationMessage The validation message
     */
    public void publishValidationSuccessEvent(Long orderId, String validationStatus, String validationMessage) {
        try {
            // Create event with metadata
            OrderValidatedEvent event = new OrderValidatedEvent(
                UUID.randomUUID().toString(),
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                orderId,
                validationStatus,
                validationMessage
            );
            
            logger.info("Publishing validation success event for order ID: {}", orderId);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(VALIDATION_SUCCESS_TOPIC, orderId.toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published validation success event for order ID: {} to topic: {} with offset: {}",
                               orderId, VALIDATION_SUCCESS_TOPIC, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish validation success event for order ID: {} to topic: {}. Error: {}",
                                orderId, VALIDATION_SUCCESS_TOPIC, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while publishing validation success event for order ID: {}. Error: {}",
                        orderId, e.getMessage(), e);
        }
    }
    
    /**
     * Publishes a validation failed event to the validation-failed topic
     * @param orderId The order ID that failed validation
     * @param validationStatus The validation status (e.g., "INVALID")
     * @param validationMessage The validation error message
     */
    public void publishValidationFailedEvent(Long orderId, String validationStatus, String validationMessage) {
        try {
            // Create event with metadata
            OrderValidationFailedEvent event = new OrderValidationFailedEvent(
                UUID.randomUUID().toString(),
                "ORDER_VALIDATION_FAILED",
                LocalDateTime.now(),
                orderId,
                validationStatus,
                validationMessage
            );
            
            logger.info("Publishing validation failed event for order ID: {}", orderId);
            
            // Send to Kafka
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(VALIDATION_FAILED_TOPIC, orderId.toString(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published validation failed event for order ID: {} to topic: {} with offset: {}",
                               orderId, VALIDATION_FAILED_TOPIC, result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish validation failed event for order ID: {} to topic: {}. Error: {}",
                                orderId, VALIDATION_FAILED_TOPIC, ex.getMessage(), ex);
                }
            });
            
        } catch (Exception e) {
            logger.error("Exception while publishing validation failed event for order ID: {}. Error: {}",
                        orderId, e.getMessage(), e);
        }
    }
}
