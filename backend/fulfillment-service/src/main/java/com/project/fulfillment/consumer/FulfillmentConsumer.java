package com.project.fulfillment.consumer;

import com.project.fulfillment.event.InventoryReservedEvent;
import com.project.fulfillment.service.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class FulfillmentConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(FulfillmentConsumer.class);
    
    private final FulfillmentService fulfillmentService;
    
    @Autowired
    public FulfillmentConsumer(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }
    
    /**
     * Consume InventoryReservedEvent from Kafka
     * @param event The InventoryReservedEvent
     * @param acknowledgment Kafka acknowledgment
     */
    @KafkaListener(topics = "inventory-reserved-events", groupId = "fulfillment-service-group")
    public void consumeInventoryReservedEvent(InventoryReservedEvent event, Acknowledgment acknowledgment) {
        logger.info("========================================");
        logger.info("Received InventoryReservedEvent");
        logger.info("Event ID: {}", event.getEventId());
        logger.info("Event Type: {}", event.getEventType());
        logger.info("Order ID: {}", event.getOrderId());
        logger.info("Product ID: {}", event.getProductId());
        logger.info("Reserved Quantity: {}", event.getReservedQuantity());
        logger.info("Available Quantity: {}", event.getAvailableQuantity());
        logger.info("Inventory Status: {}", event.getInventoryStatus());
        logger.info("========================================");
        
        try {
            // Generate a customer ID based on order ID (in real scenario, this would come from the order data)
            String customerId = "CUST-" + String.format("%05d", event.getOrderId() % 100000);
            
            // Process fulfillment
            fulfillmentService.processFulfillment(
                event.getOrderId(),
                customerId
            );
            
            logger.info("Fulfillment processing completed for order ID: {}", event.getOrderId());
            
            // Acknowledge the message
            acknowledgment.acknowledge();
            logger.info("InventoryReservedEvent processed and acknowledged successfully");
            
        } catch (Exception e) {
            logger.error("Error processing InventoryReservedEvent for order ID: {}", 
                        event.getOrderId(), e);
            // Don't acknowledge - message will be reprocessed
            throw e;
        }
    }
}
