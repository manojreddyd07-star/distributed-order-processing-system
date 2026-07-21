import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import '@testing-library/jest-dom';
import Navigation from './Navigation';

describe('Navigation Component Tests', () => {
  const renderNavigation = () => {
    return render(
      <BrowserRouter>
        <Navigation />
      </BrowserRouter>
    );
  };

  test('should render all navigation links', () => {
    renderNavigation();

    expect(screen.getByText(/Dashboard/i)).toBeInTheDocument();
    expect(screen.getByText(/Orders/i)).toBeInTheDocument();
    expect(screen.getByText(/Validation/i)).toBeInTheDocument();
    expect(screen.getByText(/Payment/i)).toBeInTheDocument();
    expect(screen.getByText(/Inventory/i)).toBeInTheDocument();
    expect(screen.getByText(/Fulfillment/i)).toBeInTheDocument();
    expect(screen.getByText(/Monitoring/i)).toBeInTheDocument();
  });

  test('should navigate to different routes on click', () => {
    renderNavigation();

    const ordersLink = screen.getByText(/Orders/i);
    fireEvent.click(ordersLink);

    expect(window.location.pathname).toBe('/orders');
  });

  test('should highlight active link', () => {
    renderNavigation();

    const dashboardLink = screen.getByText(/Dashboard/i);
    expect(dashboardLink).toHaveClass('active');
  });

  test('should display all service menu items', () => {
    renderNavigation();

    expect(screen.getByText(/Retry/i)).toBeInTheDocument();
    expect(screen.getByText(/DLQ/i)).toBeInTheDocument();
    expect(screen.getByText(/Replay/i)).toBeInTheDocument();
    expect(screen.getByText(/Audit/i)).toBeInTheDocument();
    expect(screen.getByText(/Idempotency/i)).toBeInTheDocument();
  });
});
