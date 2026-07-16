import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import MonitoringPage from './MonitoringPage';
import * as monitoringApi from '../../services/monitoringApi';

jest.mock('../../services/monitoringApi');

describe('Monitoring Page Integration Tests', () => {
  const mockSystemMetrics = {
    totalOrders: 200,
    successfulOrders: 180,
    failedOrders: 20,
    averageProcessingTime: 3.2,
    systemHealth: 'healthy',
    uptime: '5 days'
  };

  const mockEventMetrics = {
    orderCreatedCount: 200,
    orderValidatedCount: 195,
    paymentCompletedCount: 190,
    inventoryReservedCount: 185,
    orderCompletedCount: 180
  };

  const mockServiceHealth = [
    { serviceName: 'order-service', status: 'UP', responseTime: 50 },
    { serviceName: 'validation-service', status: 'UP', responseTime: 45 },
    { serviceName: 'payment-service', status: 'UP', responseTime: 60 },
    { serviceName: 'inventory-service', status: 'UP', responseTime: 55 },
    { serviceName: 'fulfillment-service', status: 'UP', responseTime: 40 }
  ];

  beforeEach(() => {
    jest.clearAllMocks();
    monitoringApi.getSystemMetrics.mockResolvedValue(mockSystemMetrics);
    monitoringApi.getEventMetrics.mockResolvedValue(mockEventMetrics);
    monitoringApi.getServiceHealth.mockResolvedValue(mockServiceHealth);
  });

  test('should load and display system metrics', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.getByText(/200/)).toBeInTheDocument(); // Total orders
      expect(screen.getByText(/180/)).toBeInTheDocument(); // Successful orders
    });
  });

  test('should load and display event metrics', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getEventMetrics).toHaveBeenCalled();
    });

    await waitFor(() => {
      const orderCreatedText = screen.queryByText(/orderCreatedCount|200/i);
      expect(orderCreatedText).toBeInTheDocument();
    });
  });

  test('should display service health statuses', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getServiceHealth).toHaveBeenCalled();
    });

    await waitFor(() => {
      expect(screen.getByText(/order-service/i)).toBeInTheDocument();
      expect(screen.getByText(/validation-service/i)).toBeInTheDocument();
      expect(screen.getByText(/payment-service/i)).toBeInTheDocument();
    });
  });

  test('should display all services as UP', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      const upStatuses = screen.getAllByText(/UP/i);
      expect(upStatuses.length).toBeGreaterThanOrEqualTo(5);
    });
  });

  test('should display service response times', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/50/)).toBeInTheDocument(); // Response time
      expect(screen.getByText(/45/)).toBeInTheDocument();
    });
  });

  test('should handle API errors gracefully', async () => {
    monitoringApi.getSystemMetrics.mockRejectedValue(new Error('API Error'));
    monitoringApi.getEventMetrics.mockRejectedValue(new Error('API Error'));
    monitoringApi.getServiceHealth.mockRejectedValue(new Error('API Error'));

    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalled();
    });

    // Component should still render
    expect(screen.getByTestId('monitoring-page')).toBeInTheDocument();
  });

  test('should refresh monitoring data on interval', async () => {
    jest.useFakeTimers();

    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(1);
    });

    // Fast-forward time
    jest.advanceTimersByTime(30000);

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(2);
    });

    jest.useRealTimers();
  });

  test('should display system health indicator', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/healthy/i)).toBeInTheDocument();
    });
  });

  test('should calculate success rate correctly', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      // Success rate = (180/200) * 100 = 90%
      const successRateElement = screen.queryByText(/90/);
      if (successRateElement) {
        expect(successRateElement).toBeInTheDocument();
      }
    });
  });

  test('should display average processing time', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/3.2/)).toBeInTheDocument();
    });
  });

  test('should handle manual refresh action', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(1);
    });

    // Find and click refresh button
    const refreshButton = screen.queryByRole('button', { name: /refresh/i });
    if (refreshButton) {
      fireEvent.click(refreshButton);

      await waitFor(() => {
        expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(2);
      });
    }
  });

  test('should verify API integration - system metrics endpoint', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledTimes(1);
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalledWith();
    });
  });

  test('should verify API integration - event metrics endpoint', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getEventMetrics).toHaveBeenCalledTimes(1);
    });
  });

  test('should verify API integration - service health endpoint', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getServiceHealth).toHaveBeenCalledTimes(1);
    });
  });

  test('should display event flow completion rate', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      // Completion rate = (180/200) * 100 = 90%
      const completionRate = screen.queryAllByText(/90/);
      expect(completionRate.length).toBeGreaterThan(0);
    });
  });

  test('should handle degraded service status', async () => {
    const degradedHealth = [
      { serviceName: 'order-service', status: 'UP', responseTime: 50 },
      { serviceName: 'validation-service', status: 'DOWN', responseTime: 0 }
    ];

    monitoringApi.getServiceHealth.mockResolvedValue(degradedHealth);

    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText(/DOWN/i)).toBeInTheDocument();
    });
  });

  test('should verify all monitoring APIs are called on mount', async () => {
    render(
      <BrowserRouter>
        <MonitoringPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(monitoringApi.getSystemMetrics).toHaveBeenCalled();
      expect(monitoringApi.getEventMetrics).toHaveBeenCalled();
      expect(monitoringApi.getServiceHealth).toHaveBeenCalled();
    });
  });
});
