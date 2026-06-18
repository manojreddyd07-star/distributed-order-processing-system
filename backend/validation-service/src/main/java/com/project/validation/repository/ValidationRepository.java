package com.project.validation.repository;

import com.project.validation.entity.ValidationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ValidationRepository extends JpaRepository<ValidationEntity, Long> {
    
    /**
     * Find all validations for a specific order
     * @param orderId The order ID
     * @return List of validations for the order
     */
    List<ValidationEntity> findByOrderId(Long orderId);
    
    /**
     * Find all validations by status
     * @param validationStatus The validation status (e.g., "VALID", "INVALID")
     * @return List of validations with the specified status
     */
    List<ValidationEntity> findByValidationStatus(String validationStatus);
}
