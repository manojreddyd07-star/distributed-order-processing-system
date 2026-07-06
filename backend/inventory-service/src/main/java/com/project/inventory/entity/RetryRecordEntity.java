package com.project.inventory.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "retry_records",
       indexes = {
           @Index(name = "idx_retry_id", columnList = "retry_id", unique = true),
           @Index(name = "idx_original_event_id", columnList = "original_event_id"),
           @Index(name = "idx_retry_status", columnList = "retry_status"),
           @Index(name = "idx_next_retry_time", columnList = "next_retry_time")
       })
public class RetryRecordEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "retry_id", nullable = false, unique = true, length = 255)
    private String retryId;
    
    @Column(name = "original_event_id", nullable = false, length = 255)
    private String originalEventId;
    
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;
    
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    @Column(name = "retry_status", nullable = false, length = 50)
    private String retryStatus;
    
    @Column(name = "last_retry_time", nullable = false)
    private LocalDateTime lastRetryTime;
    
    @Column(name = "next_retry_time")
    private LocalDateTime nextRetryTime;
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(name = "event_payload", nullable = false, columnDefinition = "TEXT")
    private String eventPayload;
    
    @Column(name = "event_class", nullable = false, length = 255)
    private String eventClass;
    
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;
    
    @Column(name = "target_topic", nullable = false, length = 100)
    private String targetTopic;
    
    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public RetryRecordEntity() {
    }
    
    public RetryRecordEntity(String retryId, String originalEventId, String eventType,
                            Integer retryCount, String retryStatus, LocalDateTime lastRetryTime,
                            LocalDateTime nextRetryTime, String failureReason, String eventPayload,
                            String eventClass, String serviceName, String targetTopic, Integer maxRetries) {
        this.retryId = retryId;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.retryCount = retryCount;
        this.retryStatus = retryStatus;
        this.lastRetryTime = lastRetryTime;
        this.nextRetryTime = nextRetryTime;
        this.failureReason = failureReason;
        this.eventPayload = eventPayload;
        this.eventClass = eventClass;
        this.serviceName = serviceName;
        this.targetTopic = targetTopic;
        this.maxRetries = maxRetries;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRetryId() {
        return retryId;
    }

    public void setRetryId(String retryId) {
        this.retryId = retryId;
    }

    public String getOriginalEventId() {
        return originalEventId;
    }

    public void setOriginalEventId(String originalEventId) {
        this.originalEventId = originalEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getRetryStatus() {
        return retryStatus;
    }

    public void setRetryStatus(String retryStatus) {
        this.retryStatus = retryStatus;
    }

    public LocalDateTime getLastRetryTime() {
        return lastRetryTime;
    }

    public void setLastRetryTime(LocalDateTime lastRetryTime) {
        this.lastRetryTime = lastRetryTime;
    }

    public LocalDateTime getNextRetryTime() {
        return nextRetryTime;
    }

    public void setNextRetryTime(LocalDateTime nextRetryTime) {
        this.nextRetryTime = nextRetryTime;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getEventPayload() {
        return eventPayload;
    }

    public void setEventPayload(String eventPayload) {
        this.eventPayload = eventPayload;
    }

    public String getEventClass() {
        return eventClass;
    }

    public void setEventClass(String eventClass) {
        this.eventClass = eventClass;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getTargetTopic() {
        return targetTopic;
    }

    public void setTargetTopic(String targetTopic) {
        this.targetTopic = targetTopic;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
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
}
