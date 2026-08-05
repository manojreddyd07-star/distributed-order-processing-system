package com.project.fulfillment.consumer;

import com.project.common.config.DlqTopicConfig;
import com.project.common.events.FailedEvent;
import com.project.fulfillment.service.FailedEventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer for Dead Letter Queue (DLQ) events
 */
@Component
public class DlqConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(DlqConsumer.class);
    
    private final FailedEventService failedEventService;

    @Value("${spring.application.name}")
    private String serviceName;
    
    @Autowired
    public DlqConsumer(FailedEventService failedEventService) {
        this.failedEventService = failedEventService;
    }
    
    /**
     * Listen to DLQ topic and persist failed events
     */
    @KafkaListener(
        topics = DlqTopicConfig.DLQ_TOPIC,
        groupId = "${spring.application.name}-dlq-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeFailedEvent(FailedEvent failedEvent) {
        logger.info("Received failed event from DLQ - EventID: {}, Type: {}, Service: {}",
                   failedEvent.getEventId(), failedEvent.getEventType(), failedEvent.getServiceName());
        
        try {
            if (!serviceName.equals(failedEvent.getServiceName())) {
                return;
            }
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
