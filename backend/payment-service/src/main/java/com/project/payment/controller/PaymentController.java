package com.project.payment.controller;

import com.project.payment.entity.PaymentEntity;
import com.project.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    private final PaymentService paymentService;
    
    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    /**
     * Get all payment records
     * @return List of all payments
     */
    @GetMapping
    public ResponseEntity<List<PaymentEntity>> getAllPayments() {
        logger.info("REST API: Fetching all payment records");
        try {
            List<PaymentEntity> payments = paymentService.getAllPayments();
            logger.info("REST API: Successfully fetched {} payment records", payments.size());
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("REST API: Error fetching all payments: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payment by payment ID
     * @param paymentId The payment ID
     * @return PaymentEntity
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentEntity> getPaymentByPaymentId(@PathVariable String paymentId) {
        logger.info("REST API: Fetching payment with ID: {}", paymentId);
        try {
            PaymentEntity payment = paymentService.getPaymentByPaymentId(paymentId);
            logger.info("REST API: Successfully fetched payment: {}", paymentId);
            return ResponseEntity.ok(payment);
        } catch (RuntimeException e) {
            logger.error("REST API: Payment not found: {}", paymentId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("REST API: Error fetching payment {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payments by order ID
     * @param orderId The order ID
     * @return List of payments for the order
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentEntity>> getPaymentsByOrderId(@PathVariable Long orderId) {
        logger.info("REST API: Fetching payments for order ID: {}", orderId);
        try {
            List<PaymentEntity> payments = paymentService.getPaymentsByOrderId(orderId);
            logger.info("REST API: Successfully fetched {} payment(s) for order ID: {}", 
                       payments.size(), orderId);
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            logger.error("REST API: Error fetching payments for order {}: {}", 
                        orderId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
