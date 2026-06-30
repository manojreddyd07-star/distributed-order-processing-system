package com.project.fulfillment.producer;

import com.project.fulfillment.entity.FulfillmentEntity;
import com.project.fulfillment.event.OrderCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class FulfillmentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(FulfillmentEventProducer.class);
    private static final String ORDER_COMPLETED_TOPIC = "order-completed";
    private static final String ORDER_COMPLETED_EVENT_TYPE = "ORDER_COMPLETED";

    private final KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate;

    @Autowired
    public FulfillmentEventProducer(KafkaTemplate<String, OrderCompletedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publish OrderCompletedEvent to Kafka
     * @param fulfillment The fulfillment entity
     */
    public void publishOrderCompletedEvent(FulfillmentEntity fulfillment) {
        try {
            // Create event with metadata
            OrderCompletedEvent event = new OrderCompletedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType(ORDER_COMPLETED_EVENT_TYPE);
            event.setEventTimestamp(LocalDateTime.now());
            
            // Set order and fulfillment data
            event.setOrderId(fulfillment.getOrderId());
            event.setCustomerId(fulfillment.getCustomerId());
            event.setFulfillmentId(fulfillment.getFulfillmentId());
            event.setTrackingNumber(fulfillment.getTrackingNumber());
            event.setFulfillmentStatus(fulfillment.getFulfillmentStatus());

            logger.info("========================================");
            logger.info("Publishing OrderCompletedEvent");
            logger.info("Event ID: {}", event.getEventId());
            logger.info("Order ID: {}", event.getOrderId());
            logger.info("Fulfillment ID: {}", event.getFulfillmentId());
            logger.info("Tracking Number: {}", event.getTrackingNumber());
            logger.info("========================================");

            // Publish to Kafka
            CompletableFuture<SendResult<String, OrderCompletedEvent>> future = 
                kafkaTemplate.send(ORDER_COMPLETED_TOPIC, fulfillment.getOrderId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published OrderCompletedEvent for order ID: {} to topic: {} at offset: {}",
                            fulfillment.getOrderId(),
                            ORDER_COMPLETED_TOPIC,
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish OrderCompletedEvent for order ID: {}", 
                            fulfillment.getOrderId(), ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing OrderCompletedEvent for order ID: {}", 
                    fulfillment.getOrderId(), e);
            throw new RuntimeException("Failed to publish order completed event", e);
        }
    }
}
