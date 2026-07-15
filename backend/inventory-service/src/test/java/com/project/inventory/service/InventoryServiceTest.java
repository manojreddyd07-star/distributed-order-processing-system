package com.project.inventory.service;

import com.project.common.events.InventoryRejectedEvent;
import com.project.common.events.InventoryReservedEvent;
import com.project.inventory.entity.InventoryEntity;
import com.project.inventory.producer.InventoryEventProducer;
import com.project.inventory.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @InjectMocks
    private InventoryService inventoryService;

    private InventoryEntity testInventory;

    @BeforeEach
    void setUp() {
        // Setup test inventory with sufficient stock
        testInventory = new InventoryEntity(
                "PROD-001",
                "Test Product",
                100,
                0,
                100,
                "IN_STOCK"
        );
        testInventory.setId(1L);
    }

    @Test
    void verifyInventory_SufficientStock_ShouldReturnTrue() {
        // Arrange
        String productId = "PROD-001";
        Integer requiredQuantity = 10;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));

        // Act
        boolean result = inventoryService.verifyInventory(productId, requiredQuantity);

        // Assert
        assertThat(result).isTrue();
        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void verifyInventory_InsufficientStock_ShouldReturnFalse() {
        // Arrange
        String productId = "PROD-001";
        Integer requiredQuantity = 150;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));

        // Act
        boolean result = inventoryService.verifyInventory(productId, requiredQuantity);

        // Assert
        assertThat(result).isFalse();
        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void verifyInventory_ProductNotFound_ShouldReturnFalse() {
        // Arrange
        String productId = "PROD-NOTFOUND";
        Integer requiredQuantity = 10;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        // Act
        boolean result = inventoryService.verifyInventory(productId, requiredQuantity);

        // Assert
        assertThat(result).isFalse();
        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void verifyInventory_ExactQuantity_ShouldReturnTrue() {
        // Arrange
        String productId = "PROD-001";
        Integer requiredQuantity = 100; // Exactly the available quantity

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));

        // Act
        boolean result = inventoryService.verifyInventory(productId, requiredQuantity);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void reserveInventory_Success_ShouldUpdateInventoryAndPublishEvent() {
        // Arrange
        String productId = "PROD-001";
        String productName = "Test Product";
        Integer quantity = 10;
        Long orderId = 1L;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(inventoryEventProducer)
                .publishInventoryReservedEvent(any(InventoryReservedEvent.class));

        // Act
        InventoryEntity result = inventoryService.reserveInventory(productId, productName, quantity, orderId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getAvailableQuantity()).isEqualTo(90); // 100 - 10
        assertThat(result.getReservedQuantity()).isEqualTo(10); // 0 + 10
        assertThat(result.getStatus()).isEqualTo("IN_STOCK");

        // Verify repository interactions
        verify(inventoryRepository, times(1)).findByProductId(productId);
        verify(inventoryRepository, times(1)).save(any(InventoryEntity.class));

        // Verify event was published
        verify(inventoryEventProducer, times(1))
                .publishInventoryReservedEvent(any(InventoryReservedEvent.class));
    }

    @Test
    void reserveInventory_UpdatesStatusToLowStock_WhenAvailableQuantityIsLow() {
        // Arrange
        String productId = "PROD-001";
        String productName = "Test Product";
        Integer quantity = 95; // After reservation, available will be 5 (LOW_STOCK)
        Long orderId = 1L;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryEntity result = inventoryService.reserveInventory(productId, productName, quantity, orderId);

        // Assert
        assertThat(result.getAvailableQuantity()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo("LOW_STOCK");
    }

    @Test
    void reserveInventory_UpdatesStatusToOutOfStock_WhenAvailableQuantityIsZero() {
        // Arrange
        String productId = "PROD-001";
        String productName = "Test Product";
        Integer quantity = 100; // Reserve all available
        Long orderId = 1L;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));
        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryEntity result = inventoryService.reserveInventory(productId, productName, quantity, orderId);

        // Assert
        assertThat(result.getAvailableQuantity()).isEqualTo(0);
        assertThat(result.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void reserveInventory_InsufficientStock_ShouldThrowExceptionAndPublishRejectedEvent() {
        // Arrange
        String productId = "PROD-001";
        String productName = "Test Product";
        Integer quantity = 150; // More than available
        Long orderId = 1L;

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));
        doNothing().when(inventoryEventProducer)
                .publishInventoryRejectedEvent(any(InventoryRejectedEvent.class));

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveInventory(productId, productName, quantity, orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient inventory for product: PROD-001");

        // Verify no save occurred
        verify(inventoryRepository, never()).save(any(InventoryEntity.class));

        // Verify rejected event was published
        verify(inventoryEventProducer, times(1))
                .publishInventoryRejectedEvent(any(InventoryRejectedEvent.class));
    }

    @Test
    void reserveInventory_ProductNotFound_ShouldCreateNewInventoryAndFail() {
        // Arrange
        String productId = "PROD-NEW";
        String productName = "New Product";
        Integer quantity = 10;
        Long orderId = 1L;

        InventoryEntity newInventory = new InventoryEntity(
                productId,
                productName,
                0,
                0,
                0,
                "OUT_OF_STOCK"
        );

        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.empty());
        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenReturn(newInventory);

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.reserveInventory(productId, productName, quantity, orderId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Insufficient inventory");

        // Verify new inventory was created
        verify(inventoryRepository, times(1)).save(argThat(inv -> 
                inv.getProductId().equals(productId) && 
                inv.getAvailableQuantity() == 0
        ));
    }

    @Test
    void getAllInventory_Success_ShouldReturnAllInventory() {
        // Arrange
        InventoryEntity inventory1 = new InventoryEntity("PROD-001", "Product 1", 100, 0, 100, "IN_STOCK");
        InventoryEntity inventory2 = new InventoryEntity("PROD-002", "Product 2", 5, 10, 15, "LOW_STOCK");
        InventoryEntity inventory3 = new InventoryEntity("PROD-003", "Product 3", 0, 20, 20, "OUT_OF_STOCK");

        when(inventoryRepository.findAll()).thenReturn(Arrays.asList(inventory1, inventory2, inventory3));

        // Act
        List<InventoryEntity> result = inventoryService.getAllInventory();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getProductId()).isEqualTo("PROD-001");
        assertThat(result.get(1).getProductId()).isEqualTo("PROD-002");
        assertThat(result.get(2).getProductId()).isEqualTo("PROD-003");

        verify(inventoryRepository, times(1)).findAll();
    }

    @Test
    void getInventoryByProductId_Success_ShouldReturnInventory() {
        // Arrange
        String productId = "PROD-001";
        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.of(testInventory));

        // Act
        InventoryEntity result = inventoryService.getInventoryByProductId(productId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getProductName()).isEqualTo("Test Product");

        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void getInventoryByProductId_NotFound_ShouldThrowException() {
        // Arrange
        String productId = "PROD-NOTFOUND";
        when(inventoryRepository.findByProductId(productId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> inventoryService.getInventoryByProductId(productId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Inventory not found for product: PROD-NOTFOUND");

        verify(inventoryRepository, times(1)).findByProductId(productId);
    }

    @Test
    void getInventoryByStatus_Success_ShouldReturnFilteredInventory() {
        // Arrange
        String status = "LOW_STOCK";
        InventoryEntity lowStock1 = new InventoryEntity("PROD-001", "Product 1", 5, 0, 5, status);
        InventoryEntity lowStock2 = new InventoryEntity("PROD-002", "Product 2", 8, 0, 8, status);

        when(inventoryRepository.findByStatus(status))
                .thenReturn(Arrays.asList(lowStock1, lowStock2));

        // Act
        List<InventoryEntity> result = inventoryService.getInventoryByStatus(status);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(status);
        assertThat(result.get(1).getStatus()).isEqualTo(status);

        verify(inventoryRepository, times(1)).findByStatus(status);
    }

    @Test
    void saveInventory_Success_ShouldSaveWithCorrectStatus() {
        // Arrange
        InventoryEntity inventoryToSave = new InventoryEntity(
                "PROD-NEW",
                "New Product",
                50,
                0,
                50,
                "UNKNOWN"
        );

        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryEntity result = inventoryService.saveInventory(inventoryToSave);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("IN_STOCK"); // Status should be updated to IN_STOCK

        verify(inventoryRepository, times(1)).save(any(InventoryEntity.class));
    }

    @Test
    void saveInventory_WithZeroQuantity_ShouldSetOutOfStock() {
        // Arrange
        InventoryEntity inventoryToSave = new InventoryEntity(
                "PROD-EMPTY",
                "Empty Product",
                0,
                0,
                0,
                "UNKNOWN"
        );

        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryEntity result = inventoryService.saveInventory(inventoryToSave);

        // Assert
        assertThat(result.getStatus()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    void saveInventory_WithLowQuantity_ShouldSetLowStock() {
        // Arrange
        InventoryEntity inventoryToSave = new InventoryEntity(
                "PROD-LOW",
                "Low Stock Product",
                7,
                0,
                7,
                "UNKNOWN"
        );

        when(inventoryRepository.save(any(InventoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InventoryEntity result = inventoryService.saveInventory(inventoryToSave);

        // Assert
        assertThat(result.getStatus()).isEqualTo("LOW_STOCK");
    }
}
