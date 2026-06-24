package com.project.inventory.controller;

import com.project.inventory.entity.InventoryEntity;
import com.project.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryController {
    
    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);
    
    private final InventoryService inventoryService;
    
    @Autowired
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Get all inventory items
     * @return List of all inventory items
     */
    @GetMapping
    public ResponseEntity<List<InventoryEntity>> getAllInventory() {
        logger.info("GET /api/inventory - Fetching all inventory items");
        
        try {
            List<InventoryEntity> inventory = inventoryService.getAllInventory();
            logger.info("Successfully retrieved {} inventory items", inventory.size());
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            logger.error("Error fetching all inventory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get inventory by product ID
     * @param productId The product ID
     * @return The inventory item
     */
    @GetMapping("/product/{productId}")
    public ResponseEntity<InventoryEntity> getInventoryByProductId(@PathVariable String productId) {
        logger.info("GET /api/inventory/product/{} - Fetching inventory for product", productId);
        
        try {
            InventoryEntity inventory = inventoryService.getInventoryByProductId(productId);
            logger.info("Successfully retrieved inventory for product: {}", productId);
            return ResponseEntity.ok(inventory);
        } catch (RuntimeException e) {
            logger.warn("Inventory not found for product: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            logger.error("Error fetching inventory for product: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get inventory by status
     * @param status The inventory status
     * @return List of inventory items with the specified status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InventoryEntity>> getInventoryByStatus(@PathVariable String status) {
        logger.info("GET /api/inventory/status/{} - Fetching inventory by status", status);
        
        try {
            List<InventoryEntity> inventory = inventoryService.getInventoryByStatus(status);
            logger.info("Successfully retrieved {} inventory items with status: {}", inventory.size(), status);
            return ResponseEntity.ok(inventory);
        } catch (Exception e) {
            logger.error("Error fetching inventory by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create or update inventory
     * @param inventory The inventory entity to save
     * @return The saved inventory entity
     */
    @PostMapping
    public ResponseEntity<InventoryEntity> createInventory(@RequestBody InventoryEntity inventory) {
        logger.info("POST /api/inventory - Creating/updating inventory for product: {}", 
                   inventory.getProductId());
        
        try {
            InventoryEntity savedInventory = inventoryService.saveInventory(inventory);
            logger.info("Successfully saved inventory for product: {}", savedInventory.getProductId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedInventory);
        } catch (Exception e) {
            logger.error("Error saving inventory for product: {}", inventory.getProductId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Verify inventory availability
     * @param productId The product ID
     * @param quantity The required quantity
     * @return true if inventory is sufficient, false otherwise
     */
    @GetMapping("/verify")
    public ResponseEntity<Boolean> verifyInventory(
            @RequestParam String productId,
            @RequestParam Integer quantity) {
        logger.info("GET /api/inventory/verify - Product: {}, Quantity: {}", productId, quantity);
        
        try {
            boolean isAvailable = inventoryService.verifyInventory(productId, quantity);
            logger.info("Inventory verification result for product {}: {}", productId, isAvailable);
            return ResponseEntity.ok(isAvailable);
        } catch (Exception e) {
            logger.error("Error verifying inventory for product: {}", productId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
