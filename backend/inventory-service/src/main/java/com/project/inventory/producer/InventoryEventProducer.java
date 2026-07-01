package com.project.inventory.producer;

import com.project.common.events.InventoryReservedEvent;
import com.project.common.events.InventoryRejectedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class InventoryEventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryEventProducer.class);
    private static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved";
    private static final String INVENTORY_REJECTED_TOPIC = "inventory-rejected";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    public InventoryEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Publish InventoryReservedEvent to Kafka
     * @param event The InventoryReservedEvent to publish
     */
    public void publishInventoryReservedEvent(InventoryReservedEvent event) {
        logger.info("Publishing InventoryReservedEvent - EventId: {}, ProductId: {}, OrderId: {}", 
                   event.getEventId(), event.getProductId(), event.getOrderId());
        
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, event.getProductId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("InventoryReservedEvent published successfully - EventId: {}, Topic: {}, Partition: {}, Offset: {}", 
                               event.getEventId(), 
                               INVENTORY_RESERVED_TOPIC,
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish InventoryReservedEvent - EventId: {}, Error: {}", 
                                event.getEventId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Exception while publishing InventoryReservedEvent - EventId: {}, Error: {}", 
                        event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory reserved event", e);
        }
    }
    
    /**
     * Publish InventoryRejectedEvent to Kafka
     * @param event The InventoryRejectedEvent to publish
     */
    public void publishInventoryRejectedEvent(InventoryRejectedEvent event) {
        logger.info("Publishing InventoryRejectedEvent - EventId: {}, ProductId: {}, OrderId: {}, Reason: {}", 
                   event.getEventId(), event.getProductId(), event.getOrderId(), event.getReason());
        
        try {
            CompletableFuture<SendResult<String, Object>> future = 
                kafkaTemplate.send(INVENTORY_REJECTED_TOPIC, event.getProductId(), event);
            
            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("InventoryRejectedEvent published successfully - EventId: {}, Topic: {}, Partition: {}, Offset: {}", 
                               event.getEventId(), 
                               INVENTORY_REJECTED_TOPIC,
                               result.getRecordMetadata().partition(),
                               result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish InventoryRejectedEvent - EventId: {}, Error: {}", 
                                event.getEventId(), ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            logger.error("Exception while publishing InventoryRejectedEvent - EventId: {}, Error: {}", 
                        event.getEventId(), e.getMessage(), e);
            throw new RuntimeException("Failed to publish inventory rejected event", e);
        }
    }
}
