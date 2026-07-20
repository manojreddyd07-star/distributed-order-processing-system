package com.project.inventory.repository;

import com.project.inventory.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import jakarta.persistence.QueryHint;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryEntity, Long> {
    
    /**
     * Find inventory by product ID with query hints for performance
     * @param productId The product ID
     * @return Optional containing the inventory item if found
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    @Query("SELECT i FROM InventoryEntity i WHERE i.productId = :productId")
    Optional<InventoryEntity> findByProductId(String productId);
    
    /**
     * Find all inventory items by status with optimized query
     * @param status The inventory status (e.g., "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK")
     * @return List of inventory items with the specified status
     */
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "false"))
    @Query("SELECT i FROM InventoryEntity i WHERE i.status = :status ORDER BY i.productId")
    List<InventoryEntity> findByStatus(String status);
    
    /**
     * Check if inventory exists for a product
     * @param productId The product ID
     * @return true if inventory exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM InventoryEntity i WHERE i.productId = :productId")
    boolean existsByProductId(String productId);
}
