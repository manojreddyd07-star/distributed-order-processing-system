package com.project.order.repository;

import com.project.common.dto.OrderSearchRequest;
import com.project.order.entity.OrderEntity;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {
    
    /**
     * Build search specification based on OrderSearchRequest
     */
    public static Specification<OrderEntity> buildSearchSpecification(OrderSearchRequest searchRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // Filter by customer ID
            if (searchRequest.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), searchRequest.getCustomerId()));
            }
            
            // Filter by order status
            if (searchRequest.getOrderStatus() != null && !searchRequest.getOrderStatus().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), searchRequest.getOrderStatus()));
            }
            
            // Filter by date range - start date
            if (searchRequest.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), searchRequest.getStartDate()));
            }
            
            // Filter by date range - end date
            if (searchRequest.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), searchRequest.getEndDate()));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
