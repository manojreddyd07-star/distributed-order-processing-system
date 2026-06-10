package com.project.order.service;

import com.project.order.dto.CreateOrderRequest;
import com.project.order.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    
    /**
     * Create a new order
     * @param request the order creation request
     * @return the created order response
     */
    OrderResponse createOrder(CreateOrderRequest request);
    
    /**
     * Get an order by ID
     * @param orderId the order ID
     * @return the order response
     */
    OrderResponse getOrderById(Long orderId);
    
    /**
     * Get all orders
     * @return list of all orders
     */
    List<OrderResponse> getAllOrders();
}
