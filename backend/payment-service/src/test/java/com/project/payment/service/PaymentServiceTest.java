package com.project.payment.service;

import com.project.common.events.OrderValidatedEvent;
import com.project.payment.entity.PaymentEntity;
import com.project.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private OrderValidatedEvent validOrderEvent;
    private PaymentEntity savedPaymentEntity;

    @BeforeEach
    void setUp() {
        // Setup valid order event
        validOrderEvent = new OrderValidatedEvent(
                "event-123",
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                1L,
                "CUST-001",
                new BigDecimal("99.99")
        );

        // Setup saved payment entity
        savedPaymentEntity = new PaymentEntity(
                "PMT-ABC123",
                1L,
                new BigDecimal("99.99"),
                "PENDING"
        );
        savedPaymentEntity.setId(1L);
    }

    @Test
    void processPayment_Success_ShouldCreatePayment() {
        // Arrange
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenReturn(savedPaymentEntity);

        // Act
        PaymentEntity result = paymentService.processPayment(validOrderEvent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(result.getPaymentId()).startsWith("PMT-");

        // Verify payment was saved
        ArgumentCaptor<PaymentEntity> paymentCaptor = ArgumentCaptor.forClass(PaymentEntity.class);
        verify(paymentRepository, times(1)).save(paymentCaptor.capture());

        PaymentEntity capturedPayment = paymentCaptor.getValue();
        assertThat(capturedPayment.getOrderId()).isEqualTo(1L);
        assertThat(capturedPayment.getAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(capturedPayment.getPaymentStatus()).isEqualTo("PENDING");
        assertThat(capturedPayment.getPaymentId()).startsWith("PMT-");
    }

    @Test
    void processPayment_WithDifferentAmounts_ShouldCreatePaymentWithCorrectAmount() {
        // Arrange
        OrderValidatedEvent eventWithLargeAmount = new OrderValidatedEvent(
                "event-456",
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                2L,
                "CUST-002",
                new BigDecimal("1500.50")
        );

        PaymentEntity largePayment = new PaymentEntity(
                "PMT-XYZ789",
                2L,
                new BigDecimal("1500.50"),
                "PENDING"
        );

        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenReturn(largePayment);

        // Act
        PaymentEntity result = paymentService.processPayment(eventWithLargeAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("1500.50"));
        assertThat(result.getOrderId()).isEqualTo(2L);
    }

    @Test
    void processPayment_GeneratesUniquePaymentId_ShouldHaveCorrectFormat() {
        // Arrange
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentEntity result1 = paymentService.processPayment(validOrderEvent);
        PaymentEntity result2 = paymentService.processPayment(validOrderEvent);

        // Assert
        assertThat(result1.getPaymentId()).startsWith("PMT-");
        assertThat(result2.getPaymentId()).startsWith("PMT-");
        assertThat(result1.getPaymentId()).isNotEqualTo(result2.getPaymentId());
        assertThat(result1.getPaymentId()).hasSize(12); // PMT- + 8 chars
        assertThat(result2.getPaymentId()).hasSize(12);
    }

    @Test
    void savePayment_Success_ShouldSaveAndReturnPayment() {
        // Arrange
        PaymentEntity paymentToSave = new PaymentEntity(
                "PMT-TEST01",
                1L,
                new BigDecimal("50.00"),
                "PENDING"
        );

        when(paymentRepository.save(paymentToSave))
                .thenReturn(paymentToSave);

        // Act
        PaymentEntity result = paymentService.savePayment(paymentToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("PMT-TEST01");
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("50.00"));

        verify(paymentRepository, times(1)).save(paymentToSave);
    }

    @Test
    void getAllPayments_Success_ShouldReturnAllPayments() {
        // Arrange
        PaymentEntity payment1 = new PaymentEntity("PMT-001", 1L, new BigDecimal("100.00"), "PENDING");
        PaymentEntity payment2 = new PaymentEntity("PMT-002", 2L, new BigDecimal("200.00"), "COMPLETED");
        PaymentEntity payment3 = new PaymentEntity("PMT-003", 3L, new BigDecimal("300.00"), "FAILED");

        when(paymentRepository.findAll()).thenReturn(Arrays.asList(payment1, payment2, payment3));

        // Act
        List<PaymentEntity> result = paymentService.getAllPayments();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getPaymentId()).isEqualTo("PMT-001");
        assertThat(result.get(1).getPaymentId()).isEqualTo("PMT-002");
        assertThat(result.get(2).getPaymentId()).isEqualTo("PMT-003");

        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void getAllPayments_EmptyRepository_ShouldReturnEmptyList() {
        // Arrange
        when(paymentRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<PaymentEntity> result = paymentService.getAllPayments();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void getPaymentByPaymentId_Success_ShouldReturnPayment() {
        // Arrange
        String paymentId = "PMT-ABC123";
        when(paymentRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.of(savedPaymentEntity));

        // Act
        PaymentEntity result = paymentService.getPaymentByPaymentId(paymentId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getPaymentId()).isEqualTo("PMT-ABC123");
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualTo(new BigDecimal("99.99"));

        verify(paymentRepository, times(1)).findByPaymentId(paymentId);
    }

    @Test
    void getPaymentByPaymentId_NotFound_ShouldThrowException() {
        // Arrange
        String paymentId = "PMT-NOTFOUND";
        when(paymentRepository.findByPaymentId(paymentId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> paymentService.getPaymentByPaymentId(paymentId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Payment not found with ID: PMT-NOTFOUND");

        verify(paymentRepository, times(1)).findByPaymentId(paymentId);
    }

    @Test
    void getPaymentsByOrderId_Success_ShouldReturnPayments() {
        // Arrange
        Long orderId = 1L;
        PaymentEntity payment1 = new PaymentEntity("PMT-001", orderId, new BigDecimal("100.00"), "PENDING");
        PaymentEntity payment2 = new PaymentEntity("PMT-002", orderId, new BigDecimal("50.00"), "COMPLETED");

        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Arrays.asList(payment1, payment2));

        // Act
        List<PaymentEntity> result = paymentService.getPaymentsByOrderId(orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(result.get(1).getOrderId()).isEqualTo(orderId);
        assertThat(result.get(0).getPaymentId()).isEqualTo("PMT-001");
        assertThat(result.get(1).getPaymentId()).isEqualTo("PMT-002");

        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getPaymentsByOrderId_NoPayments_ShouldReturnEmptyList() {
        // Arrange
        Long orderId = 999L;
        when(paymentRepository.findByOrderId(orderId))
                .thenReturn(Arrays.asList());

        // Act
        List<PaymentEntity> result = paymentService.getPaymentsByOrderId(orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();

        verify(paymentRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void processPayment_DefaultStatus_ShouldBePending() {
        // Arrange
        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        PaymentEntity result = paymentService.processPayment(validOrderEvent);

        // Assert
        assertThat(result.getPaymentStatus()).isEqualTo("PENDING");
    }

    @Test
    void processPayment_WithZeroAmount_ShouldStillCreatePayment() {
        // Arrange
        OrderValidatedEvent eventWithZeroAmount = new OrderValidatedEvent(
                "event-zero",
                "ORDER_VALIDATED",
                LocalDateTime.now(),
                10L,
                "CUST-010",
                BigDecimal.ZERO
        );

        PaymentEntity zeroPayment = new PaymentEntity(
                "PMT-ZERO",
                10L,
                BigDecimal.ZERO,
                "PENDING"
        );

        when(paymentRepository.save(any(PaymentEntity.class)))
                .thenReturn(zeroPayment);

        // Act
        PaymentEntity result = paymentService.processPayment(eventWithZeroAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAmount()).isEqualTo(BigDecimal.ZERO);
        
        verify(paymentRepository, times(1)).save(any(PaymentEntity.class));
    }
}
