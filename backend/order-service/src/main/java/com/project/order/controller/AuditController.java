package com.project.order.controller;

import com.project.order.entity.AuditEventEntity;
import com.project.order.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);
    
    private final AuditService auditService;
    
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }
    
    /**
     * Get all audit events with pagination
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllAuditEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("Fetching audit events - Page: {}, Size: {}", page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditEventEntity> auditPage = auditService.getAuditEvents(pageable);
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", auditPage.getContent());
            response.put("currentPage", auditPage.getNumber());
            response.put("totalItems", auditPage.getTotalElements());
            response.put("totalPages", auditPage.getTotalPages());
            response.put("hasNext", auditPage.hasNext());
            response.put("hasPrevious", auditPage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching audit events: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get audit events by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<AuditEventEntity>> getAuditEventsByOrderId(@PathVariable Long orderId) {
        try {
            logger.info("Fetching audit events for OrderID: {}", orderId);
            List<AuditEventEntity> events = auditService.getAuditEventsByOrderId(orderId);
            return ResponseEntity.ok(events);
        } catch (Exception e) {
            logger.error("Error fetching audit events for OrderID {}: {}", orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get filtered audit events
     */
    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> getFilteredAuditEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            logger.info("Fetching filtered audit events - Type: {}, Service: {}, Status: {}, Page: {}, Size: {}", 
                       eventType, serviceName, status, page, size);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditEventEntity> auditPage = auditService.getFilteredAuditEvents(
                eventType, serviceName, status, pageable
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("events", auditPage.getContent());
            response.put("currentPage", auditPage.getNumber());
            response.put("totalItems", auditPage.getTotalElements());
            response.put("totalPages", auditPage.getTotalPages());
            response.put("hasNext", auditPage.hasNext());
            response.put("hasPrevious", auditPage.hasPrevious());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching filtered audit events: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "audit");
        return ResponseEntity.ok(response);
    }
}
