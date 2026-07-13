package com.project.monitoring.repository;

import com.project.monitoring.entity.EventMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventMetricRepository extends JpaRepository<EventMetric, Long> {
    
    /**
     * Count events by status within a time range
     */
    long countByStatusAndTimestampBetween(String status, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Count all events within a time range
     */
    long countByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get average processing time for successful events
     */
    @Query("SELECT AVG(e.processingTimeMs) FROM EventMetric e WHERE e.status = 'SUCCESS' AND e.timestamp BETWEEN :startTime AND :endTime")
    Double getAverageProcessingTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get events by service name and time range
     */
    List<EventMetric> findByServiceNameAndTimestampBetween(String serviceName, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get events by event type and time range
     */
    List<EventMetric> findByEventTypeAndTimestampBetween(String eventType, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Get all events within a time range
     */
    List<EventMetric> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Delete events older than specified time
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
