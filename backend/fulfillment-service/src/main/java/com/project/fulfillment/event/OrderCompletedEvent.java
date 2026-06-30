package com.project.fulfillment.event;

import java.time.LocalDateTime;

public class OrderCompletedEvent {
    
    // Event metadata
    private String eventId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    
    // Order and fulfillment data
    private Long orderId;
    private String customerId;
    private Long fulfillmentId;
    private String trackingNumber;
    private String fulfillmentStatus;

    // Default constructor
    public OrderCompletedEvent() {
    }

    // All-args constructor
    public OrderCompletedEvent(String eventId, String eventType, LocalDateTime eventTimestamp,
                               Long orderId, String customerId, Long fulfillmentId,
                               String trackingNumber, String fulfillmentStatus) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.orderId = orderId;
        this.customerId = customerId;
        this.fulfillmentId = fulfillmentId;
        this.trackingNumber = trackingNumber;
        this.fulfillmentStatus = fulfillmentStatus;
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

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Long getFulfillmentId() {
        return fulfillmentId;
    }

    public void setFulfillmentId(Long fulfillmentId) {
        this.fulfillmentId = fulfillmentId;
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

    @Override
    public String toString() {
        return "OrderCompletedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", orderId=" + orderId +
                ", customerId='" + customerId + '\'' +
                ", fulfillmentId=" + fulfillmentId +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", fulfillmentStatus='" + fulfillmentStatus + '\'' +
                '}';
    }
}
