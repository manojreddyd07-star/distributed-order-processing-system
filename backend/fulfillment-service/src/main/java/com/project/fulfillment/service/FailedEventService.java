package com.project.fulfillment.service;

import com.project.fulfillment.entity.FailedEventEntity;
import com.project.fulfillment.repository.FailedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FailedEventService {
    
    private static final Logger logger = LoggerFactory.getLogger(FailedEventService.class);
    
    private final FailedEventRepository failedEventRepository;
    
    @Autowired
    public FailedEventService(FailedEventRepository failedEventRepository) {
        this.failedEventRepository = failedEventRepository;
    }
    
    /**
     * Save failed event to database
     */
    @Transactional
    public void saveFailedEvent(String eventId, String eventType, String serviceName,
                               String errorMessage, String payload) {
        logger.info("Saving failed event to DLQ database - EventID: {}, Type: {}, Service: {}",
                   eventId, eventType, serviceName);
        
        try {
            FailedEventEntity failedEvent = new FailedEventEntity(
                eventId,
                eventType,
                serviceName,
                errorMessage,
                payload
            );
            
            failedEventRepository.save(failedEvent);
            logger.info("Failed event saved successfully - EventID: {}", eventId);
            
        } catch (Exception e) {
            logger.error("Failed to save failed event - EventID: {}. Error: {}", 
                        eventId, e.getMessage(), e);
        }
    }
    
    /**
     * Get all failed events
     */
    public List<FailedEventEntity> getAllFailedEvents() {
        return failedEventRepository.findAllByOrderByFailedAtDesc();
    }
    
    /**
     * Get failed events by service name
     */
    public List<FailedEventEntity> getFailedEventsByService(String serviceName) {
        return failedEventRepository.findByServiceNameOrderByFailedAtDesc(serviceName);
    }
    
    /**
     * Get failed events by event type
     */
    public List<FailedEventEntity> getFailedEventsByType(String eventType) {
        return failedEventRepository.findByEventTypeOrderByFailedAtDesc(eventType);
    }
}
