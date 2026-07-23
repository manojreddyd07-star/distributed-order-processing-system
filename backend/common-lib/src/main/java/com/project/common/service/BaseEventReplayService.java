package com.project.common.service;

import com.project.common.dto.ReplayRequest;
import com.project.common.dto.ReplayResponse;
import com.project.common.util.JsonUtil;
import com.project.common.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Abstract base class for Event Replay Service functionality
 * Provides common replay logic that can be extended by individual services
 * 
 * @param <T> The FailedEventEntity type for the specific service
 * @param <R> The FailedEventRepository type for the specific service
 */
public abstract class BaseEventReplayService<T, R> {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseEventReplayService.class);
    
    protected final R failedEventRepository;
    protected final KafkaTemplate<String, Object> kafkaTemplate;
    
    protected BaseEventReplayService(R failedEventRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.failedEventRepository = failedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
    
    /**
     * Replay a failed event by republishing it to the specified Kafka topic
     */
    @Transactional
    public ReplayResponse replayEvent(ReplayRequest request) {
        logger.info("Replay request received: {}", request);
        
        // Validate request
        try {
            ValidationUtil.requireNonEmpty(request.getEventId(), "Event ID");
            ValidationUtil.requireNonEmpty(request.getReplayTopic(), "Replay topic");
        } catch (IllegalArgumentException e) {
            logger.error("Replay request validation failed: {}", e.getMessage());
            return ReplayResponse.error(e.getMessage());
        }
        
        try {
            // Find the failed event
            Optional<T> failedEventOpt = findByEventId(request.getEventId());
            
            if (!failedEventOpt.isPresent()) {
                logger.error("Failed event not found with eventId: {}", request.getEventId());
                return ReplayResponse.error("Failed event not found with ID: " + request.getEventId());
            }
            
            T failedEvent = failedEventOpt.get();
            String eventType = getEventType(failedEvent);
            String payload = getPayload(failedEvent);
            
            // Log replay request
            logger.info("Replaying event - EventID: {}, EventType: {}, ReplayTopic: {}",
                       request.getEventId(), eventType, request.getReplayTopic());
            
            // Parse the payload back to an object
            Object eventPayload = JsonUtil.fromJson(payload);
            
            // Republish event to the specified Kafka topic
            kafkaTemplate.send(request.getReplayTopic(), eventPayload);
            
            // Log replay result
            logger.info("Event replayed successfully - EventID: {}, Topic: {}", 
                       request.getEventId(), request.getReplayTopic());
            
            // Return success response
            return ReplayResponse.success(
                request.getEventId(),
                eventType,
                request.getReplayTopic()
            );
            
        } catch (Exception e) {
            logger.error("Failed to replay event - EventID: {}. Error: {}", 
                        request.getEventId(), e.getMessage(), e);
            return ReplayResponse.error("Failed to replay event: " + e.getMessage());
        }
    }
    
    /**
     * Find failed event by event ID - to be implemented by subclasses
     */
    protected abstract Optional<T> findByEventId(String eventId);
    
    /**
     * Get event type from failed event entity - to be implemented by subclasses
     */
    protected abstract String getEventType(T failedEvent);
    
    /**
     * Get payload from failed event entity - to be implemented by subclasses
     */
    protected abstract String getPayload(T failedEvent);
}
