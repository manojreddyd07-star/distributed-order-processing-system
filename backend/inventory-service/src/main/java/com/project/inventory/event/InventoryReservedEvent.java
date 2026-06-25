package com.project.inventory.event;

import java.time.LocalDateTime;

public class InventoryReservedEvent {
    
    // Event metadata
    private String eventId;
    private String eventType;
    private LocalDateTime eventTimestamp;
    
    // Inventory data
    private String productId;
    private Long orderId;
    private Integer availableQuantity;
    private Integer reservedQuantity;
    private String inventoryStatus;

    // Default constructor
    public InventoryReservedEvent() {
    }

    // All-args constructor
    public InventoryReservedEvent(String eventId, String eventType, LocalDateTime eventTimestamp,
                                  String productId, Long orderId, Integer availableQuantity,
                                  Integer reservedQuantity, String inventoryStatus) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.eventTimestamp = eventTimestamp;
        this.productId = productId;
        this.orderId = orderId;
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.inventoryStatus = inventoryStatus;
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

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getInventoryStatus() {
        return inventoryStatus;
    }

    public void setInventoryStatus(String inventoryStatus) {
        this.inventoryStatus = inventoryStatus;
    }

    @Override
    public String toString() {
        return "InventoryReservedEvent{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", eventTimestamp=" + eventTimestamp +
                ", productId='" + productId + '\'' +
                ", orderId=" + orderId +
                ", availableQuantity=" + availableQuantity +
                ", reservedQuantity=" + reservedQuantity +
                ", inventoryStatus='" + inventoryStatus + '\'' +
                '}';
    }
}
