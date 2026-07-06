package com.project.inventory.consumer;

import com.project.common.events.PaymentCompletedEvent;
import com.project.inventory.service.InventoryService;
import com.project.inventory.service.IdempotencyService;
import com.project.inventory.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class InventoryConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryConsumer.class);
    
    private final InventoryService inventoryService;
    private final IdempotencyService idempotencyService;
    private final RetryService retryService;
    
    @Autowired
    public InventoryConsumer(InventoryService inventoryService, 
                            IdempotencyService idempotencyService,
                            RetryService retryService) {
        this.inventoryService = inventoryService;
        this.idempotencyService = idempotencyService;
        this.retryService = retryService;
    }
    
    /**
     * Consume PaymentCompletedEvent from Kafka
     * @param event The PaymentCompletedEvent
     * @param acknowledgment Kafka acknowledgment
     */
    @KafkaListener(topics = "payment-completed-events", groupId = "inventory-service-group")
    public void consumePaymentCompletedEvent(PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        logger.info("========================================");
        logger.info("Received PaymentCompletedEvent");
        logger.info("Event ID: {}", event.getEventId());
        logger.info("Order ID: {}", event.getOrderId());
        logger.info("Payment ID: {}", event.getPaymentId());
        logger.info("Product ID: {}", event.getProductId());
        logger.info("Product Name: {}", event.getProductName());
        logger.info("Quantity: {}", event.getQuantity());
        logger.info("========================================");
        
        try {
            // Check for duplicate events using idempotency service
            if (idempotencyService.isEventProcessed(event.getEventId())) {
                logger.warn("Duplicate event detected - Event {} has already been processed. Skipping.", 
                           event.getEventId());
                acknowledgment.acknowledge();
                return;
            }
            
            // Verify inventory availability
            boolean inventoryAvailable = inventoryService.verifyInventory(
                event.getProductId(), 
                event.getQuantity()
            );
            
            if (inventoryAvailable) {
                logger.info("Inventory verification successful for product: {}", event.getProductId());
                
                // Reserve inventory
                inventoryService.reserveInventory(
                    event.getProductId(),
                    event.getProductName(),
                    event.getQuantity(),
                    event.getOrderId()
                );
                
                logger.info("Inventory reservation completed for order ID: {}", event.getOrderId());
            } else {
                logger.warn("Inventory verification failed for product: {} - Insufficient stock", 
                           event.getProductId());
            }
            
            // Mark event as processed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "PROCESSED");
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.info("PaymentCompletedEvent processed and acknowledged successfully");
            
        } catch (Exception e) {
            logger.error("Error processing PaymentCompletedEvent for order ID: {}", 
                        event.getOrderId(), e);
            
            // Mark event as failed
            idempotencyService.markEventAsProcessed(event.getEventId(), event.getEventType(), "FAILED");
            
            // Forward failed event to retry topic
            logger.info("Forwarding failed event to retry topic: {}", event.getEventId());
            retryService.createRetryEvent(event, event.getEventType(), event.getEventId(), 
                                         "payment-completed-events", e.getMessage());
            
            // Acknowledge to prevent blocking the consumer
            acknowledgment.acknowledge();
        }
    }
}
