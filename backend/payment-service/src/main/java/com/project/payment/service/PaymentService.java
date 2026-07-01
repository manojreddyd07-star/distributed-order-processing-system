package com.project.payment.service;

import com.project.payment.entity.PaymentEntity;
import com.project.common.events.OrderValidatedEvent;
import com.project.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private static final String DEFAULT_PAYMENT_STATUS = "PENDING";
    
    private final PaymentRepository paymentRepository;
    
    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    /**
     * Process a payment for a validated order
     * @param event The OrderValidatedEvent containing order details
     * @return PaymentEntity representing the processed payment
     */
    public PaymentEntity processPayment(OrderValidatedEvent event) {
        logger.info("Processing payment for order ID: {}", event.getOrderId());
        
        // Generate unique payment ID
        String paymentId = "PMT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Extract amount from the event
        BigDecimal amount = event.getAmount();
        
        // Create payment entity with default status
        PaymentEntity payment = new PaymentEntity(
            paymentId,
            event.getOrderId(),
            amount,
            DEFAULT_PAYMENT_STATUS
        );
        
        // Save the payment
        PaymentEntity savedPayment = savePayment(payment);
        
        logger.info("Payment processed successfully - Payment ID: {}, Order ID: {}, Amount: {}, Status: {}",
                   savedPayment.getPaymentId(), 
                   savedPayment.getOrderId(), 
                   savedPayment.getAmount(), 
                   savedPayment.getPaymentStatus());
        
        return savedPayment;
    }
    
    /**
     * Save a payment record to the database
     * @param payment The payment entity to save
     * @return Saved PaymentEntity
     */
    public PaymentEntity savePayment(PaymentEntity payment) {
        logger.info("Persisting payment record - Payment ID: {}, Order ID: {}", 
                   payment.getPaymentId(), payment.getOrderId());
        
        PaymentEntity savedPayment = paymentRepository.save(payment);
        
        logger.info("Payment record persisted successfully - ID: {}", savedPayment.getId());
        
        return savedPayment;
    }
    
    /**
     * Get all payment records
     * @return List of all payments
     */
    public List<PaymentEntity> getAllPayments() {
        logger.info("Fetching all payment records");
        return paymentRepository.findAll();
    }
    
    /**
     * Get payment by payment ID
     * @param paymentId The payment ID
     * @return PaymentEntity if found
     */
    public PaymentEntity getPaymentByPaymentId(String paymentId) {
        logger.info("Fetching payment by payment ID: {}", paymentId);
        return paymentRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + paymentId));
    }
    
    /**
     * Get payments by order ID
     * @param orderId The order ID
     * @return List of payments for the order
     */
    public List<PaymentEntity> getPaymentsByOrderId(Long orderId) {
        logger.info("Fetching payments for order ID: {}", orderId);
        return paymentRepository.findByOrderId(orderId);
    }
}
