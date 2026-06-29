package com.project.fulfillment.repository;

import com.project.fulfillment.entity.FulfillmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FulfillmentRepository extends JpaRepository<FulfillmentEntity, Long> {
    
    /**
     * Find fulfillment by order ID
     * @param orderId The order ID
     * @return Optional containing the fulfillment if found
     */
    Optional<FulfillmentEntity> findByOrderId(Long orderId);
    
    /**
     * Find all fulfillments by customer ID
     * @param customerId The customer ID
     * @return List of fulfillments for the customer
     */
    List<FulfillmentEntity> findByCustomerId(String customerId);
    
    /**
     * Find all fulfillments by status
     * @param fulfillmentStatus The fulfillment status
     * @return List of fulfillments with the specified status
     */
    List<FulfillmentEntity> findByFulfillmentStatus(String fulfillmentStatus);
    
    /**
     * Find fulfillment by tracking number
     * @param trackingNumber The tracking number
     * @return Optional containing the fulfillment if found
     */
    Optional<FulfillmentEntity> findByTrackingNumber(String trackingNumber);
    
    /**
     * Check if fulfillment exists for an order
     * @param orderId The order ID
     * @return true if fulfillment exists, false otherwise
     */
    boolean existsByOrderId(Long orderId);
}
