package com.project.validation.dto;

import java.time.LocalDateTime;

public class ValidationResponse {
    
    private Long id;
    private Long orderId;
    private String customerId;
    private String validationStatus;
    private String validationMessage;
    private LocalDateTime validatedAt;
    
    // Default constructor
    public ValidationResponse() {
    }
    
    // All-args constructor
    public ValidationResponse(Long id, Long orderId, String customerId, String validationStatus, 
                            String validationMessage, LocalDateTime validatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.customerId = customerId;
        this.validationStatus = validationStatus;
        this.validationMessage = validationMessage;
        this.validatedAt = validatedAt;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
    
    public String getValidationStatus() {
        return validationStatus;
    }
    
    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }
    
    public String getValidationMessage() {
        return validationMessage;
    }
    
    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }
    
    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }
    
    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }
}
