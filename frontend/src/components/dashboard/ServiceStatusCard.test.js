import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ServiceStatusCard from './ServiceStatusCard';

describe('ServiceStatusCard Component Tests', () => {
  const mockService = {
    name: 'Order Service',
    status: 'healthy',
    uptime: '99.9%',
    lastCheck: '2026-07-16T10:00:00'
  };

  test('should render service information correctly', () => {
    render(<ServiceStatusCard service={mockService} />);

    expect(screen.getByText('Order Service')).toBeInTheDocument();
    expect(screen.getByText(/99.9%/i)).toBeInTheDocument();
    expect(screen.getByText(/healthy/i)).toBeInTheDocument();
  });

  test('should display healthy status with green indicator', () => {
    render(<ServiceStatusCard service={mockService} />);

    const statusElement = screen.getByText(/healthy/i);
    expect(statusElement).toHaveClass('status-healthy');
  });

  test('should display unhealthy status with red indicator', () => {
    const unhealthyService = { ...mockService, status: 'unhealthy' };
    render(<ServiceStatusCard service={unhealthyService} />);

    const statusElement = screen.getByText(/unhealthy/i);
    expect(statusElement).toHaveClass('status-unhealthy');
  });

  test('should display warning status with yellow indicator', () => {
    const warningService = { ...mockService, status: 'degraded' };
    render(<ServiceStatusCard service={warningService} />);

    const statusElement = screen.getByText(/degraded/i);
    expect(statusElement).toHaveClass('status-degraded');
  });

  test('should format uptime correctly', () => {
    render(<ServiceStatusCard service={mockService} />);

    expect(screen.getByText(/Uptime:/i)).toBeInTheDocument();
    expect(screen.getByText(/99.9%/i)).toBeInTheDocument();
  });

  test('should display last check timestamp', () => {
    render(<ServiceStatusCard service={mockService} />);

    expect(screen.getByText(/Last Check:/i)).toBeInTheDocument();
  });

  test('should handle missing uptime gracefully', () => {
    const serviceWithoutUptime = { ...mockService, uptime: null };
    render(<ServiceStatusCard service={serviceWithoutUptime} />);

    expect(screen.getByText('Order Service')).toBeInTheDocument();
    expect(screen.queryByText(/99.9%/i)).not.toBeInTheDocument();
  });
});
