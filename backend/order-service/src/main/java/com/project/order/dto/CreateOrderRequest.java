package com.project.order.dto;

import java.math.BigDecimal;

public class CreateOrderRequest {
    
    private Long customerId;
    private BigDecimal totalAmount;
    
    // Constructors
    public CreateOrderRequest() {
    }
    
    public CreateOrderRequest(Long customerId, BigDecimal totalAmount) {
        this.customerId = customerId;
        this.totalAmount = totalAmount;
    }
    
    // Getters and Setters
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
