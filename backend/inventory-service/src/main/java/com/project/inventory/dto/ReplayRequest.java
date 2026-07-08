package com.project.inventory.dto;

public class ReplayRequest {
    
    private String eventId;
    private String eventType;
    private String replayTopic;
    
    // Constructors
    public ReplayRequest() {
    }
    
    public ReplayRequest(String eventId, String eventType, String replayTopic) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.replayTopic = replayTopic;
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
    
    public String getReplayTopic() {
        return replayTopic;
    }
    
    public void setReplayTopic(String replayTopic) {
        this.replayTopic = replayTopic;
    }
    
    @Override
    public String toString() {
        return "ReplayRequest{" +
                "eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", replayTopic='" + replayTopic + '\'' +
                '}';
    }
}
