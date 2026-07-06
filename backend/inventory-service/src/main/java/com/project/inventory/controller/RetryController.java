package com.project.inventory.controller;

import com.project.inventory.dto.RetryRecordDTO;
import com.project.inventory.entity.RetryRecordEntity;
import com.project.inventory.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/retry")
@CrossOrigin(origins = "*")
public class RetryController {
    
    private static final Logger logger = LoggerFactory.getLogger(RetryController.class);
    
    private final RetryService retryService;
    
    @Autowired
    public RetryController(RetryService retryService) {
        this.retryService = retryService;
    }
    
    @GetMapping
    public ResponseEntity<List<RetryRecordDTO>> getAllRetryRecords() {
        logger.info("GET /retry - Fetching all retry records");
        
        try {
            List<RetryRecordEntity> retryRecords = retryService.getAllRetryRecords();
            List<RetryRecordDTO> dtos = retryRecords.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            logger.info("Successfully retrieved {} retry records", dtos.size());
            return ResponseEntity.ok(dtos);
            
        } catch (Exception e) {
            logger.error("Error fetching retry records: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    private RetryRecordDTO convertToDTO(RetryRecordEntity entity) {
        return new RetryRecordDTO(
            entity.getId(), entity.getRetryId(), entity.getOriginalEventId(),
            entity.getEventType(), entity.getRetryCount(), entity.getRetryStatus(),
            entity.getLastRetryTime(), entity.getNextRetryTime(), entity.getFailureReason(),
            entity.getServiceName(), entity.getTargetTopic(), entity.getMaxRetries(),
            entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
