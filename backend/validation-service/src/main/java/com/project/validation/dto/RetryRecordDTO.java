package com.project.validation.dto;

import java.time.LocalDateTime;

public class RetryRecordDTO {
    
    private Long id;
    private String retryId;
    private String originalEventId;
    private String eventType;
    private Integer retryCount;
    private String retryStatus;
    private LocalDateTime lastRetryTime;
    private LocalDateTime nextRetryTime;
    private String failureReason;
    private String serviceName;
    private String targetTopic;
    private Integer maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Constructors
    public RetryRecordDTO() {
    }
    
    public RetryRecordDTO(Long id, String retryId, String originalEventId, String eventType,
                         Integer retryCount, String retryStatus, LocalDateTime lastRetryTime,
                         LocalDateTime nextRetryTime, String failureReason, String serviceName,
                         String targetTopic, Integer maxRetries, LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
        this.id = id;
        this.retryId = retryId;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.retryCount = retryCount;
        this.retryStatus = retryStatus;
        this.lastRetryTime = lastRetryTime;
        this.nextRetryTime = nextRetryTime;
        this.failureReason = failureReason;
        this.serviceName = serviceName;
        this.targetTopic = targetTopic;
        this.maxRetries = maxRetries;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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
