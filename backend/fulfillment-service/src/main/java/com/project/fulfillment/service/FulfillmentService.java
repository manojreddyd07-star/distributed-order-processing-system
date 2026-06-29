package com.project.fulfillment.service;

import com.project.fulfillment.entity.FulfillmentEntity;
import com.project.fulfillment.repository.FulfillmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class FulfillmentService {
    
    private static final Logger logger = LoggerFactory.getLogger(FulfillmentService.class);
    private static final String DEFAULT_FULFILLMENT_STATUS = "PENDING";
    
    private final FulfillmentRepository fulfillmentRepository;
    
    @Autowired
    public FulfillmentService(FulfillmentRepository fulfillmentRepository) {
        this.fulfillmentRepository = fulfillmentRepository;
    }
    
    /**
     * Process fulfillment for an order
     * @param orderId The order ID
     * @param customerId The customer ID
     * @return The created fulfillment entity
     */
    @Transactional
    public FulfillmentEntity processFulfillment(Long orderId, String customerId) {
        logger.info("========================================");
        logger.info("Processing fulfillment for Order ID: {}", orderId);
        logger.info("Customer ID: {}", customerId);
        
        // Check if fulfillment already exists for this order
        if (fulfillmentRepository.existsByOrderId(orderId)) {
            logger.warn("Fulfillment already exists for order ID: {}", orderId);
            return fulfillmentRepository.findByOrderId(orderId).orElse(null);
        }
        
        // Generate tracking number
        String trackingNumber = generateTrackingNumber();
        logger.info("Generated tracking number: {}", trackingNumber);
        
        // Create fulfillment record with default status
        FulfillmentEntity fulfillment = new FulfillmentEntity(
            orderId,
            customerId,
            DEFAULT_FULFILLMENT_STATUS,
            trackingNumber
        );
        
        // Persist fulfillment record
        FulfillmentEntity savedFulfillment = fulfillmentRepository.save(fulfillment);
        
        logger.info("Fulfillment record created successfully");
        logger.info("Fulfillment ID: {}", savedFulfillment.getFulfillmentId());
        logger.info("Tracking Number: {}", savedFulfillment.getTrackingNumber());
        logger.info("Status: {}", savedFulfillment.getFulfillmentStatus());
        logger.info("Created At: {}", savedFulfillment.getCreatedAt());
        logger.info("========================================");
        
        return savedFulfillment;
    }
    
    /**
     * Generate a unique tracking number
     * @return Tracking number in format TRK-XXXXXXXXXX
     */
    private String generateTrackingNumber() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        return "TRK-" + uuid;
    }
    
    /**
     * Get all fulfillments
     * @return List of all fulfillments
     */
    public List<FulfillmentEntity> getAllFulfillments() {
        logger.info("Fetching all fulfillments");
        List<FulfillmentEntity> fulfillments = fulfillmentRepository.findAll();
        logger.info("Retrieved {} fulfillments", fulfillments.size());
        return fulfillments;
    }
    
    /**
     * Get fulfillment by ID
     * @param fulfillmentId The fulfillment ID
     * @return The fulfillment entity
     */
    public FulfillmentEntity getFulfillmentById(Long fulfillmentId) {
        logger.info("Fetching fulfillment by ID: {}", fulfillmentId);
        return fulfillmentRepository.findById(fulfillmentId)
                .orElseThrow(() -> {
                    logger.error("Fulfillment not found with ID: {}", fulfillmentId);
                    return new RuntimeException("Fulfillment not found with ID: " + fulfillmentId);
                });
    }
    
    /**
     * Get fulfillment by order ID
     * @param orderId The order ID
     * @return The fulfillment entity
     */
    public FulfillmentEntity getFulfillmentByOrderId(Long orderId) {
        logger.info("Fetching fulfillment by order ID: {}", orderId);
        return fulfillmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> {
                    logger.error("Fulfillment not found for order ID: {}", orderId);
                    return new RuntimeException("Fulfillment not found for order ID: " + orderId);
                });
    }
    
    /**
     * Get fulfillments by customer ID
     * @param customerId The customer ID
     * @return List of fulfillments for the customer
     */
    public List<FulfillmentEntity> getFulfillmentsByCustomerId(String customerId) {
        logger.info("Fetching fulfillments for customer ID: {}", customerId);
        List<FulfillmentEntity> fulfillments = fulfillmentRepository.findByCustomerId(customerId);
        logger.info("Retrieved {} fulfillments for customer: {}", fulfillments.size(), customerId);
        return fulfillments;
    }
    
    /**
     * Get fulfillments by status
     * @param status The fulfillment status
     * @return List of fulfillments with the specified status
     */
    public List<FulfillmentEntity> getFulfillmentsByStatus(String status) {
        logger.info("Fetching fulfillments by status: {}", status);
        List<FulfillmentEntity> fulfillments = fulfillmentRepository.findByFulfillmentStatus(status);
        logger.info("Retrieved {} fulfillments with status: {}", fulfillments.size(), status);
        return fulfillments;
    }
}
