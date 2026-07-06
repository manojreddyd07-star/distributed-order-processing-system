package com.project.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.RetryTopicConfig;
import com.project.common.events.PaymentCompletedEvent;
import com.project.common.events.RetryEvent;
import com.project.inventory.entity.RetryRecordEntity;
import com.project.inventory.repository.RetryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RetryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryService.class);
    
    private final RetryRepository retryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final InventoryService inventoryService;
    private final ObjectMapper objectMapper;
    
    @Value("${spring.application.name}")
    private String serviceName;
    
    @Autowired
    public RetryService(RetryRepository retryRepository,
                       KafkaTemplate<String, Object> kafkaTemplate,
                       InventoryService inventoryService,
                       ObjectMapper objectMapper) {
        this.retryRepository = retryRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.inventoryService = inventoryService;
        this.objectMapper = objectMapper;
    }
    
    @Transactional
    public void processRetryEvent(RetryEvent retryEvent) {
        logger.info("Processing retry event: {}", retryEvent.getRetryId());
        
        try {
            Class<?> eventClass = Class.forName(retryEvent.getEventClass());
            Object originalEvent = objectMapper.readValue(retryEvent.getEventPayload(), eventClass);
            
            if (originalEvent instanceof PaymentCompletedEvent) {
                PaymentCompletedEvent paymentEvent = (PaymentCompletedEvent) originalEvent;
                
                boolean inventoryAvailable = inventoryService.verifyInventory(
                    paymentEvent.getProductId(), 
                    paymentEvent.getQuantity()
                );
                
                if (inventoryAvailable) {
                    inventoryService.reserveInventory(
                        paymentEvent.getProductId(),
                        paymentEvent.getProductName(),
                        paymentEvent.getQuantity(),
                        paymentEvent.getOrderId()
                    );
                } else {
                    logger.warn("Inventory not available for product: {} - Insufficient stock", 
                               paymentEvent.getProductId());
                }
                
                updateRetryStatus(retryEvent.getRetryId(), "SUCCESS", null);
                logger.info("Retry successful for event: {}", retryEvent.getOriginalEventId());
                
            } else {
                throw new IllegalArgumentException("Unsupported event type: " + retryEvent.getEventClass());
            }
            
        } catch (Exception e) {
            logger.error("Retry attempt failed for event: {}. Error: {}", 
                        retryEvent.getOriginalEventId(), e.getMessage(), e);
            retryFailedEvent(retryEvent, e.getMessage());
        }
    }
    
    @Transactional
    public void retryFailedEvent(RetryEvent retryEvent, String failureReason) {
        logger.info("Handling failed retry for event: {}", retryEvent.getOriginalEventId());
        
        int newRetryCount = retryEvent.getRetryCount() + 1;
        
        if (RetryTopicConfig.isMaxRetriesReached(newRetryCount)) {
            logger.error("Max retries exhausted for event: {}. Final failure.", 
                        retryEvent.getOriginalEventId());
            updateRetryStatus(retryEvent.getRetryId(), "EXHAUSTED", failureReason);
        } else {
            long retryInterval = RetryTopicConfig.getRetryInterval(newRetryCount);
            LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(retryInterval / 1000);
            
            logger.info("Scheduling retry {} for event: {}. Next retry at: {}", 
                       newRetryCount, retryEvent.getOriginalEventId(), nextRetryTime);
            
            RetryRecordEntity retryRecord = retryRepository.findByRetryId(retryEvent.getRetryId())
                    .orElseThrow(() -> new RuntimeException("Retry record not found: " + retryEvent.getRetryId()));
            
            retryRecord.setRetryCount(newRetryCount);
            retryRecord.setRetryStatus("PENDING");
            retryRecord.setLastRetryTime(LocalDateTime.now());
            retryRecord.setNextRetryTime(nextRetryTime);
            retryRecord.setFailureReason(failureReason);
            
            retryRepository.save(retryRecord);
            
            RetryEvent updatedRetryEvent = createRetryEventFromRecord(retryRecord);
            kafkaTemplate.send(RetryTopicConfig.RETRY_TOPIC, updatedRetryEvent);
            
            logger.info("Retry event rescheduled for event: {}", retryEvent.getOriginalEventId());
        }
    }
    
    @Transactional
    public void createRetryEvent(Object event, String eventType, String eventId, 
                                String targetTopic, String failureReason) {
        logger.info("Creating retry event for failed event: {}", eventId);
        
        try {
            String eventPayload = objectMapper.writeValueAsString(event);
            String eventClass = event.getClass().getName();
            
            String retryId = UUID.randomUUID().toString();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRetryTime = now.plusSeconds(RetryTopicConfig.RETRY_INTERVAL_1 / 1000);
            
            RetryRecordEntity retryRecord = new RetryRecordEntity(
                retryId, eventId, eventType, 0, "PENDING", now, nextRetryTime,
                failureReason, eventPayload, eventClass, serviceName, targetTopic,
                RetryTopicConfig.MAX_RETRY_ATTEMPTS
            );
            
            retryRepository.save(retryRecord);
            
            RetryEvent retryEvent = createRetryEventFromRecord(retryRecord);
            kafkaTemplate.send(RetryTopicConfig.RETRY_TOPIC, retryEvent);
            
            logger.info("Retry event created and sent to retry topic: {}", retryId);
            
        } catch (Exception e) {
            logger.error("Failed to create retry event for event: {}. Error: {}", 
                        eventId, e.getMessage(), e);
        }
    }
    
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void processPendingRetries() {
        LocalDateTime now = LocalDateTime.now();
        List<RetryRecordEntity> pendingRetries = retryRepository.findPendingRetries(now);
        
        if (!pendingRetries.isEmpty()) {
            logger.info("Processing {} pending retries", pendingRetries.size());
            
            for (RetryRecordEntity retryRecord : pendingRetries) {
                try {
                    RetryEvent retryEvent = createRetryEventFromRecord(retryRecord);
                    processRetryEvent(retryEvent);
                } catch (Exception e) {
                    logger.error("Error processing pending retry: {}. Error: {}", 
                                retryRecord.getRetryId(), e.getMessage(), e);
                }
            }
        }
    }
    
    public List<RetryRecordEntity> getAllRetryRecords() {
        return retryRepository.findAllByOrderByCreatedAtDesc();
    }
    
    @Transactional
    private void updateRetryStatus(String retryId, String status, String failureReason) {
        RetryRecordEntity retryRecord = retryRepository.findByRetryId(retryId)
                .orElseThrow(() -> new RuntimeException("Retry record not found: " + retryId));
        
        retryRecord.setRetryStatus(status);
        retryRecord.setLastRetryTime(LocalDateTime.now());
        if (failureReason != null) {
            retryRecord.setFailureReason(failureReason);
        }
        
        retryRepository.save(retryRecord);
    }
    
    private RetryEvent createRetryEventFromRecord(RetryRecordEntity record) {
        return new RetryEvent(
            record.getRetryId(), record.getOriginalEventId(), record.getEventType(),
            record.getRetryCount(), record.getLastRetryTime(), record.getNextRetryTime(),
            record.getFailureReason(), record.getEventPayload(), record.getEventClass(),
            record.getServiceName(), record.getTargetTopic(), record.getMaxRetries()
        );
    }
}
