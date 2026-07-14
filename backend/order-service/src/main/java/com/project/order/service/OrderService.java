package com.project.order.service;

import com.project.common.dto.CreateOrderRequest;
import com.project.common.dto.OrderResponse;
import com.project.common.dto.OrderSearchRequest;
import com.project.common.dto.PagedResponse;

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
    
    /**
     * Search orders with filters and pagination
     * @param searchRequest the search request with filters and pagination
     * @return paginated list of orders
     */
    PagedResponse<OrderResponse> searchOrders(OrderSearchRequest searchRequest);
}
