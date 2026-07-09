package com.project.order.repository;

import com.project.order.entity.AuditEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditEventEntity, Long> {
    
    /**
     * Find audit events by order ID
     */
    List<AuditEventEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * Find audit events by order ID with pagination
     */
    Page<AuditEventEntity> findByOrderIdOrderByCreatedAtDesc(Long orderId, Pageable pageable);
    
    /**
     * Find audit events by event type
     */
    Page<AuditEventEntity> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);
    
    /**
     * Find audit events by service name
     */
    Page<AuditEventEntity> findByServiceNameOrderByCreatedAtDesc(String serviceName, Pageable pageable);
    
    /**
     * Find audit events by status
     */
    Page<AuditEventEntity> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    /**
     * Find all audit events with pagination
     */
    Page<AuditEventEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find audit events by event type and service name
     */
    @Query("SELECT a FROM AuditEventEntity a WHERE " +
           "(:eventType IS NULL OR a.eventType = :eventType) AND " +
           "(:serviceName IS NULL OR a.serviceName = :serviceName) AND " +
           "(:status IS NULL OR a.status = :status) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditEventEntity> findByFilters(
        @Param("eventType") String eventType,
        @Param("serviceName") String serviceName,
        @Param("status") String status,
        Pageable pageable
    );
    
    /**
     * Count audit events by service name
     */
    long countByServiceName(String serviceName);
    
    /**
     * Count audit events by event type
     */
    long countByEventType(String eventType);
    
    /**
     * Find audit events within a time range
     */
    List<AuditEventEntity> findByCreatedAtBetweenOrderByCreatedAtDesc(
        LocalDateTime startTime,
        LocalDateTime endTime
    );
}
