import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import App from './App';

// Mock all page components
jest.mock('./components/Navigation/Navigation', () => {
  return function MockNavigation() {
    return <nav data-testid="navigation">Navigation</nav>;
  };
});

jest.mock('./pages/Dashboard/DashboardPage', () => {
  return function MockDashboardPage() {
    return <div data-testid="dashboard-page">Dashboard Page</div>;
  };
});

jest.mock('./pages/Orders/OrdersPage', () => {
  return function MockOrdersPage() {
    return <div data-testid="orders-page">Orders Page</div>;
  };
});

jest.mock('./pages/Validation/ValidationPage', () => {
  return function MockValidationPage() {
    return <div data-testid="validation-page">Validation Page</div>;
  };
});

jest.mock('./pages/Payment/PaymentPage', () => {
  return function MockPaymentPage() {
    return <div data-testid="payment-page">Payment Page</div>;
  };
});

jest.mock('./pages/Inventory/InventoryPage', () => {
  return function MockInventoryPage() {
    return <div data-testid="inventory-page">Inventory Page</div>;
  };
});

jest.mock('./pages/Fulfillment/FulfillmentPage', () => {
  return function MockFulfillmentPage() {
    return <div data-testid="fulfillment-page">Fulfillment Page</div>;
  };
});

jest.mock('./pages/Idempotency/IdempotencyPage', () => {
  return function MockIdempotencyPage() {
    return <div data-testid="idempotency-page">Idempotency Page</div>;
  };
});

jest.mock('./pages/Retry/RetryPage', () => {
  return function MockRetryPage() {
    return <div data-testid="retry-page">Retry Page</div>;
  };
});

jest.mock('./pages/DLQ/DLQPage', () => {
  return function MockDLQPage() {
    return <div data-testid="dlq-page">DLQ Page</div>;
  };
});

jest.mock('./pages/Replay/ReplayPage', () => {
  return function MockReplayPage() {
    return <div data-testid="replay-page">Replay Page</div>;
  };
});

jest.mock('./pages/Audit/AuditPage', () => {
  return function MockAuditPage() {
    return <div data-testid="audit-page">Audit Page</div>;
  };
});

jest.mock('./pages/Monitoring/MonitoringPage', () => {
  return function MockMonitoringPage() {
    return <div data-testid="monitoring-page">Monitoring Page</div>;
  };
});

describe('App Component - Routing', () => {
  
  test('renders navigation component', () => {
    window.history.pushState({}, 'Dashboard', '/dashboard');
    render(<App />);
    
    expect(screen.getByTestId('navigation')).toBeInTheDocument();
  });

  test('redirects root path to dashboard', () => {
    window.history.pushState({}, 'Root', '/');
    render(<App />);
    
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
  });

  test('renders dashboard page at /dashboard', () => {
    window.history.pushState({}, 'Dashboard', '/dashboard');
    render(<App />);
    
    expect(screen.getByTestId('dashboard-page')).toBeInTheDocument();
  });

  test('renders orders page at /orders', () => {
    window.history.pushState({}, 'Orders', '/orders');
    render(<App />);
    
    expect(screen.getByTestId('orders-page')).toBeInTheDocument();
  });

  test('renders validation page at /validations', () => {
    window.history.pushState({}, 'Validations', '/validations');
    render(<App />);
    
    expect(screen.getByTestId('validation-page')).toBeInTheDocument();
  });

  test('renders payment page at /payments', () => {
    window.history.pushState({}, 'Payments', '/payments');
    render(<App />);
    
    expect(screen.getByTestId('payment-page')).toBeInTheDocument();
  });

  test('renders inventory page at /inventory', () => {
    window.history.pushState({}, 'Inventory', '/inventory');
    render(<App />);
    
    expect(screen.getByTestId('inventory-page')).toBeInTheDocument();
  });

  test('renders fulfillment page at /fulfillment', () => {
    window.history.pushState({}, 'Fulfillment', '/fulfillment');
    render(<App />);
    
    expect(screen.getByTestId('fulfillment-page')).toBeInTheDocument();
  });

  test('renders idempotency page at /idempotency', () => {
    window.history.pushState({}, 'Idempotency', '/idempotency');
    render(<App />);
    
    expect(screen.getByTestId('idempotency-page')).toBeInTheDocument();
  });

  test('renders retry page at /retry', () => {
    window.history.pushState({}, 'Retry', '/retry');
    render(<App />);
    
    expect(screen.getByTestId('retry-page')).toBeInTheDocument();
  });

  test('renders DLQ page at /dlq', () => {
    window.history.pushState({}, 'DLQ', '/dlq');
    render(<App />);
    
    expect(screen.getByTestId('dlq-page')).toBeInTheDocument();
  });

  test('renders replay page at /replay', () => {
    window.history.pushState({}, 'Replay', '/replay');
    render(<App />);
    
    expect(screen.getByTestId('replay-page')).toBeInTheDocument();
  });

  test('renders audit page at /audit', () => {
    window.history.pushState({}, 'Audit', '/audit');
    render(<App />);
    
    expect(screen.getByTestId('audit-page')).toBeInTheDocument();
  });

  test('renders monitoring page at /monitoring', () => {
    window.history.pushState({}, 'Monitoring', '/monitoring');
    render(<App />);
    
    expect(screen.getByTestId('monitoring-page')).toBeInTheDocument();
  });

  test('has correct app structure', () => {
    window.history.pushState({}, 'Dashboard', '/dashboard');
    const { container } = render(<App />);
    
    const appDiv = container.querySelector('.app');
    expect(appDiv).toBeInTheDocument();
    
    const mainContent = container.querySelector('.main-content');
    expect(mainContent).toBeInTheDocument();
  });

  test('renders without crashing', () => {
    const { container } = render(<App />);
    expect(container).toBeInTheDocument();
  });
});
