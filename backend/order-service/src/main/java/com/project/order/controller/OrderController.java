package com.project.order.controller;

import com.project.common.dto.CreateOrderRequest;
import com.project.common.dto.OrderResponse;
import com.project.common.dto.OrderSearchRequest;
import com.project.common.dto.PagedResponse;
import com.project.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Create a new order
     * @param request the order creation request
     * @return the created order response
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Get all orders
     * @return list of all orders
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = orderService.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    /**
     * Get an order by ID
     * @param id the order ID
     * @return the order response
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Search orders with filters and pagination
     * @param customerId filter by customer ID (optional)
     * @param orderStatus filter by order status (optional)
     * @param startDate filter by start date in ISO format (optional)
     * @param endDate filter by end date in ISO format (optional)
     * @param page page number (default: 0)
     * @param size page size (default: 10)
     * @param sortBy sort field (default: createdAt)
     * @param sortDirection sort direction ASC/DESC (default: DESC)
     * @return paginated list of orders
     */
    @GetMapping("/search")
    public ResponseEntity<PagedResponse<OrderResponse>> searchOrders(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        
        // Build search request
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setCustomerId(customerId);
        searchRequest.setOrderStatus(orderStatus);
        
        // Parse dates if provided
        if (startDate != null && !startDate.trim().isEmpty()) {
            searchRequest.setStartDate(java.time.LocalDateTime.parse(startDate));
        }
        if (endDate != null && !endDate.trim().isEmpty()) {
            searchRequest.setEndDate(java.time.LocalDateTime.parse(endDate));
        }
        
        searchRequest.setPage(page);
        searchRequest.setSize(size);
        searchRequest.setSortBy(sortBy);
        searchRequest.setSortDirection(sortDirection);
        
        // Execute search
        PagedResponse<OrderResponse> response = orderService.searchOrders(searchRequest);
        return ResponseEntity.ok(response);
    }
}
