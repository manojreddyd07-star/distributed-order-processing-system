package com.project.validation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dto.ReplayRequest;
import com.project.common.dto.ReplayResponse;
import com.project.common.service.BaseEventReplayService;
import com.project.validation.entity.FailedEventEntity;
import com.project.validation.repository.FailedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EventReplayService extends BaseEventReplayService<FailedEventEntity, FailedEventRepository> {
    
    private static final Logger log = LoggerFactory.getLogger(EventReplayService.class);
    private final ObjectMapper objectMapper;
    
    @Autowired
    public EventReplayService(FailedEventRepository failedEventRepository,
                             KafkaTemplate<String, Object> kafkaTemplate,
                             ObjectMapper objectMapper) {
        super(failedEventRepository, kafkaTemplate);
        this.objectMapper = objectMapper;
    }
    
    @Override
    protected Optional<FailedEventEntity> findByEventId(String eventId) {
        return failedEventRepository.findByEventId(eventId);
    }
    
    @Override
    protected String getEventType(FailedEventEntity failedEvent) {
        return failedEvent.getEventType();
    }
    
    @Override
    protected String getPayload(FailedEventEntity failedEvent) {
        return failedEvent.getPayload();
    }
    
    /**
     * Replay a failed event by republishing it to the specified Kafka topic
     */
}
