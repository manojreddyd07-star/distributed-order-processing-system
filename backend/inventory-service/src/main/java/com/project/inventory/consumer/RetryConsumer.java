package com.project.inventory.consumer;

import com.project.common.config.RetryTopicConfig;
import com.project.common.events.RetryEvent;
import com.project.inventory.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class RetryConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryConsumer.class);
    
    private final RetryService retryService;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Autowired
    public RetryConsumer(RetryService retryService) {
        this.retryService = retryService;
    }
    
    @KafkaListener(
        topics = RetryTopicConfig.RETRY_TOPIC,
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeRetryEvent(
            @Payload RetryEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("========== Retry Event Consumption ==========");
        logger.info("Received RetryEvent from topic: {}, partition: {}, offset: {}", topic, partition, offset);
        logger.info("Retry event details: {}", event);
        
        try {
            if (!serviceName.equals(event.getServiceName())) {
                logger.debug("Skipping retry event - not for this service. Target: {}, Current: {}", 
                           event.getServiceName(), serviceName);
                acknowledgment.acknowledge();
                return;
            }
            
            if (event.getNextRetryTime() != null && event.getNextRetryTime().isAfter(LocalDateTime.now())) {
                logger.info("Retry event not ready yet. Scheduled for: {}", event.getNextRetryTime());
                acknowledgment.acknowledge();
                return;
            }
            
            logger.info("========== Retry Attempt ==========");
            logger.info("Event ID: {}", event.getOriginalEventId());
            logger.info("Event Type: {}", event.getEventType());
            logger.info("Retry Count: {}/{}", event.getRetryCount() + 1, event.getMaxRetries());
            logger.info("Last Failure: {}", event.getFailureReason());
            
            retryService.processRetryEvent(event);
            
            logger.info("========== Retry Success ==========");
            logger.info("Successfully processed retry for event: {}", event.getOriginalEventId());
            logger.info("======================================");
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("========== Retry Failure ==========");
            logger.error("Error processing retry event: {}. Error: {}", 
                        event.getOriginalEventId(), e.getMessage(), e);
            logger.error("Retry Count: {}/{}", event.getRetryCount() + 1, event.getMaxRetries());
            logger.error("======================================");
            
            retryService.retryFailedEvent(event, e.getMessage());
            
            acknowledgment.acknowledge();
        }
    }
}
