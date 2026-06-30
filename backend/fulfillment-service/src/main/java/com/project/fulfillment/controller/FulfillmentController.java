package com.project.fulfillment.controller;

import com.project.fulfillment.entity.FulfillmentAuditLog;
import com.project.fulfillment.entity.FulfillmentEntity;
import com.project.fulfillment.service.FulfillmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fulfillments")
@CrossOrigin(origins = "*")
public class FulfillmentController {
    
    private static final Logger logger = LoggerFactory.getLogger(FulfillmentController.class);
    
    private final FulfillmentService fulfillmentService;
    
    @Autowired
    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }
    
    /**
     * Get all fulfillments
     * @return List of all fulfillments
     */
    @GetMapping
    public ResponseEntity<List<FulfillmentEntity>> getAllFulfillments() {
        logger.info("GET /api/fulfillments - Fetching all fulfillments");
        
        try {
            List<FulfillmentEntity> fulfillments = fulfillmentService.getAllFulfillments();
            logger.info("Successfully retrieved {} fulfillments", fulfillments.size());
            return ResponseEntity.ok(fulfillments);
        } catch (Exception e) {
            logger.error("Error fetching all fulfillments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get fulfillment by ID
     * @param id The fulfillment ID
     * @return The fulfillment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<FulfillmentEntity> getFulfillmentById(@PathVariable Long id) {
        logger.info("GET /api/fulfillments/{} - Fetching fulfillment by ID", id);
        
        try {
            FulfillmentEntity fulfillment = fulfillmentService.getFulfillmentById(id);
            logger.info("Successfully retrieved fulfillment with ID: {}", id);
            return ResponseEntity.ok(fulfillment);
        } catch (RuntimeException e) {
            logger.warn("Fulfillment not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error fetching fulfillment with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get fulfillment by order ID
     * @param orderId The order ID
     * @return The fulfillment details
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<FulfillmentEntity> getFulfillmentByOrderId(@PathVariable Long orderId) {
        logger.info("GET /api/fulfillments/order/{} - Fetching fulfillment by order ID", orderId);
        
        try {
            FulfillmentEntity fulfillment = fulfillmentService.getFulfillmentByOrderId(orderId);
            logger.info("Successfully retrieved fulfillment for order ID: {}", orderId);
            return ResponseEntity.ok(fulfillment);
        } catch (RuntimeException e) {
            logger.warn("Fulfillment not found for order ID: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error fetching fulfillment for order ID: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get fulfillments by customer ID
     * @param customerId The customer ID
     * @return List of fulfillments for the customer
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<FulfillmentEntity>> getFulfillmentsByCustomerId(@PathVariable String customerId) {
        logger.info("GET /api/fulfillments/customer/{} - Fetching fulfillments by customer ID", customerId);
        
        try {
            List<FulfillmentEntity> fulfillments = fulfillmentService.getFulfillmentsByCustomerId(customerId);
            logger.info("Successfully retrieved {} fulfillments for customer ID: {}", fulfillments.size(), customerId);
            return ResponseEntity.ok(fulfillments);
        } catch (Exception e) {
            logger.error("Error fetching fulfillments for customer ID: {}", customerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get fulfillments by status
     * @param status The fulfillment status
     * @return List of fulfillments with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<FulfillmentEntity>> getFulfillmentsByStatus(@PathVariable String status) {
        logger.info("GET /api/fulfillments/status/{} - Fetching fulfillments by status", status);
        
        try {
            List<FulfillmentEntity> fulfillments = fulfillmentService.getFulfillmentsByStatus(status);
            logger.info("Successfully retrieved {} fulfillments with status: {}", fulfillments.size(), status);
            return ResponseEntity.ok(fulfillments);
        } catch (Exception e) {
            logger.error("Error fetching fulfillments by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get audit history for a fulfillment
     * @param fulfillmentId The fulfillment ID
     * @return List of audit logs
     */
    @GetMapping("/{fulfillmentId}/history")
    public ResponseEntity<List<FulfillmentAuditLog>> getAuditHistory(@PathVariable Long fulfillmentId) {
        logger.info("GET /api/fulfillments/{}/history - Fetching audit history", fulfillmentId);
        
        try {
            List<FulfillmentAuditLog> auditLogs = fulfillmentService.getAuditHistory(fulfillmentId);
            logger.info("Successfully retrieved {} audit logs for fulfillment ID: {}", 
                       auditLogs.size(), fulfillmentId);
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            logger.error("Error fetching audit history for fulfillment ID: {}", fulfillmentId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get all audit logs
     * @return List of all audit logs
     */
    @GetMapping("/history")
    public ResponseEntity<List<FulfillmentAuditLog>> getAllAuditLogs() {
        logger.info("GET /api/fulfillments/history - Fetching all audit logs");
        
        try {
            List<FulfillmentAuditLog> auditLogs = fulfillmentService.getAllAuditLogs();
            logger.info("Successfully retrieved {} audit logs", auditLogs.size());
            return ResponseEntity.ok(auditLogs);
        } catch (Exception e) {
            logger.error("Error fetching all audit logs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
