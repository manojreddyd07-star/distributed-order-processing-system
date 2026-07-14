package com.project.order.service.impl;

import com.project.common.dto.CreateOrderRequest;
import com.project.common.dto.OrderResponse;
import com.project.common.dto.OrderSearchRequest;
import com.project.common.dto.PagedResponse;
import com.project.order.entity.OrderEntity;
import com.project.order.repository.OrderRepository;
import com.project.order.repository.OrderSpecification;
import com.project.order.service.KafkaProducerService;
import com.project.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final KafkaProducerService kafkaProducerService;
    
    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, KafkaProducerService kafkaProducerService) {
        this.orderRepository = orderRepository;
        this.kafkaProducerService = kafkaProducerService;
    }
    
    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Map CreateOrderRequest to OrderEntity
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setCustomerId(request.getCustomerId());
        orderEntity.setTotalAmount(request.getTotalAmount());
        
        // Set default orderStatus value
        orderEntity.setOrderStatus("PENDING");
        
        // Set createdAt timestamp
        orderEntity.setCreatedAt(LocalDateTime.now());
        
        // Save order using OrderRepository
        OrderEntity savedOrder = orderRepository.save(orderEntity);
        
        // Publish order created event to Kafka
        kafkaProducerService.publishOrderCreatedEvent(savedOrder);
        
        // Map OrderEntity to OrderResponse
        return mapToOrderResponse(savedOrder);
    }
    
    @Override
    public OrderResponse getOrderById(Long orderId) {
        // Fetch order using OrderRepository
        OrderEntity orderEntity = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));
        
        return mapToOrderResponse(orderEntity);
    }
    
    @Override
    public List<OrderResponse> getAllOrders() {
        // Fetch all orders using OrderRepository
        List<OrderEntity> orders = orderRepository.findAll();
        
        // Return list of OrderResponse
        return orders.stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public PagedResponse<OrderResponse> searchOrders(OrderSearchRequest searchRequest) {
        // Build specification for dynamic filtering
        Specification<OrderEntity> spec = OrderSpecification.buildSearchSpecification(searchRequest);
        
        // Build sort
        Sort sort = Sort.by(
            "DESC".equalsIgnoreCase(searchRequest.getSortDirection()) 
                ? Sort.Direction.DESC 
                : Sort.Direction.ASC,
            searchRequest.getSortBy()
        );
        
        // Build pageable
        Pageable pageable = PageRequest.of(
            searchRequest.getPage(), 
            searchRequest.getSize(), 
            sort
        );
        
        // Execute search
        Page<OrderEntity> orderPage = orderRepository.findAll(spec, pageable);
        
        // Map to OrderResponse
        List<OrderResponse> orderResponses = orderPage.getContent().stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
        
        // Return paginated response
        return new PagedResponse<>(
                orderResponses,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }
    
    /**
     * Helper method to map OrderEntity to OrderResponse
     */
    private OrderResponse mapToOrderResponse(OrderEntity entity) {
        OrderResponse response = new OrderResponse();
        response.setId(entity.getId());
        response.setCustomerId(entity.getCustomerId());
        response.setOrderStatus(entity.getOrderStatus());
        response.setTotalAmount(entity.getTotalAmount());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
