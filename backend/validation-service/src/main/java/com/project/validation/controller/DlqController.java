package com.project.validation.controller;

import com.project.validation.entity.FailedEventEntity;
import com.project.validation.service.FailedEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dlq")
@CrossOrigin(origins = "*")
public class DlqController {
    
    private final FailedEventService failedEventService;
    
    @Autowired
    public DlqController(FailedEventService failedEventService) {
        this.failedEventService = failedEventService;
    }
    
    /**
     * Get all failed events
     */
    @GetMapping
    public ResponseEntity<List<FailedEventEntity>> getAllFailedEvents() {
        try {
            List<FailedEventEntity> failedEvents = failedEventService.getAllFailedEvents();
            return ResponseEntity.ok(failedEvents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Get failed events by service name
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<FailedEventEntity>> getFailedEventsByService(@PathVariable String serviceName) {
        try {
            List<FailedEventEntity> failedEvents = failedEventService.getFailedEventsByService(serviceName);
            return ResponseEntity.ok(failedEvents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
