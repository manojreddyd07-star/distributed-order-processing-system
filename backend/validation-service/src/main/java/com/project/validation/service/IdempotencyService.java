package com.project.validation.service;

import com.project.validation.entity.IdempotencyRecordEntity;
import com.project.validation.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String SERVICE_NAME = "validation-service";
    
    private final IdempotencyRepository idempotencyRepository;
    
    @Autowired
    public IdempotencyService(IdempotencyRepository idempotencyRepository) {
        this.idempotencyRepository = idempotencyRepository;
    }
    
    /**
     * Check if an event has already been processed
     * @param eventId The unique event ID
     * @return true if the event has already been processed
     */
    public boolean isEventProcessed(String eventId) {
        boolean exists = idempotencyRepository.existsByEventId(eventId);
        if (exists) {
            logger.info("Event {} has already been processed by {}", eventId, SERVICE_NAME);
        }
        return exists;
    }
    
    /**
     * Mark an event as processed
     * @param eventId The unique event ID
     * @param eventType The type of event
     * @param processingStatus The processing status (e.g., "PROCESSED", "FAILED")
     */
    @Transactional
    public void markEventAsProcessed(String eventId, String eventType, String processingStatus) {
        try {
            IdempotencyRecordEntity record = new IdempotencyRecordEntity(
                eventId,
                eventType,
                SERVICE_NAME,
                processingStatus
            );
            idempotencyRepository.save(record);
            logger.info("Marked event {} as {} in {}", eventId, processingStatus, SERVICE_NAME);
        } catch (Exception e) {
            logger.error("Error marking event {} as processed: {}", eventId, e.getMessage());
            // Don't throw exception - this is a best-effort tracking mechanism
        }
    }
    
    /**
     * Get all idempotency records
     * @return List of all idempotency records
     */
    public List<IdempotencyRecordEntity> getAllRecords() {
        return idempotencyRepository.findAll();
    }
}
