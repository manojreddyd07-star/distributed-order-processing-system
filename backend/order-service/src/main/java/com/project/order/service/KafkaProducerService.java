package com.project.order.service;

import com.project.order.entity.OrderEntity;
import com.project.common.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);
    private static final String ORDER_CREATED_TOPIC = "order-created";
    private static final String ORDER_CREATED_EVENT_TYPE = "ORDER_CREATED";

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishOrderCreatedEvent(OrderEntity order) {
        try {
            // Create event with metadata
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setEventType(ORDER_CREATED_EVENT_TYPE);
            event.setEventTimestamp(LocalDateTime.now());
            
            // Set order data
            event.setOrderId(order.getId());
            event.setCustomerId(order.getCustomerId());
            event.setOrderStatus(order.getStatus());
            event.setTotalAmount(order.getTotalAmount());
            event.setCreatedAt(order.getCreatedAt());

            logger.info("Publishing order created event: {}", event);

            // Publish to Kafka
            CompletableFuture<SendResult<String, OrderCreatedEvent>> future = 
                kafkaTemplate.send(ORDER_CREATED_TOPIC, order.getId().toString(), event);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    logger.info("Successfully published order created event for order ID: {} to topic: {} at offset: {}",
                            order.getId(),
                            ORDER_CREATED_TOPIC,
                            result.getRecordMetadata().offset());
                } else {
                    logger.error("Failed to publish order created event for order ID: {}", order.getId(), ex);
                }
            });

        } catch (Exception e) {
            logger.error("Error publishing order created event for order ID: {}", order.getId(), e);
        }
    }
}
