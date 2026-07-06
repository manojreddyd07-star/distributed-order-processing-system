package com.project.common.events;

import java.time.LocalDateTime;

/**
 * Event model for retry mechanism
 * Wraps failed events for retry processing
 */
public class RetryEvent {
    
    // Retry metadata
    private String retryId;
    private String originalEventId;
    private String eventType;
    private int retryCount;
    private LocalDateTime lastRetryTime;
    private LocalDateTime nextRetryTime;
    private String failureReason;
    
    // Original event payload (as JSON string)
    private String eventPayload;
    private String eventClass;
    
    // Service information
    private String serviceName;
    private String targetTopic;
    
    // Retry configuration
    private int maxRetries;
    
    // Default constructor
    public RetryEvent() {
    }

    // All-args constructor
    public RetryEvent(String retryId, String originalEventId, String eventType,
                      int retryCount, LocalDateTime lastRetryTime, LocalDateTime nextRetryTime,
                      String failureReason, String eventPayload, String eventClass,
                      String serviceName, String targetTopic, int maxRetries) {
        this.retryId = retryId;
        this.originalEventId = originalEventId;
        this.eventType = eventType;
        this.retryCount = retryCount;
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

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
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

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    @Override
    public String toString() {
        return "RetryEvent{" +
                "retryId='" + retryId + '\'' +
                ", originalEventId='" + originalEventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", retryCount=" + retryCount +
                ", lastRetryTime=" + lastRetryTime +
                ", nextRetryTime=" + nextRetryTime +
                ", failureReason='" + failureReason + '\'' +
                ", eventClass='" + eventClass + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", targetTopic='" + targetTopic + '\'' +
                ", maxRetries=" + maxRetries +
                '}';
    }
}
