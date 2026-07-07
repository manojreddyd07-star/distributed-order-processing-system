package com.project.payment.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.DlqTopicConfig;
import com.project.common.events.FailedEvent;
import com.project.payment.service.FailedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for Dead Letter Queue (DLQ) events
 */
@Component
public class DlqConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);
    
    private final FailedEventService failedEventService;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public DlqConsumer(FailedEventService failedEventService, ObjectMapper objectMapper) {
        this.failedEventService = failedEventService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Listen to DLQ topic and persist failed events
     */
    @KafkaListener(
        topics = DlqTopicConfig.DLQ_TOPIC,
        groupId = DlqTopicConfig.DLQ_CONSUMER_GROUP,
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFailedEvent(FailedEvent failedEvent) {
        logger.info("Received failed event from DLQ - EventID: {}, Type: {}, Service: {}",
                   failedEvent.getEventId(), failedEvent.getEventType(), failedEvent.getServiceName());
        
        try {
            // Persist to database
            failedEventService.saveFailedEvent(
                failedEvent.getEventId(),
                failedEvent.getEventType(),
                failedEvent.getServiceName(),
                failedEvent.getErrorMessage(),
                failedEvent.getPayload()
            );
            
            logger.info("Failed event persisted successfully - EventID: {}", failedEvent.getEventId());
            
        } catch (Exception e) {
            logger.error("Error persisting failed event - EventID: {}. Error: {}",
                        failedEvent.getEventId(), e.getMessage(), e);
        }
    }
}
