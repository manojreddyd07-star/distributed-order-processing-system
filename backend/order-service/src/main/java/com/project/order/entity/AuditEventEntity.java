package com.project.order.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events", 
       indexes = {
           @Index(name = "idx_event_id", columnList = "event_id"),
           @Index(name = "idx_order_id", columnList = "order_id"),
           @Index(name = "idx_event_type", columnList = "event_type"),
           @Index(name = "idx_service_name", columnList = "service_name"),
           @Index(name = "idx_created_at", columnList = "created_at")
       })
public class AuditEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    
    @Column(name = "status", nullable = false, length = 50)
    private String status;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public AuditEventEntity() {
    }
    
    public AuditEventEntity(String eventId, String eventType, String serviceName, Long orderId, String status, String message) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.orderId = orderId;
        this.status = status;
        this.message = message;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "AuditEventEntity{" +
                "id=" + id +
                ", eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", orderId=" + orderId +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
