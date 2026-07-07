package com.project.common.events;

import java.time.LocalDateTime;

/**
 * Event model for Dead Letter Queue (DLQ)
 * Represents events that have exhausted all retry attempts
 */
public class FailedEvent {
    
    private String eventId;
    private String eventType;
    private String serviceName;
    private String errorMessage;
    private String payload;
    private LocalDateTime failedAt;
    
    // Default constructor
    public FailedEvent() {
    }
    
    // All-args constructor
    public FailedEvent(String eventId, String eventType, String serviceName,
                      String errorMessage, String payload, LocalDateTime failedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.serviceName = serviceName;
        this.errorMessage = errorMessage;
        this.payload = payload;
        this.failedAt = failedAt;
    }
    
    // Getters and Setters
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
    
    @Override
    public String toString() {
        return "FailedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                ", failedAt=" + failedAt +
                '}';
    }
}
