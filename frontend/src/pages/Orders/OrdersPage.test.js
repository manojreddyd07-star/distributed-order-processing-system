import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import OrdersPage from './OrdersPage';
import * as orderApi from '../../services/orderApi';

// Mock the orderApi module
jest.mock('../../services/orderApi');

// Mock child components
jest.mock('../../components/orders/OrderTable', () => {
  return function MockOrderTable({ orders }) {
    return (
      <div data-testid="order-table">
        {orders.map(order => (
          <div key={order.id} data-testid={`order-${order.id}`}>
            Order {order.id}
          </div>
        ))}
      </div>
    );
  };
});

jest.mock('../../components/orders/OrderEventTimeline', () => {
  return function MockOrderEventTimeline() {
    return <div data-testid="order-event-timeline">Event Timeline</div>;
  };
});

jest.mock('../../components/orders/SearchFilters', () => {
  return function MockSearchFilters({ onSearch, onReset }) {
    return (
      <div data-testid="search-filters">
        <button onClick={() => onSearch({ customerId: 'CUST-001' })}>Search</button>
        <button onClick={onReset}>Reset</button>
      </div>
    );
  };
});

jest.mock('../../components/orders/Pagination', () => {
  return function MockPagination({ currentPage, totalPages, onPageChange, onPageSizeChange }) {
    return (
      <div data-testid="pagination">
        <button onClick={() => onPageChange(currentPage + 1)}>Next</button>
        <button onClick={() => onPageSizeChange(20)}>Change Size</button>
        <span>Page {currentPage} of {totalPages}</span>
      </div>
    );
  };
});

describe('OrdersPage Component', () => {
  
  const mockOrders = [
    { id: 1, customerId: 'CUST-001', orderStatus: 'PENDING', totalAmount: 99.99 },
    { id: 2, customerId: 'CUST-002', orderStatus: 'COMPLETED', totalAmount: 149.99 }
  ];

  const mockResponse = {
    content: mockOrders,
    totalPages: 1,
    totalElements: 2,
    page: 0,
    size: 10
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders orders page with header', () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    expect(screen.getByText('Orders')).toBeInTheDocument();
    expect(screen.getByText('Manage and view all orders in the system')).toBeInTheDocument();
  });

  test('fetches and displays orders on mount', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    // Should show loading initially
    expect(screen.getByText('Loading orders...')).toBeInTheDocument();
    
    // Wait for orders to load
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Verify API was called
    expect(orderApi.searchOrders).toHaveBeenCalledWith({
      page: 0,
      size: 10
    });
    
    // Verify orders are displayed
    expect(screen.getByTestId('order-1')).toBeInTheDocument();
    expect(screen.getByTestId('order-2')).toBeInTheDocument();
  });

  test('displays error message when fetch fails', async () => {
    orderApi.searchOrders.mockRejectedValue(new Error('Failed to load orders'));
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByText('Failed to load orders')).toBeInTheDocument();
    });
    
    // Should show retry button
    expect(screen.getByRole('button', { name: /Retry/i })).toBeInTheDocument();
  });

  test('retries fetching orders when retry button is clicked', async () => {
    orderApi.searchOrders
      .mockRejectedValueOnce(new Error('Failed to load orders'))
      .mockResolvedValueOnce(mockResponse);
    
    render(<OrdersPage />);
    
    // Wait for error
    await waitFor(() => {
      expect(screen.getByText('Failed to load orders')).toBeInTheDocument();
    });
    
    // Click retry button
    const retryButton = screen.getByRole('button', { name: /Retry/i });
    fireEvent.click(retryButton);
    
    // Wait for orders to load
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
  });

  test('displays empty message when no orders found', async () => {
    orderApi.searchOrders.mockResolvedValue({
      content: [],
      totalPages: 0,
      totalElements: 0,
      page: 0,
      size: 10
    });
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByText('No orders found')).toBeInTheDocument();
    });
  });

  test('applies search filters when search is triggered', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Trigger search with filters
    const searchButton = screen.getByText('Search');
    fireEvent.click(searchButton);
    
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        customerId: 'CUST-001',
        page: 0,
        size: 10
      });
    });
  });

  test('resets filters when reset is triggered', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Trigger reset
    const resetButton = screen.getByText('Reset');
    fireEvent.click(resetButton);
    
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        page: 0,
        size: 10
      });
    });
  });

  test('changes page when pagination is used', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Click next page
    const nextButton = screen.getByText('Next');
    fireEvent.click(nextButton);
    
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        page: 1,
        size: 10
      });
    });
  });

  test('changes page size when pagination size is changed', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Change page size
    const changeSizeButton = screen.getByText('Change Size');
    fireEvent.click(changeSizeButton);
    
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        page: 0,
        size: 20
      });
    });
  });

  test('displays search filters component', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    expect(screen.getByTestId('search-filters')).toBeInTheDocument();
  });

  test('displays order event timeline', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-event-timeline')).toBeInTheDocument();
    });
  });

  test('displays pagination component when orders are present', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('pagination')).toBeInTheDocument();
    });
  });

  test('resets to first page when new search is triggered', async () => {
    orderApi.searchOrders.mockResolvedValue(mockResponse);
    
    render(<OrdersPage />);
    
    await waitFor(() => {
      expect(screen.getByTestId('order-table')).toBeInTheDocument();
    });
    
    // Go to page 2
    const nextButton = screen.getByText('Next');
    fireEvent.click(nextButton);
    
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        page: 1,
        size: 10
      });
    });
    
    // Now trigger a new search
    const searchButton = screen.getByText('Search');
    fireEvent.click(searchButton);
    
    // Should reset to page 0
    await waitFor(() => {
      expect(orderApi.searchOrders).toHaveBeenCalledWith({
        customerId: 'CUST-001',
        page: 0,
        size: 10
      });
    });
  });
});
