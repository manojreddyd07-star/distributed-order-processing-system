package com.project.fulfillment.repository;

import com.project.fulfillment.entity.FulfillmentAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FulfillmentAuditLogRepository extends JpaRepository<FulfillmentAuditLog, Long> {
    
    /**
     * Find all audit logs for a specific fulfillment
     * @param fulfillmentId The fulfillment ID
     * @return List of audit logs
     */
    List<FulfillmentAuditLog> findByFulfillmentId(Long fulfillmentId);
    
    /**
     * Find all audit logs for a specific order
     * @param orderId The order ID
     * @return List of audit logs
     */
    List<FulfillmentAuditLog> findByOrderId(Long orderId);
    
    /**
     * Find all audit logs for a specific customer
     * @param customerId The customer ID
     * @return List of audit logs
     */
    List<FulfillmentAuditLog> findByCustomerId(String customerId);
    
    /**
     * Find all audit logs ordered by creation date (most recent first)
     * @return List of audit logs
     */
    List<FulfillmentAuditLog> findAllByOrderByCreatedAtDesc();
}
