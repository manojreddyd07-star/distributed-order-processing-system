package com.project.validation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validations")
public class ValidationEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus;
    
    @Column(name = "validation_message", length = 500)
    private String validationMessage;
    
    @Column(name = "validated_at", nullable = false, updatable = false)
    private LocalDateTime validatedAt;
    
    @PrePersist
    protected void onCreate() {
        validatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public ValidationEntity() {
    }
    
    public ValidationEntity(Long orderId, String validationStatus, String validationMessage) {
        this.orderId = orderId;
        this.validationStatus = validationStatus;
        this.validationMessage = validationMessage;
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
