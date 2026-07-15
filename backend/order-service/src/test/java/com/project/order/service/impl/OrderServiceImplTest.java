package com.project.order.service.impl;

import com.project.common.dto.CreateOrderRequest;
import com.project.common.dto.OrderResponse;
import com.project.common.dto.OrderSearchRequest;
import com.project.common.dto.PagedResponse;
import com.project.order.entity.OrderEntity;
import com.project.order.repository.OrderRepository;
import com.project.order.repository.OrderSpecification;
import com.project.order.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private CreateOrderRequest validCreateRequest;
    private OrderEntity savedOrderEntity;

    @BeforeEach
    void setUp() {
        // Setup valid create request
        validCreateRequest = new CreateOrderRequest();
        validCreateRequest.setCustomerId("CUST-001");
        validCreateRequest.setTotalAmount(new BigDecimal("99.99"));

        // Setup saved order entity
        savedOrderEntity = new OrderEntity();
        savedOrderEntity.setId(1L);
        savedOrderEntity.setCustomerId("CUST-001");
        savedOrderEntity.setTotalAmount(new BigDecimal("99.99"));
        savedOrderEntity.setOrderStatus("PENDING");
        savedOrderEntity.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void createOrder_Success_ShouldSaveOrderAndPublishEvent() {
        // Arrange
        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrderEntity);
        doNothing().when(kafkaProducerService).publishOrderCreatedEvent(any(OrderEntity.class));

        // Act
        OrderResponse response = orderService.createOrder(validCreateRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCustomerId()).isEqualTo("CUST-001");
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(response.getOrderStatus()).isEqualTo("PENDING");
        assertThat(response.getCreatedAt()).isNotNull();

        // Verify repository interaction
        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        
        OrderEntity capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getCustomerId()).isEqualTo("CUST-001");
        assertThat(capturedOrder.getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(capturedOrder.getOrderStatus()).isEqualTo("PENDING");
        assertThat(capturedOrder.getCreatedAt()).isNotNull();

        // Verify Kafka event was published
        verify(kafkaProducerService, times(1)).publishOrderCreatedEvent(savedOrderEntity);
    }

    @Test
    void createOrder_WithNullCustomerId_ShouldStillCreateOrder() {
        // Arrange
        CreateOrderRequest requestWithNullCustomerId = new CreateOrderRequest();
        requestWithNullCustomerId.setCustomerId(null);
        requestWithNullCustomerId.setTotalAmount(new BigDecimal("50.00"));

        OrderEntity savedOrder = new OrderEntity();
        savedOrder.setId(2L);
        savedOrder.setCustomerId(null);
        savedOrder.setTotalAmount(new BigDecimal("50.00"));
        savedOrder.setOrderStatus("PENDING");
        savedOrder.setCreatedAt(LocalDateTime.now());

        when(orderRepository.save(any(OrderEntity.class))).thenReturn(savedOrder);

        // Act
        OrderResponse response = orderService.createOrder(requestWithNullCustomerId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getCustomerId()).isNull();
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("50.00"));
        
        verify(orderRepository, times(1)).save(any(OrderEntity.class));
        verify(kafkaProducerService, times(1)).publishOrderCreatedEvent(savedOrder);
    }

    @Test
    void getOrderById_Success_ShouldReturnOrder() {
        // Arrange
        Long orderId = 1L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(savedOrderEntity));

        // Act
        OrderResponse response = orderService.getOrderById(orderId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCustomerId()).isEqualTo("CUST-001");
        assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(response.getOrderStatus()).isEqualTo("PENDING");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getOrderById_OrderNotFound_ShouldThrowException() {
        // Arrange
        Long orderId = 999L;
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Order not found with id: 999");

        verify(orderRepository, times(1)).findById(orderId);
    }

    @Test
    void getAllOrders_Success_ShouldReturnAllOrders() {
        // Arrange
        OrderEntity order2 = new OrderEntity();
        order2.setId(2L);
        order2.setCustomerId("CUST-002");
        order2.setTotalAmount(new BigDecimal("150.00"));
        order2.setOrderStatus("COMPLETED");
        order2.setCreatedAt(LocalDateTime.now());

        List<OrderEntity> orders = Arrays.asList(savedOrderEntity, order2);
        when(orderRepository.findAll()).thenReturn(orders);

        // Act
        List<OrderResponse> responses = orderService.getAllOrders();

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo(1L);
        assertThat(responses.get(1).getId()).isEqualTo(2L);
        assertThat(responses.get(0).getCustomerId()).isEqualTo("CUST-001");
        assertThat(responses.get(1).getCustomerId()).isEqualTo("CUST-002");

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void getAllOrders_EmptyRepository_ShouldReturnEmptyList() {
        // Arrange
        when(orderRepository.findAll()).thenReturn(Arrays.asList());

        // Act
        List<OrderResponse> responses = orderService.getAllOrders();

        // Assert
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void searchOrders_Success_ShouldReturnPagedResponse() {
        // Arrange
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(10);
        searchRequest.setSortBy("createdAt");
        searchRequest.setSortDirection("DESC");

        List<OrderEntity> orders = Arrays.asList(savedOrderEntity);
        Page<OrderEntity> orderPage = new PageImpl<>(orders, 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // Act
        PagedResponse<OrderResponse> response = orderService.searchOrders(searchRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);

        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchOrders_WithFilters_ShouldApplyFilters() {
        // Arrange
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(10);
        searchRequest.setSortBy("id");
        searchRequest.setSortDirection("ASC");
        searchRequest.setCustomerId("CUST-001");
        searchRequest.setOrderStatus("PENDING");

        List<OrderEntity> filteredOrders = Arrays.asList(savedOrderEntity);
        Page<OrderEntity> orderPage = new PageImpl<>(filteredOrders, 
                org.springframework.data.domain.PageRequest.of(0, 10), 1);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // Act
        PagedResponse<OrderResponse> response = orderService.searchOrders(searchRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCustomerId()).isEqualTo("CUST-001");
        assertThat(response.getContent().get(0).getOrderStatus()).isEqualTo("PENDING");

        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void searchOrders_EmptyResult_ShouldReturnEmptyPagedResponse() {
        // Arrange
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setPage(0);
        searchRequest.setSize(10);
        searchRequest.setSortBy("createdAt");
        searchRequest.setSortDirection("DESC");

        Page<OrderEntity> emptyPage = new PageImpl<>(Arrays.asList(), 
                org.springframework.data.domain.PageRequest.of(0, 10), 0);

        when(orderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        PagedResponse<OrderResponse> response = orderService.searchOrders(searchRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isEqualTo(0L);
        assertThat(response.getTotalPages()).isEqualTo(0);

        verify(orderRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}
