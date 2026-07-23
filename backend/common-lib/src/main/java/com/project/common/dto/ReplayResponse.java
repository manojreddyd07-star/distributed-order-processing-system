package com.project.common.dto;

/**
 * Shared DTO for replay response across all services
 */
public class ReplayResponse {
    private boolean success;
    private String message;
    private String eventId;
    private String eventType;
    private String replayTopic;
    
    private ReplayResponse() {}
    
    public static ReplayResponse success(String eventId, String eventType, String replayTopic) {
        ReplayResponse response = new ReplayResponse();
        response.success = true;
        response.message = "Event replayed successfully";
        response.eventId = eventId;
        response.eventType = eventType;
        response.replayTopic = replayTopic;
        return response;
    }
    
    public static ReplayResponse error(String message) {
        ReplayResponse response = new ReplayResponse();
        response.success = false;
        response.message = message;
        return response;
    }
    
    // Getters
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public String getEventType() {
        return eventType;
    }
    
    public String getReplayTopic() {
        return replayTopic;
    }
}
