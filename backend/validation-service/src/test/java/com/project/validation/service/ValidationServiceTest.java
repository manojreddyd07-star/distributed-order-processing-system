package com.project.validation.service;

import com.project.common.events.OrderCreatedEvent;
import com.project.validation.entity.ValidationEntity;
import com.project.validation.event.ValidationEventProducer;
import com.project.validation.repository.ValidationRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {

    @Mock
    private ValidationRepository validationRepository;

    @Mock
    private ValidationEventProducer validationEventProducer;

    @InjectMocks
    private ValidationService validationService;

    private OrderCreatedEvent validOrderEvent;
    private ValidationEntity savedValidationEntity;

    @BeforeEach
    void setUp() {
        // Setup valid order event
        validOrderEvent = new OrderCreatedEvent(
                "event-123",
                "ORDER_CREATED",
                LocalDateTime.now(),
                1L,
                "CUST-001",
                new BigDecimal("99.99")
        );

        // Setup saved validation entity
        savedValidationEntity = new ValidationEntity(
                1L,
                "VALID",
                "Order validation successful"
        );
        savedValidationEntity.setId(1L);
    }

    @Test
    void validateOrder_ValidOrder_ShouldReturnValidStatus() {
        // Arrange
        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(savedValidationEntity);
        doNothing().when(validationEventProducer)
                .publishValidationSuccessEvent(any(), any(), any());

        // Act
        ValidationEntity result = validationService.validateOrder(validOrderEvent);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getValidationStatus()).isEqualTo("VALID");
        assertThat(result.getValidationMessage()).isEqualTo("Order validation successful");

        // Verify repository save was called
        ArgumentCaptor<ValidationEntity> validationCaptor = ArgumentCaptor.forClass(ValidationEntity.class);
        verify(validationRepository, times(1)).save(validationCaptor.capture());

        ValidationEntity capturedValidation = validationCaptor.getValue();
        assertThat(capturedValidation.getOrderId()).isEqualTo(1L);
        assertThat(capturedValidation.getValidationStatus()).isEqualTo("VALID");

        // Verify success event was published
        verify(validationEventProducer, times(1))
                .publishValidationSuccessEvent(eq(1L), eq("VALID"), eq("Order validation successful"));
        verify(validationEventProducer, never())
                .publishValidationFailedEvent(any(), any(), any());
    }

    @Test
    void validateOrder_NullCustomerId_ShouldReturnInvalidStatus() {
        // Arrange
        OrderCreatedEvent eventWithNullCustomerId = new OrderCreatedEvent(
                "event-456",
                "ORDER_CREATED",
                LocalDateTime.now(),
                2L,
                null,
                new BigDecimal("50.00")
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                2L,
                "INVALID",
                "Customer ID cannot be null"
        );
        invalidValidation.setId(2L);

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);
        doNothing().when(validationEventProducer)
                .publishValidationFailedEvent(any(), any(), any());

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithNullCustomerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(2L);
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Customer ID cannot be null");

        // Verify failure event was published
        verify(validationEventProducer, times(1))
                .publishValidationFailedEvent(eq(2L), eq("INVALID"), anyString());
        verify(validationEventProducer, never())
                .publishValidationSuccessEvent(any(), any(), any());
    }

    @Test
    void validateOrder_EmptyCustomerId_ShouldReturnInvalidStatus() {
        // Arrange
        OrderCreatedEvent eventWithEmptyCustomerId = new OrderCreatedEvent(
                "event-789",
                "ORDER_CREATED",
                LocalDateTime.now(),
                3L,
                "   ",
                new BigDecimal("75.00")
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                3L,
                "INVALID",
                "Customer ID cannot be empty"
        );

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithEmptyCustomerId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Customer ID cannot be empty");

        verify(validationEventProducer, times(1))
                .publishValidationFailedEvent(any(), any(), anyString());
    }

    @Test
    void validateOrder_NullTotalAmount_ShouldReturnInvalidStatus() {
        // Arrange
        OrderCreatedEvent eventWithNullAmount = new OrderCreatedEvent(
                "event-111",
                "ORDER_CREATED",
                LocalDateTime.now(),
                4L,
                "CUST-004",
                null
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                4L,
                "INVALID",
                "Total amount must be greater than 0"
        );

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithNullAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Total amount must be greater than 0");

        verify(validationRepository, times(1)).save(any(ValidationEntity.class));
        verify(validationEventProducer, times(1))
                .publishValidationFailedEvent(any(), any(), anyString());
    }

    @Test
    void validateOrder_ZeroTotalAmount_ShouldReturnInvalidStatus() {
        // Arrange
        OrderCreatedEvent eventWithZeroAmount = new OrderCreatedEvent(
                "event-222",
                "ORDER_CREATED",
                LocalDateTime.now(),
                5L,
                "CUST-005",
                BigDecimal.ZERO
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                5L,
                "INVALID",
                "Total amount must be greater than 0"
        );

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithZeroAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Total amount must be greater than 0");
    }

    @Test
    void validateOrder_NegativeTotalAmount_ShouldReturnInvalidStatus() {
        // Arrange
        OrderCreatedEvent eventWithNegativeAmount = new OrderCreatedEvent(
                "event-333",
                "ORDER_CREATED",
                LocalDateTime.now(),
                6L,
                "CUST-006",
                new BigDecimal("-10.00")
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                6L,
                "INVALID",
                "Total amount must be greater than 0"
        );

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithNegativeAmount);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Total amount must be greater than 0");
    }

    @Test
    void validateOrder_MultipleValidationErrors_ShouldReturnAllErrors() {
        // Arrange
        OrderCreatedEvent eventWithMultipleErrors = new OrderCreatedEvent(
                "event-444",
                "ORDER_CREATED",
                LocalDateTime.now(),
                7L,
                null,
                BigDecimal.ZERO
        );

        ValidationEntity invalidValidation = new ValidationEntity(
                7L,
                "INVALID",
                "Customer ID cannot be null; Total amount must be greater than 0"
        );

        when(validationRepository.save(any(ValidationEntity.class)))
                .thenReturn(invalidValidation);

        // Act
        ValidationEntity result = validationService.validateOrder(eventWithMultipleErrors);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getValidationStatus()).isEqualTo("INVALID");
        assertThat(result.getValidationMessage()).contains("Customer ID cannot be null");
        assertThat(result.getValidationMessage()).contains("Total amount must be greater than 0");
    }

    @Test
    void getAllValidations_Success_ShouldReturnAllValidations() {
        // Arrange
        ValidationEntity validation1 = new ValidationEntity(1L, "VALID", "Success");
        ValidationEntity validation2 = new ValidationEntity(2L, "INVALID", "Failed");

        when(validationRepository.findAll()).thenReturn(Arrays.asList(validation1, validation2));

        // Act
        List<ValidationEntity> result = validationService.getAllValidations();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getOrderId()).isEqualTo(1L);
        assertThat(result.get(1).getOrderId()).isEqualTo(2L);

        verify(validationRepository, times(1)).findAll();
    }

    @Test
    void getValidationsByOrderId_Success_ShouldReturnValidations() {
        // Arrange
        Long orderId = 1L;
        ValidationEntity validation = new ValidationEntity(orderId, "VALID", "Success");

        when(validationRepository.findByOrderId(orderId))
                .thenReturn(Arrays.asList(validation));

        // Act
        List<ValidationEntity> result = validationService.getValidationsByOrderId(orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(orderId);

        verify(validationRepository, times(1)).findByOrderId(orderId);
    }

    @Test
    void getValidationsByStatus_Success_ShouldReturnValidations() {
        // Arrange
        String status = "VALID";
        ValidationEntity validation1 = new ValidationEntity(1L, status, "Success 1");
        ValidationEntity validation2 = new ValidationEntity(2L, status, "Success 2");

        when(validationRepository.findByValidationStatus(status))
                .thenReturn(Arrays.asList(validation1, validation2));

        // Act
        List<ValidationEntity> result = validationService.getValidationsByStatus(status);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getValidationStatus()).isEqualTo(status);
        assertThat(result.get(1).getValidationStatus()).isEqualTo(status);

        verify(validationRepository, times(1)).findByValidationStatus(status);
    }
}
