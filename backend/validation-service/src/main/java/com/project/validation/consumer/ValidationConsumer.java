package com.project.validation.consumer;

import com.project.common.events.OrderCreatedEvent;
import com.project.validation.service.ValidationService;
import com.project.validation.service.IdempotencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ValidationConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationConsumer.class);
    
    private final ValidationService validationService;
    private final IdempotencyService idempotencyService;
    
    @Autowired
    public ValidationConsumer(ValidationService validationService, IdempotencyService idempotencyService) {
        this.validationService = validationService;
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Consumes OrderCreatedEvent from Kafka topic
     */
    @KafkaListener(
        topics = "order-created",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderCreatedEvent(
            @Payload OrderCreatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Received OrderCreatedEvent from topic: {}, partition: {}, offset: {}", 
                   topic, partition, offset);
        logger.info("Event details: {}", event);
        
        try {
            // Check for duplicate events using idempotency service
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                logger.warn("Duplicate event detected - Event {} has already been processed. Skipping.", 
                           event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Validate the order
            validationService.validateOrder(event);
            
            // Mark event as processed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "PROCESSED");
            
            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged OrderCreatedEvent for order ID: {}", 
                       event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Error processing OrderCreatedEvent for order ID: {}. Error: {}", 
                        event.getOrderId(), e.getMessage(), e);
            
            // Mark event as failed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "FAILED");
            
            // In production, you might want to implement retry logic or send to DLQ
            // For now, we'll acknowledge to prevent blocking the consumer
            acknowledgment.acknowledge();
        }
    }
}
