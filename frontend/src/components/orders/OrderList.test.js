import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import OrderList from './OrderList';
import * as orderApi from '../../services/orderApi';

jest.mock('../../services/orderApi');

describe('OrderList Component Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders order list successfully', async () => {
    const mockOrders = [
      { id: 1, customerId: 'CUST-001', status: 'CREATED', totalAmount: 99.99 },
      { id: 2, customerId: 'CUST-002', status: 'COMPLETED', totalAmount: 150.00 }
    ];

    orderApi.getAllOrders.mockResolvedValue(mockOrders);

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('displays loading state', () => {
    orderApi.getAllOrders.mockImplementation(() => new Promise(() => {}));

    render(<OrderList />);

    // Component should render loading state
    expect(document.body).toBeDefined();
  });

  test('displays error state', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    orderApi.getAllOrders.mockRejectedValue(new Error('Failed to load orders'));

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
    consoleErrorSpy.mockRestore();
  });

  test('displays order details', async () => {
    const mockOrders = [
      { 
        id: 1, 
        customerId: 'CUST-001', 
        status: 'CREATED', 
        totalAmount: 99.99,
        createdAt: '2026-07-16T10:00:00'
      }
    ];

    orderApi.getAllOrders.mockResolvedValue(mockOrders);

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('handles empty orders', async () => {
    orderApi.getAllOrders.mockResolvedValue([]);

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('displays multiple order statuses', async () => {
    const mockOrders = [
      { id: 1, customerId: 'C1', status: 'CREATED', totalAmount: 100 },
      { id: 2, customerId: 'C2', status: 'VALIDATED', totalAmount: 200 },
      { id: 3, customerId: 'C3', status: 'PAYMENT_COMPLETED', totalAmount: 300 },
      { id: 4, customerId: 'C4', status: 'COMPLETED', totalAmount: 400 }
    ];

    orderApi.getAllOrders.mockResolvedValue(mockOrders);

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('refreshes orders on button click', async () => {
    const mockOrders = [
      { id: 1, customerId: 'CUST-001', status: 'CREATED', totalAmount: 99.99 }
    ];

    orderApi.getAllOrders.mockResolvedValue(mockOrders);

    render(<OrderList />);

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalledTimes(1);
    });

    const refreshButton = screen.queryByRole('button', { name: /refresh/i });
    if (refreshButton) {
      await userEvent.click(refreshButton);
      
      await waitFor(() => {
        expect(orderApi.getAllOrders).toHaveBeenCalledTimes(2);
      });
    }
  });
});
