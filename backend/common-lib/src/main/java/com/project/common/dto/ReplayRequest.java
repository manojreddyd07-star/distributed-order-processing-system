package com.project.common.dto;

/**
 * Shared DTO for replay request across all services
 */
public class ReplayRequest {
    private String eventId;
    private String replayTopic;
    
    public ReplayRequest() {}
    
    public ReplayRequest(String eventId, String replayTopic) {
        this.eventId = eventId;
        this.replayTopic = replayTopic;
    }
    
    public String getEventId() {
        return eventId;
    }
    
    public void setEventId(String eventId) {
        this.eventId = eventId;
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
                ", replayTopic='" + replayTopic + '\'' +
                '}';
    }
}
