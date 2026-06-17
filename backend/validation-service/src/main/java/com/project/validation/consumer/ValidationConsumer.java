package com.project.validation.consumer;

import com.project.validation.event.OrderCreatedEvent;
import com.project.validation.service.ValidationService;
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
    
    @Autowired
    public ValidationConsumer(ValidationService validationService) {
        this.validationService = validationService;
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
            // Validate the order
            validationService.validateOrder(event);
            
            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged OrderCreatedEvent for order ID: {}", 
                       event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Error processing OrderCreatedEvent for order ID: {}. Error: {}", 
                        event.getOrderId(), e.getMessage(), e);
            // In production, you might want to implement retry logic or send to DLQ
            // For now, we'll acknowledge to prevent blocking the consumer
            acknowledgment.acknowledge();
        }
    }
}
