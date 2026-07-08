package com.project.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.validation.dto.ReplayRequest;
import com.project.validation.dto.ReplayResponse;
import com.project.validation.entity.FailedEventEntity;
import com.project.validation.repository.FailedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EventReplayService {
    
    private static final Logger logger = LoggerFactory.getLogger(EventReplayService.class);
    
    private final FailedEventRepository failedEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventReplayService(FailedEventRepository failedEventRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.failedEventRepository = failedEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Replay a failed event by republishing it to the specified Kafka topic
     */
    @Transactional
    public ReplayResponse replayEvent(ReplayRequest request) {
        logger.info("Replay request received: {}", request);
        
        // Validate request
        if (request.getEventId() == null || request.getEventId().trim().isEmpty()) {
            logger.error("Replay request validation failed: eventId is required");
            return ReplayResponse.error("Event ID is required");
        }
        
        if (request.getReplayTopic() == null || request.getReplayTopic().trim().isEmpty()) {
            logger.error("Replay request validation failed: replayTopic is required");
            return ReplayResponse.error("Replay topic is required");
        }
        
        try {
            // Check if event exists in FailedEvent table
            Optional<FailedEventEntity> failedEventOpt = failedEventRepository.findByEventId(request.getEventId());
            
            if (!failedEventOpt.isPresent()) {
                logger.error("Failed event not found with eventId: {}", request.getEventId());
                return ReplayResponse.error("Failed event not found with ID: " + request.getEventId());
            }
            
            FailedEventEntity failedEvent = failedEventOpt.get();
            
            // Log replay request
            logger.info("Replaying event - EventID: {}, EventType: {}, ReplayTopic: {}",
                       request.getEventId(), failedEvent.getEventType(), request.getReplayTopic());
            
            // Parse the payload back to an object
            Object eventPayload = parsePayload(failedEvent.getPayload(), failedEvent.getEventType());
            
            // Republish event to the specified Kafka topic
            kafkaTemplate.send(request.getReplayTopic(), eventPayload);
            
            // Log replay result
            logger.info("Event replayed successfully - EventID: {}, Topic: {}", 
                       request.getEventId(), request.getReplayTopic());
            
            // Return success response
            return ReplayResponse.success(
                request.getEventId(),
                failedEvent.getEventType(),
                request.getReplayTopic()
            );
            
        } catch (Exception e) {
            logger.error("Failed to replay event - EventID: {}. Error: {}", 
                        request.getEventId(), e.getMessage(), e);
            return ReplayResponse.error("Failed to replay event: " + e.getMessage());
        }
    }
    
    /**
     * Parse JSON payload to appropriate event object
     */
    private Object parsePayload(String payload, String eventType) throws Exception {
        // For now, we'll return the parsed JSON as a generic object
        // In production, you might want to deserialize to specific event classes
        return objectMapper.readValue(payload, Object.class);
    }
}
