package com.project.fulfillment.repository;

import com.project.fulfillment.entity.FailedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEventEntity, Long> {
    
    /**
     * Find all failed events ordered by failed time descending
     */
    List<FailedEventEntity> findAllByOrderByFailedAtDesc();
    
    /**
     * Find failed events by service name
     */
    List<FailedEventEntity> findByServiceNameOrderByFailedAtDesc(String serviceName);
    
    /**
     * Find failed events by event type
     */
    List<FailedEventEntity> findByEventTypeOrderByFailedAtDesc(String eventType);
}
