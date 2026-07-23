package com.project.payment.service;

import com.project.common.dto.ReplayRequest;
import com.project.common.dto.ReplayResponse;
import com.project.common.service.BaseEventReplayService;
import com.project.payment.entity.FailedEventEntity;
import com.project.payment.repository.FailedEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EventReplayService extends BaseEventReplayService<FailedEventEntity, FailedEventRepository> {
    
    @Autowired
    public EventReplayService(FailedEventRepository failedEventRepository,
                             KafkaTemplate<String, Object> kafkaTemplate) {
        super(failedEventRepository, kafkaTemplate);
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
}
