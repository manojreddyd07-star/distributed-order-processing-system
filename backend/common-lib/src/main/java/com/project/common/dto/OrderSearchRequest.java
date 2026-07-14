package com.project.common.dto;

import java.time.LocalDateTime;

public class OrderSearchRequest {
    
    private Long customerId;
    private String orderStatus;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "createdAt";
    private String sortDirection = "DESC";
    
    // Constructors
    public OrderSearchRequest() {
    }
    
    public OrderSearchRequest(Long customerId, String orderStatus, LocalDateTime startDate, 
                             LocalDateTime endDate, Integer page, Integer size, 
                             String sortBy, String sortDirection) {
        this.customerId = customerId;
        this.orderStatus = orderStatus;
        this.startDate = startDate;
        this.endDate = endDate;
        this.page = page;
        this.size = size;
        this.sortBy = sortBy;
        this.sortDirection = sortDirection;
    }
    
    // Getters and Setters
    public Long getCustomerId() {
        return customerId;
    }
    
    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
    
    public String getOrderStatus() {
        return orderStatus;
    }
    
    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }
    
    public LocalDateTime getStartDate() {
        return startDate;
    }
    
    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }
    
    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }
    
    public Integer getPage() {
        return page;
    }
    
    public void setPage(Integer page) {
        this.page = page;
    }
    
    public Integer getSize() {
        return size;
    }
    
    public void setSize(Integer size) {
        this.size = size;
    }
    
    public String getSortBy() {
        return sortBy;
    }
    
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    public String getSortDirection() {
        return sortDirection;
    }
    
    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
    
    @Override
    public String toString() {
        return "OrderSearchRequest{" +
                "customerId=" + customerId +
                ", orderStatus='" + orderStatus + '\'' +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                ", sortDirection='" + sortDirection + '\'' +
                '}';
    }
}
