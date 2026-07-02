package com.project.validation.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_records", 
       indexes = {@Index(name = "idx_event_id", columnList = "event_id", unique = true)})
public class IdempotencyRecordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "event_id", nullable = false, unique = true, length = 255)
    private String eventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "processing_status", nullable = false, length = 50)
    private String processingStatus;
    
    @Column(name = "processed_at", nullable = false, updatable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
    
    // Constructors
    public IdempotencyRecordEntity() {
    }
    
    public IdempotencyRecordEntity(String eventId, String eventType, String serviceName, String processingStatus) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.processingStatus = processingStatus;
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
    
    public String getProcessingStatus() {
        return processingStatus;
    }
    
    public void setProcessingStatus(String processingStatus) {
        this.processingStatus = processingStatus;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
