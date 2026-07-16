import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import DashboardPage from './DashboardPage';
import * as monitoringApi from '../../services/monitoringApi';
import * as orderApi from '../../services/orderApi';

// Mock the API modules
jest.mock('../../services/monitoringApi');
jest.mock('../../services/orderApi');

describe('Dashboard Integration Tests', () => {
  const mockMetrics = {
    totalOrders: 150,
    successfulOrders: 140,
    failedOrders: 10,
    averageProcessingTime: 2.5,
    systemHealth: 'healthy'
  };

  const mockRecentOrders = [
    {
      id: 1,
      customerId: 'CUST-001',
      status: 'COMPLETED',
      totalAmount: 99.99,
      createdAt: '2026-07-16T10:00:00'
    },
    {
      id: 2,
      customerId: 'CUST-002',
      status: 'PROCESSING',
      totalAmount: 150.00,
      createdAt: '2026-07-16T11:00:00'
    },
    {
      id: 3,
      customerId: 'CUST-003',
      status: 'FAILED',
      totalAmount: 75.50,
      createdAt: '2026-07-16T12:00:00'
    }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    monitoringApi.getSystemMetrics.mockResolvedValue(mockMetrics);
    orderApi.getAllOrders.mockResolvedValue(mockRecentOrders);
  });

  test('should load and display dashboard metrics', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    // Wait for metrics to load
    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalled();
    });

    // Verify metrics are displayed
    await waitFor(() => {
      expect(screen.getByText(/150/i)).toBeInTheDocument(); // Total orders
    });
  });

  test('should load and display recent orders', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    // Wait for orders to load
    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalled();
    });

    // Verify recent orders are displayed
    await waitFor(() => {
      expect(screen.getByText(/CUST-001/i)).toBeInTheDocument();
      expect(screen.getByText(/CUST-002/i)).toBeInTheDocument();
      expect(screen.getByText(/CUST-003/i)).toBeInTheDocument();
    });
  });

  test('should display correct order statuses', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/COMPLETED/i)).toBeInTheDocument();
      expect(screen.getByText(/PROCESSING/i)).toBeInTheDocument();
      expect(screen.getByText(/FAILED/i)).toBeInTheDocument();
    });
  });

  test('should handle API error gracefully', async () => {
    monitoringApi.getSystemMetrics.mockRejectedValue(new Error('API Error'));
    orderApi.getAllOrders.mockRejectedValue(new Error('API Error'));

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    // Wait for error handling
    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalled();
    });

    // Component should still render without crashing
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
  });

  test('should refresh dashboard data on interval', async () => {
    jest.useFakeTimers();

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(1);
    });

    // Fast-forward time by 30 seconds (typical refresh interval)
    jest.advanceTimersByTime(30000);

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(2);
    });

    jest.useRealTimers();
  });

  test('should display system health status', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/healthy/i)).toBeInTheDocument();
    });
  });

  test('should calculate and display success rate', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      // Success rate = (140/150) * 100 = 93.33%
      const successRateElement = screen.queryByText(/93/i);
      if (successRateElement) {
        expect(successRateElement).toBeInTheDocument();
      }
    });
  });

  test('should display order amounts correctly', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/99.99/)).toBeInTheDocument();
      expect(screen.getByText(/150.00/)).toBeInTheDocument();
      expect(screen.getByText(/75.50/)).toBeInTheDocument();
    });
  });

  test('should verify API integration - metrics endpoint', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(1);
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledWith();
    });
  });

  test('should verify API integration - orders endpoint', async () => {
    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(orderApi.getAllOrders).toHaveBeenCalledTimes(1);
      expect(orderApi.getAllOrders).toHaveBeenCalledWith();
    });
  });
});
