package com.project.fulfillment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fulfillment")
public class FulfillmentEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long fulfillmentId;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "customer_id", nullable = false)
    private String customerId;
    
    @Column(name = "fulfillment_status", nullable = false)
    private String fulfillmentStatus;
    
    @Column(name = "tracking_number", nullable = false, unique = true)
    private String trackingNumber;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // Default constructor
    public FulfillmentEntity() {
    }
    
    // Constructor with essential fields
    public FulfillmentEntity(Long orderId, String customerId, String fulfillmentStatus, String trackingNumber) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.fulfillmentStatus = fulfillmentStatus;
        this.trackingNumber = trackingNumber;
    }
    
    // All-args constructor
    public FulfillmentEntity(Long fulfillmentId, Long orderId, String customerId, 
                            String fulfillmentStatus, String trackingNumber,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.fulfillmentId = fulfillmentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.fulfillmentStatus = fulfillmentStatus;
        this.trackingNumber = trackingNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
    
    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
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
    
    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }
    
    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }
    
    public String getTrackingNumber() {
        return trackingNumber;
    }
    
    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "FulfillmentEntity{" +
                "fulfillmentId=" + fulfillmentId +
                ", orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                ", fulfillmentStatus='" + fulfillmentStatus + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
