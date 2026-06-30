package com.project.fulfillment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fulfillment_audit_log")
public class FulfillmentAuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;
    
    @Column(name = "fulfillment_id", nullable = false)
    private Long fulfillmentId;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "tracking_number", nullable = false)
    private String trackingNumber;
    
    @Column(name = "fulfillment_status", nullable = false)
    private String fulfillmentStatus;
    
    @Column(name = "action", nullable = false)
    private String action;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    // Default constructor
    public FulfillmentAuditLog() {
    }
    
    // Constructor with essential fields
    public FulfillmentAuditLog(Long fulfillmentId, Long orderId, String customerId,
                               String trackingNumber, String fulfillmentStatus,
                               String action, String description) {
        this.fulfillmentId = fulfillmentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.trackingNumber = trackingNumber;
        this.fulfillmentStatus = fulfillmentStatus;
        this.action = action;
        this.description = description;
    }
    
    // Lifecycle callback
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getAuditId() {
        return auditId;
    }
    
    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }
    
    public Long getFulfillmentId() {
        return fulfillmentId;
    }
    
    public void setFulfillmentId(Long fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
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
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }
    
    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "FulfillmentAuditLog{" +
                "auditId=" + auditId +
                ", fulfillmentId=" + fulfillmentId +
                ", orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", fulfillmentStatus='" + fulfillmentStatus + '\'' +
                ", action='" + action + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
