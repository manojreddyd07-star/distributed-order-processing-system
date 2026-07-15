import React from 'react';
import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import InventoryPage from './InventoryPage';
import * as inventoryApi from '../../shared/api/inventoryApi';

// Mock the inventoryApi module
jest.mock('../../shared/api/inventoryApi');

// Mock the InventoryTable component
jest.mock('../../components/inventory/InventoryTable', () => {
  return function MockInventoryTable({ inventory, loading, onRefresh }) {
    if (loading) {
      return <div data-testid="inventory-table">Loading...</div>;
    }
    return (
      <div data-testid="inventory-table">
        {inventory.map(item => (
          <div key={item.id} data-testid={`inventory-${item.id}`}>
            {item.productId} - {item.status}
          </div>
        ))}
        <button onClick={onRefresh}>Refresh</button>
      </div>
    );
  };
});

describe('InventoryPage Component', () => {
  
  const mockInventory = [
    { id: 1, productId: 'PROD-001', productName: 'Product 1', availableQuantity: 100, status: 'IN_STOCK' },
    { id: 2, productId: 'PROD-002', productName: 'Product 2', availableQuantity: 5, status: 'LOW_STOCK' },
    { id: 3, productId: 'PROD-003', productName: 'Product 3', availableQuantity: 0, status: 'OUT_OF_STOCK' }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders inventory page with header', () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    expect(screen.getByText('Inventory')).toBeInTheDocument();
    expect(screen.getByText('Manage and view product inventory levels')).toBeInTheDocument();
  });

  test('fetches and displays inventory on mount', async () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
    
    // Verify API was called
    expect(inventoryApi.getAllInventory).toHaveBeenCalledTimes(1);
    
    // Verify inventory items are displayed
    expect(screen.getByTestId('inventory-1')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-2')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-3')).toBeInTheDocument();
  });

  test('displays success notification after loading inventory', async () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    await waitFor(() => {
      expect(screen.getByText('✅ Inventory data loaded successfully')).toBeInTheDocument();
    });
  });

  test('displays error message when fetch fails', async () => {
    inventoryApi.getAllInventory.mockRejectedValue(new Error('Failed to load'));
    
    render(<InventoryPage />);
    
    await waitFor(() => {
      expect(screen.getByText('Failed to load inventory data. Please try again.')).toBeInTheDocument();
    });
    
    // Should show error notification
    expect(screen.getByText('❌ Failed to load inventory data. Please try again.')).toBeInTheDocument();
    
    // Should show retry button
    expect(screen.getByRole('button', { name: /Retry/i })).toBeInTheDocument();
  });

  test('retries fetching inventory when retry button is clicked', async () => {
    inventoryApi.getAllInventory
      .mockRejectedValueOnce(new Error('Failed to load'))
      .mockResolvedValueOnce(mockInventory);
    
    render(<InventoryPage />);
    
    // Wait for error
    await waitFor(() => {
      expect(screen.getByText('Failed to load inventory data. Please try again.')).toBeInTheDocument();
    });
    
    // Click retry button
    const retryButton = screen.getByRole('button', { name: /Retry/i });
    fireEvent.click(retryButton);
    
    // Wait for inventory to load
    await waitFor(() => {
      expect(screen.getByTestId('inventory-1')).toBeInTheDocument();
    });
  });

  test('displays status filter dropdown', () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    expect(screen.getByText('Filter by Status:')).toBeInTheDocument();
    
    const filterSelect = screen.getByRole('combobox');
    expect(filterSelect).toBeInTheDocument();
  });

  test('filters inventory by status when filter is changed', async () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    // Wait for inventory to load
    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
    
    // All items should be visible initially
    expect(screen.getByTestId('inventory-1')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-2')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-3')).toBeInTheDocument();
    
    // Change filter to LOW_STOCK
    const filterSelect = screen.getByRole('combobox');
    fireEvent.change(filterSelect, { target: { value: 'LOW_STOCK' } });
    
    // Only LOW_STOCK item should be visible
    await waitFor(() => {
      expect(screen.queryByTestId('inventory-1')).not.toBeInTheDocument();
      expect(screen.getByTestId('inventory-2')).toBeInTheDocument();
      expect(screen.queryByTestId('inventory-3')).not.toBeInTheDocument();
    });
  });

  test('shows all inventory when filter is set to ALL', async () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    // Wait for inventory to load
    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
    
    const filterSelect = screen.getByRole('combobox');
    
    // Change to specific status
    fireEvent.change(filterSelect, { target: { value: 'LOW_STOCK' } });
    
    // Change back to ALL
    fireEvent.change(filterSelect, { target: { value: 'ALL' } });
    
    // All items should be visible
    expect(screen.getByTestId('inventory-1')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-2')).toBeInTheDocument();
    expect(screen.getByTestId('inventory-3')).toBeInTheDocument();
  });

  test('refreshes inventory when refresh button is clicked', async () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    // Wait for initial load
    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
    
    // Click refresh button
    const refreshButton = screen.getByRole('button', { name: /Refresh/i });
    fireEvent.click(refreshButton);
    
    // API should be called again
    await waitFor(() => {
      expect(inventoryApi.getAllInventory).toHaveBeenCalledTimes(2);
    });
    
    // Should show refresh success notification
    await waitFor(() => {
      expect(screen.getByText('✅ Inventory data refreshed successfully')).toBeInTheDocument();
    });
  });

  test('displays error notification when refresh fails', async () => {
    inventoryApi.getAllInventory
      .mockResolvedValueOnce(mockInventory)
      .mockRejectedValueOnce(new Error('Refresh failed'));
    
    render(<InventoryPage />);
    
    // Wait for initial load
    await waitFor(() => {
      expect(screen.queryByText('Loading...')).not.toBeInTheDocument();
    });
    
    // Click refresh button
    const refreshButton = screen.getByRole('button', { name: /Refresh/i });
    fireEvent.click(refreshButton);
    
    // Should show error notification
    await waitFor(() => {
      expect(screen.getByText('❌ Failed to refresh inventory data')).toBeInTheDocument();
    });
  });

  test('notification auto-dismisses after 4 seconds', async () => {
    jest.useFakeTimers();
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    await waitFor(() => {
      expect(screen.getByText('✅ Inventory data loaded successfully')).toBeInTheDocument();
    });
    
    // Fast-forward time by 4 seconds
    act(() => {
      jest.advanceTimersByTime(4000);
    });
    
    // Notification should be dismissed
    await waitFor(() => {
      expect(screen.queryByText('✅ Inventory data loaded successfully')).not.toBeInTheDocument();
    });
    
    jest.useRealTimers();
  });

  test('has correct page structure', () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    const { container } = render(<InventoryPage />);
    
    const inventoryPage = container.querySelector('.inventory-page');
    expect(inventoryPage).toBeInTheDocument();
    
    const header = container.querySelector('.inventory-page-header');
    expect(header).toBeInTheDocument();
  });

  test('displays all filter options', () => {
    inventoryApi.getAllInventory.mockResolvedValue(mockInventory);
    
    render(<InventoryPage />);
    
    const filterSelect = screen.getByRole('combobox');
    const options = Array.from(filterSelect.options).map(opt => opt.value);
    
    expect(options).toContain('ALL');
    expect(options).toContain('IN_STOCK');
    expect(options).toContain('LOW_STOCK');
    expect(options).toContain('OUT_OF_STOCK');
  });
});
