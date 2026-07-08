package com.project.validation.controller;

import com.project.validation.dto.ReplayRequest;
import com.project.validation.dto.ReplayResponse;
import com.project.validation.service.EventReplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/replay")
@CrossOrigin(origins = "*")
public class ReplayController {
    
    private static final Logger logger = LoggerFactory.getLogger(ReplayController.class);
    
    private final EventReplayService eventReplayService;
    
    @Autowired
    public ReplayController(EventReplayService eventReplayService) {
        this.eventReplayService = eventReplayService;
    }
    
    /**
     * Replay a failed event
     * POST /api/replay
     */
    @PostMapping
    public ResponseEntity<ReplayResponse> replayEvent(@RequestBody ReplayRequest request) {
        logger.info("POST /api/replay - Replaying event: {}", request);
        
        try {
            ReplayResponse response = eventReplayService.replayEvent(request);
            
            if (response.isSuccess()) {
                logger.info("Event replayed successfully - EventID: {}", request.getEventId());
                return ResponseEntity.ok(response);
            } else {
                logger.error("Event replay failed - EventID: {}, Message: {}", 
                           request.getEventId(), response.getMessage());
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (Exception e) {
            logger.error("Error replaying event: {}", e.getMessage(), e);
            ReplayResponse errorResponse = ReplayResponse.error("Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
