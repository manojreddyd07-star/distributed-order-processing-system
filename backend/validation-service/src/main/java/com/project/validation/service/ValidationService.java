package com.project.validation.service;

import com.project.validation.entity.ValidationEntity;
import com.project.common.events.OrderCreatedEvent;
import com.project.validation.event.ValidationEventProducer;
import com.project.validation.repository.ValidationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ValidationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    
    private final ValidationRepository validationRepository;
    private final ValidationEventProducer validationEventProducer;
    
    @Autowired
    public ValidationService(ValidationRepository validationRepository,
                           ValidationEventProducer validationEventProducer) {
        this.validationRepository = validationRepository;
        this.validationEventProducer = validationEventProducer;
    }
    
    /**
     * Validates an order based on business rules
     * @param event The OrderCreatedEvent to validate
     * @return ValidationEntity representing the validation result
     */
    public ValidationEntity validateOrder(OrderCreatedEvent event) {
        logger.info("Starting validation for order ID: {}", event.getOrderId());
        
        List<String> validationErrors = new ArrayList<>();
        
        // Validate customerId is not null
        if (event.getCustomerId() == null) {
            validationErrors.add("Customer ID cannot be null");
            logger.warn("Validation failed for order {}: Customer ID is null", event.getOrderId());
        }
        
        // Validate customerId is not empty
        if (event.getCustomerId() != null && event.getCustomerId().trim().isEmpty()) {
            validationErrors.add("Customer ID cannot be empty");
            logger.warn("Validation failed for order {}: Customer ID is empty", event.getOrderId());
        }
        
        // Validate totalAmount is greater than 0
        if (event.getTotalAmount() == null || event.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            validationErrors.add("Total amount must be greater than 0");
            logger.warn("Validation failed for order {}: Total amount is {} (must be > 0)", 
                       event.getOrderId(), event.getTotalAmount());
        }
        
        // Create validation result
        ValidationEntity validationResult;
        
        if (validationErrors.isEmpty()) {
            // Validation passed
            validationResult = new ValidationEntity(
                event.getOrderId(),
                "VALID",
                "Order validation successful"
            );
            logger.info("Validation successful for order ID: {}", event.getOrderId());
        } else {
            // Validation failed
            String errorMessage = String.join("; ", validationErrors);
            validationResult = new ValidationEntity(
                event.getOrderId(),
                "INVALID",
                errorMessage
            );
            logger.error("Validation failed for order ID: {}. Errors: {}", 
                        event.getOrderId(), errorMessage);
        }
        
        // Save validation result to database
        ValidationEntity savedResult = validationRepository.save(validationResult);
        logger.info("Validation result saved for order ID: {} with status: {}", 
                   event.getOrderId(), savedResult.getValidationStatus());
        
        // Publish Kafka events based on validation result
        if (validationErrors.isEmpty()) {
            // Publish validation success event
            validationEventProducer.publishValidationSuccessEvent(
                savedResult.getOrderId(),
                savedResult.getValidationStatus(),
                savedResult.getValidationMessage()
            );
            logger.info("Published validation success event for order ID: {}", savedResult.getOrderId());
        } else {
            // Publish validation failure event
            validationEventProducer.publishValidationFailedEvent(
                savedResult.getOrderId(),
                savedResult.getValidationStatus(),
                savedResult.getValidationMessage()
            );
            logger.error("Published validation failure event for order ID: {}", savedResult.getOrderId());
        }
        
        return savedResult;
    }
    
    /**
     * Retrieve all validations
     */
    public List<ValidationEntity> getAllValidations() {
        return validationRepository.findAll();
    }
    
    /**
     * Retrieve validations by order ID
     */
    public List<ValidationEntity> getValidationsByOrderId(Long orderId) {
        return validationRepository.findByOrderId(orderId);
    }
    
    /**
     * Retrieve validations by status
     */
    public List<ValidationEntity> getValidationsByStatus(String status) {
        return validationRepository.findByValidationStatus(status);
    }
}
