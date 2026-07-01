package com.project.common.events;

import java.time.LocalDateTime;

public class OrderValidatedEvent {
    
    // Event metadata
    private String eventId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    
    // Validation data
    private Long orderId;
    private String validationStatus;
    private String validationMessage;

    // Default constructor
    public OrderValidatedEvent() {
    }

    // All-args constructor
    public OrderValidatedEvent(String eventId, String eventType, LocalDateTime eventTimestamp,
                               Long orderId, String validationStatus, String validationMessage) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.orderId = orderId;
        this.validationStatus = validationStatus;
        this.validationMessage = validationMessage;
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

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
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

    @Override
    public String toString() {
        return "OrderValidatedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", orderId=" + orderId +
                ", validationStatus='" + validationStatus + '\'' +
                ", validationMessage='" + validationMessage + '\'' +
                '}';
    }
}
