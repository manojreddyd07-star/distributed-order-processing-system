package com.project.inventory.dto;

import java.time.LocalDateTime;

public class ReplayResponse {
    
    private boolean success;
    private String message;
    private String eventId;
    private String eventType;
    private String replayTopic;
    private LocalDateTime replayedAt;
    
    // Constructors
    public ReplayResponse() {
    }
    
    public ReplayResponse(boolean success, String message, String eventId, 
                         String eventType, String replayTopic, LocalDateTime replayedAt) {
        this.success = success;
        this.message = message;
        this.eventId = eventId;
        this.eventType = eventType;
        this.replayTopic = replayTopic;
        this.replayedAt = replayedAt;
    }
    
    // Static factory methods for common responses
    public static ReplayResponse success(String eventId, String eventType, String replayTopic) {
        return new ReplayResponse(
            true,
            "Event replayed successfully",
            eventId,
            eventType,
            replayTopic,
            LocalDateTime.now()
        );
    }
    
    public static ReplayResponse error(String message) {
        return new ReplayResponse(
            false,
            message,
            null,
            null,
            null,
            LocalDateTime.now()
        );
    }
    
    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
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
    
    public String getReplayTopic() {
        return replayTopic;
    }
    
    public void setReplayTopic(String replayTopic) {
        this.replayTopic = replayTopic;
    }
    
    public LocalDateTime getReplayedAt() {
        return replayedAt;
    }
    
    public void setReplayedAt(LocalDateTime replayedAt) {
        this.replayedAt = replayedAt;
    }
    
    @Override
    public String toString() {
        return "ReplayResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", replayTopic='" + replayTopic + '\'' +
                ", replayedAt=" + replayedAt +
                '}';
    }
}
