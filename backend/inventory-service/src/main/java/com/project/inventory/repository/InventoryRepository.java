package com.project.inventory.repository;

import com.project.inventory.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {
    
    /**
     * Find inventory by product ID
     * @param productId The product ID
     * @return Optional containing the inventory item if found
     */
    Optional<InventoryEntity> findByProductId(String productId);
    
    /**
     * Find all inventory items by status
     * @param status The inventory status (e.g., "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK")
     * @return List of inventory items with the specified status
     */
    List<InventoryEntity> findByStatus(String status);
    
    /**
     * Check if inventory exists for a product
     * @param productId The product ID
     * @return true if inventory exists, false otherwise
     */
    boolean existsByProductId(String productId);
}
