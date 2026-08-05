package com.project.fulfillment.service;

import com.project.fulfillment.entity.IdempotencyRecordEntity;
import com.project.fulfillment.repository.IdempotencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class IdempotencyService {
    
    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String SERVICE_NAME = "fulfillment-service";
    
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
        boolean exists = idempotencyRepository.findByEventId(eventId)
                .map(record -> "PROCESSED".equals(record.getProcessingStatus()))
                .orElse(false);
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
            IdempotencyRecordEntity record = idempotencyRepository.findByEventId(eventId)
                .orElseGet(() -> new IdempotencyRecordEntity(eventId, eventType, SERVICE_NAME, processingStatus));
            record.setEventType(eventType);
            record.setProcessingStatus(processingStatus);
            idempotencyRepository.save(record);
            logger.info("Marked event {} as {} in {}", eventId, processingStatus, SERVICE_NAME);
        } catch (Exception e) {
            logger.error("Error marking event {} as processed: {}", eventId, e.getMessage());
            throw new IllegalStateException("Unable to persist idempotency record", e);
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
