package com.project.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class CreateOrderRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Total amount is required")
    @Positive(message = "Total amount must be positive")
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

    @Override
    public String toString() {
        return "CreateOrderRequest{" +
                "customerId=" + customerId +
                ", totalAmount=" + totalAmount +
                '}';
    }
}
