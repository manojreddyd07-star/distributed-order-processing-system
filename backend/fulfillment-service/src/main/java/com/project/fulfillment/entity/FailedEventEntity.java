package com.project.fulfillment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "failed_events",
       indexes = {
           @Index(name = "idx_failed_event_id", columnList = "event_id"),
           @Index(name = "idx_failed_event_type", columnList = "event_type"),
           @Index(name = "idx_failed_service_name", columnList = "service_name"),
           @Index(name = "idx_failed_at", columnList = "failed_at")
       })
public class FailedEventEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, length = 255)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(name = "failed_at", nullable = false, updatable = false)
    private LocalDateTime failedAt;
    
    @PrePersist
    protected void onCreate() {
        failedAt = LocalDateTime.now();
    }
    
    // Constructors
    public FailedEventEntity() {
    }
    
    public FailedEventEntity(String eventId, String eventType, String serviceName,
                           String errorMessage, String payload) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.errorMessage = errorMessage;
        this.payload = payload;
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
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getPayload() {
        return payload;
    }
    
    public void setPayload(String payload) {
        this.payload = payload;
    }
    
    public LocalDateTime getFailedAt() {
        return failedAt;
    }
    
    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }
}
