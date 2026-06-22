package com.project.payment.repository;

import com.project.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    
    /**
     * Find a payment by payment ID
     * @param paymentId The payment ID
     * @return Optional containing the payment if found
     */
    Optional<PaymentEntity> findByPaymentId(String paymentId);
    
    /**
     * Find all payments for a specific order
     * @param orderId The order ID
     * @return List of payments for the order
     */
    List<PaymentEntity> findByOrderId(Long orderId);
    
    /**
     * Find all payments by status
     * @param paymentStatus The payment status (e.g., "PENDING", "COMPLETED", "FAILED")
     * @return List of payments with the specified status
     */
    List<PaymentEntity> findByPaymentStatus(String paymentStatus);
}
