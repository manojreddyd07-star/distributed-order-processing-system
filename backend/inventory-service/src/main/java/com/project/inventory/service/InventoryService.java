package com.project.inventory.service;

import com.project.inventory.entity.InventoryEntity;
import com.project.inventory.event.InventoryReservedEvent;
import com.project.inventory.event.InventoryRejectedEvent;
import com.project.inventory.producer.InventoryEventProducer;
import com.project.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);
    private static final String STATUS_IN_STOCK = "IN_STOCK";
    private static final String STATUS_LOW_STOCK = "LOW_STOCK";
    private static final String STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";
    
    private final InventoryRepository inventoryRepository;
    private final InventoryEventProducer inventoryEventProducer;
    
    @Autowired
    public InventoryService(InventoryRepository inventoryRepository, 
                          InventoryEventProducer inventoryEventProducer) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryEventProducer = inventoryEventProducer;
    }
    
    /**
     * Verify inventory availability for a product
     * @param productId The product ID
     * @param requiredQuantity The required quantity
     * @return true if inventory is sufficient, false otherwise
     */
    public boolean verifyInventory(String productId, Integer requiredQuantity) {
        logger.info("Verifying inventory for product ID: {}, required quantity: {}", productId, requiredQuantity);
        
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElse(null);
        
        if (inventory == null) {
            logger.warn("Inventory verification failed - Product not found: {}", productId);
            return false;
        }
        
        boolean isAvailable = inventory.getAvailableQuantity() >= requiredQuantity;
        
        if (isAvailable) {
            logger.info("Inventory verification successful - Product: {}, Available: {}, Required: {}", 
                       productId, inventory.getAvailableQuantity(), requiredQuantity);
        } else {
            logger.warn("Inventory verification failed - Product: {}, Available: {}, Required: {}", 
                       productId, inventory.getAvailableQuantity(), requiredQuantity);
        }
        
        return isAvailable;
    }
    
    /**
     * Reserve inventory for an order
     * @param productId The product ID
     * @param productName The product name
     * @param quantity The quantity to reserve
     * @param orderId The order ID
     * @return InventoryEntity representing the updated inventory
     */
    @Transactional
    public InventoryEntity reserveInventory(String productId, String productName, Integer quantity, Long orderId) {
        logger.info("Reserving inventory - Product ID: {}, Product Name: {}, Quantity: {}, Order ID: {}", 
                   productId, productName, quantity, orderId);
        
        // Find existing inventory or create new one
        InventoryEntity inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> {
                    logger.info("Creating new inventory record for product: {}", productId);
                    InventoryEntity newInventory = new InventoryEntity(
                        productId,
                        productName,
                        0,
                        0,
                        0,
                        STATUS_OUT_OF_STOCK
                    );
                    return inventoryRepository.save(newInventory);
                });
        
        // Update quantities
        Integer currentAvailable = inventory.getAvailableQuantity();
        Integer currentReserved = inventory.getReservedQuantity();
        
        if (currentAvailable < quantity) {
            logger.error("Insufficient inventory - Product: {}, Available: {}, Requested: {}", 
                        productId, currentAvailable, quantity);
            
            // Publish InventoryRejectedEvent
            publishInventoryRejectedEvent(productId, orderId, currentAvailable, currentReserved, 
                                         inventory.getStatus(), "Insufficient inventory");
            
            throw new RuntimeException("Insufficient inventory for product: " + productId);
        }
        
        // Update available and reserved quantities
        inventory.setAvailableQuantity(currentAvailable - quantity);
        inventory.setReservedQuantity(currentReserved + quantity);
        
        // Update status based on available quantity
        updateInventoryStatus(inventory);
        
        // Persist the updated inventory
        InventoryEntity updatedInventory = inventoryRepository.save(inventory);
        
        logger.info("✅ Inventory reserved successfully - Product: {}, Order: {}, New Available: {}, New Reserved: {}, Status: {}", 
                   productId, orderId, updatedInventory.getAvailableQuantity(), 
                   updatedInventory.getReservedQuantity(), updatedInventory.getStatus());
        
        // Publish InventoryReservedEvent
        publishInventoryReservedEvent(productId, orderId, updatedInventory.getAvailableQuantity(),
                                     updatedInventory.getReservedQuantity(), updatedInventory.getStatus());
        
        return updatedInventory;
    }
    
    /**
     * Update inventory status based on available quantity
     * @param inventory The inventory entity to update
     */
    private void updateInventoryStatus(InventoryEntity inventory) {
        Integer available = inventory.getAvailableQuantity();
        
        if (available == 0) {
            inventory.setStatus(STATUS_OUT_OF_STOCK);
        } else if (available <= 10) {
            inventory.setStatus(STATUS_LOW_STOCK);
        } else {
            inventory.setStatus(STATUS_IN_STOCK);
        }
    }
    
    /**
     * Get all inventory items
     * @return List of all inventory items
     */
    public List<InventoryEntity> getAllInventory() {
        logger.info("Fetching all inventory records");
        return inventoryRepository.findAll();
    }
    
    /**
     * Get inventory by product ID
     * @param productId The product ID
     * @return InventoryEntity if found
     */
    public InventoryEntity getInventoryByProductId(String productId) {
        logger.info("Fetching inventory for product ID: {}", productId);
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException("Inventory not found for product: " + productId));
    }
    
    /**
     * Get inventory by status
     * @param status The inventory status
     * @return List of inventory items with the specified status
     */
    public List<InventoryEntity> getInventoryByStatus(String status) {
        logger.info("Fetching inventory by status: {}", status);
        return inventoryRepository.findByStatus(status);
    }
    
    /**
     * Save an inventory record
     * @param inventory The inventory entity to save
     * @return Saved InventoryEntity
     */
    public InventoryEntity saveInventory(InventoryEntity inventory) {
        logger.info("Saving inventory record - Product ID: {}", inventory.getProductId());
        updateInventoryStatus(inventory);
        InventoryEntity savedInventory = inventoryRepository.save(inventory);
        logger.info("Inventory record saved successfully - ID: {}", savedInventory.getId());
        return savedInventory;
    }
    
    /**
     * Publish InventoryReservedEvent
     * @param productId The product ID
     * @param orderId The order ID
     * @param availableQuantity The available quantity after reservation
     * @param reservedQuantity The reserved quantity after reservation
     * @param inventoryStatus The inventory status
     */
    private void publishInventoryReservedEvent(String productId, Long orderId, Integer availableQuantity,
                                               Integer reservedQuantity, String inventoryStatus) {
        try {
            String eventId = UUID.randomUUID().toString();
            InventoryReservedEvent event = new InventoryReservedEvent(
                eventId,
                "INVENTORY_RESERVED",
                LocalDateTime.now(),
                productId,
                orderId,
                availableQuantity,
                reservedQuantity,
                inventoryStatus
            );
            
            inventoryEventProducer.publishInventoryReservedEvent(event);
            logger.info("✅ InventoryReservedEvent published successfully - EventId: {}, ProductId: {}, OrderId: {}", 
                       eventId, productId, orderId);
        } catch (Exception e) {
            logger.error("❌ Failed to publish InventoryReservedEvent - ProductId: {}, OrderId: {}, Error: {}", 
                        productId, orderId, e.getMessage(), e);
        }
    }
    
    /**
     * Publish InventoryRejectedEvent
     * @param productId The product ID
     * @param orderId The order ID
     * @param availableQuantity The available quantity
     * @param reservedQuantity The reserved quantity
     * @param inventoryStatus The inventory status
     * @param reason The rejection reason
     */
    private void publishInventoryRejectedEvent(String productId, Long orderId, Integer availableQuantity,
                                               Integer reservedQuantity, String inventoryStatus, String reason) {
        try {
            String eventId = UUID.randomUUID().toString();
            InventoryRejectedEvent event = new InventoryRejectedEvent(
                eventId,
                "INVENTORY_REJECTED",
                LocalDateTime.now(),
                productId,
                orderId,
                availableQuantity,
                reservedQuantity,
                inventoryStatus,
                reason
            );
            
            inventoryEventProducer.publishInventoryRejectedEvent(event);
            logger.info("❌ InventoryRejectedEvent published successfully - EventId: {}, ProductId: {}, OrderId: {}, Reason: {}", 
                       eventId, productId, orderId, reason);
        } catch (Exception e) {
            logger.error("❌ Failed to publish InventoryRejectedEvent - ProductId: {}, OrderId: {}, Error: {}", 
                        productId, orderId, e.getMessage(), e);
        }
    }
}
