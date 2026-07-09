package com.project.order.service;

import com.project.order.entity.AuditEventEntity;
import com.project.order.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AuditService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    
    private final AuditRepository auditRepository;
    
    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }
    
    /**
     * Save audit event to the database
     */
    @Transactional
    public AuditEventEntity saveAuditEvent(String eventId, String eventType, String serviceName, 
                                           Long orderId, String status, String message) {
        logger.info("Creating audit event - EventID: {}, Type: {}, Service: {}, OrderID: {}, Status: {}", 
                    eventId, eventType, serviceName, orderId, status);
        
        AuditEventEntity auditEvent = new AuditEventEntity(
            eventId, 
            eventType, 
            serviceName, 
            orderId, 
            status, 
            message
        );
        
        try {
            AuditEventEntity savedEvent = auditRepository.save(auditEvent);
            logger.info("Audit event created successfully - ID: {}, EventID: {}", 
                       savedEvent.getId(), savedEvent.getEventId());
            
            // Verify persistence
            if (savedEvent.getId() != null) {
                logger.debug("Audit record persisted with ID: {}", savedEvent.getId());
            }
            
            return savedEvent;
        } catch (Exception e) {
            logger.error("Failed to save audit event - EventID: {}, Error: {}", 
                        eventId, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * Capture OrderCreated events
     */
    @KafkaListener(topics = "order-created", groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void captureOrderCreatedEvent(Map<String, Object> event) {
        try {
            String eventId = (String) event.get("eventId");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String status = (String) event.getOrDefault("status", "CREATED");
            String message = "Order created successfully";
            
            saveAuditEvent(eventId, "ORDER_CREATED", "order-service", orderId, status, message);
            logger.info("Captured OrderCreated event - OrderID: {}, EventID: {}", orderId, eventId);
        } catch (Exception e) {
            logger.error("Error capturing OrderCreated event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Capture Validation events
     */
    @KafkaListener(topics = {"validation-success", "validation-failed"}, 
                   groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void captureValidationEvent(Map<String, Object> event) {
        try {
            String eventId = (String) event.get("eventId");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String topic = (String) event.get("topic");
            String status = topic != null && topic.contains("success") ? "SUCCESS" : "FAILED";
            String eventType = "VALIDATION_" + status;
            String message = (String) event.getOrDefault("message", 
                status.equals("SUCCESS") ? "Validation passed" : "Validation failed");
            
            saveAuditEvent(eventId, eventType, "validation-service", orderId, status, message);
            logger.info("Captured Validation event - OrderID: {}, Status: {}", orderId, status);
        } catch (Exception e) {
            logger.error("Error capturing Validation event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Capture Payment events
     */
    @KafkaListener(topics = {"payment-success", "payment-failed"}, 
                   groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void capturePaymentEvent(Map<String, Object> event) {
        try {
            String eventId = (String) event.get("eventId");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String topic = (String) event.get("topic");
            String status = topic != null && topic.contains("success") ? "SUCCESS" : "FAILED";
            String eventType = "PAYMENT_" + status;
            String message = (String) event.getOrDefault("message", 
                status.equals("SUCCESS") ? "Payment processed successfully" : "Payment processing failed");
            
            saveAuditEvent(eventId, eventType, "payment-service", orderId, status, message);
            logger.info("Captured Payment event - OrderID: {}, Status: {}", orderId, status);
        } catch (Exception e) {
            logger.error("Error capturing Payment event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Capture Inventory events
     */
    @KafkaListener(topics = {"inventory-reserved", "inventory-failed"}, 
                   groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void captureInventoryEvent(Map<String, Object> event) {
        try {
            String eventId = (String) event.get("eventId");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String topic = (String) event.get("topic");
            String status = topic != null && topic.contains("reserved") ? "SUCCESS" : "FAILED";
            String eventType = "INVENTORY_" + status;
            String message = (String) event.getOrDefault("message", 
                status.equals("SUCCESS") ? "Inventory reserved" : "Inventory reservation failed");
            
            saveAuditEvent(eventId, eventType, "inventory-service", orderId, status, message);
            logger.info("Captured Inventory event - OrderID: {}, Status: {}", orderId, status);
        } catch (Exception e) {
            logger.error("Error capturing Inventory event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Capture Fulfillment events
     */
    @KafkaListener(topics = {"fulfillment-completed", "fulfillment-failed"}, 
                   groupId = "audit-service-group", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void captureFulfillmentEvent(Map<String, Object> event) {
        try {
            String eventId = (String) event.get("eventId");
            Long orderId = ((Number) event.get("orderId")).longValue();
            String topic = (String) event.get("topic");
            String status = topic != null && topic.contains("completed") ? "SUCCESS" : "FAILED";
            String eventType = "FULFILLMENT_" + status;
            String message = (String) event.getOrDefault("message", 
                status.equals("SUCCESS") ? "Fulfillment completed" : "Fulfillment failed");
            
            saveAuditEvent(eventId, eventType, "fulfillment-service", orderId, status, message);
            logger.info("Captured Fulfillment event - OrderID: {}, Status: {}", orderId, status);
        } catch (Exception e) {
            logger.error("Error capturing Fulfillment event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Get audit events by order ID
     */
    public List<AuditEventEntity> getAuditEventsByOrderId(Long orderId) {
        logger.debug("Fetching audit events for OrderID: {}", orderId);
        return auditRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
    }
    
    /**
     * Get audit events with pagination
     */
    public Page<AuditEventEntity> getAuditEvents(Pageable pageable) {
        logger.debug("Fetching all audit events with pagination");
        return auditRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
    
    /**
     * Get filtered audit events
     */
    public Page<AuditEventEntity> getFilteredAuditEvents(String eventType, String serviceName, 
                                                          String status, Pageable pageable) {
        logger.debug("Fetching filtered audit events - Type: {}, Service: {}, Status: {}", 
                    eventType, serviceName, status);
        return auditRepository.findByFilters(eventType, serviceName, status, pageable);
    }
}
