package com.project.payment.consumer;

import com.project.common.events.OrderValidatedEvent;
import com.project.payment.service.PaymentService;
import com.project.payment.service.IdempotencyService;
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
public class PaymentConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentConsumer.class);
    
    private final PaymentService paymentService;
    private final IdempotencyService idempotencyService;
    
    @Autowired
    public PaymentConsumer(PaymentService paymentService, IdempotencyService idempotencyService) {
        this.paymentService = paymentService;
        this.idempotencyService = idempotencyService;
    }
    
    /**
     * Consumes OrderValidatedEvent from Kafka topic
     * Verifies Kafka event consumption and triggers payment processing
     */
    @KafkaListener(
        topics = "order-validated",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeOrderValidatedEvent(
            @Payload OrderValidatedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("========== Kafka Event Consumption Verification ==========");
        logger.info("Received OrderValidatedEvent from topic: {}, partition: {}, offset: {}", 
                   topic, partition, offset);
        logger.info("Event details: {}", event);
        
        try {
            // Verify event consumption
            if (event == null || event.getOrderId() == null) {
                logger.error("Invalid OrderValidatedEvent received - event is null or orderId is missing");
                acknowledgment.acknowledge();
                return;
            }
            
            // Check for duplicate events using idempotency service
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                logger.warn("Duplicate event detected - Event {} has already been processed. Skipping.", 
                           event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            logger.info("Event consumption verified successfully - Order ID: {}, Status: {}", 
                       event.getOrderId(), event.getValidationStatus());
            
            // Only process payment if validation was successful
            if ("VALID".equalsIgnoreCase(event.getValidationStatus())) {
                logger.info("Order validation successful - proceeding with payment processing");
                
                // Process payment
                paymentService.processPayment(event);
                
                logger.info("========== Payment Record Insertion Verification ==========");
                logger.info("Payment record successfully inserted for order ID: {}", event.getOrderId());
                
            } else {
                logger.warn("Order validation failed with status: {} - skipping payment processing", 
                           event.getValidationStatus());
                logger.warn("Validation message: {}", event.getValidationMessage());
            }
            
            // Mark event as processed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "PROCESSED");
            
            // Acknowledge message after successful processing
            acknowledgment.acknowledge();
            logger.info("Successfully processed and acknowledged OrderValidatedEvent for order ID: {}", 
                       event.getOrderId());
            logger.info("==========================================================");
            
        } catch (Exception e) {
            logger.error("========== Error Processing OrderValidatedEvent ==========");
            logger.error("Error processing OrderValidatedEvent for order ID: {}. Error: {}", 
                        event.getOrderId(), e.getMessage(), e);
            logger.error("==========================================================");
            
            // Mark event as failed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "FAILED");
            
            // In production, you might want to implement retry logic or send to DLQ
            // For now, we'll acknowledge to prevent blocking the consumer
            acknowledgment.acknowledge();
        }
    }
}
