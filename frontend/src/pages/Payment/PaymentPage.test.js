import React from 'react';
import { render, screen } from '@testing-library/react';
import '@testing-library/jest-dom';
import PaymentPage from './PaymentPage';

// Mock the PaymentTable component
jest.mock('../../components/payment/PaymentTable', () => {
  return function MockPaymentTable() {
    return <div data-testid="payment-table">Payment Table</div>;
  };
});

describe('PaymentPage Component', () => {
  
  test('renders payment page with header', () => {
    render(<PaymentPage />);
    
    expect(screen.getByText('Payments')).toBeInTheDocument();
    expect(screen.getByText('View and monitor payment transactions')).toBeInTheDocument();
  });

  test('renders PaymentTable component', () => {
    render(<PaymentPage />);
    
    expect(screen.getByTestId('payment-table')).toBeInTheDocument();
  });

  test('has correct page structure', () => {
    const { container } = render(<PaymentPage />);
    
    const paymentPage = container.querySelector('.payment-page');
    expect(paymentPage).toBeInTheDocument();
    
    const header = container.querySelector('.payment-page-header');
    expect(header).toBeInTheDocument();
  });

  test('displays main heading as h1 element', () => {
    render(<PaymentPage />);
    
    const heading = screen.getByRole('heading', { name: /Payments/i });
    expect(heading).toBeInTheDocument();
    expect(heading.tagName).toBe('H1');
  });

  test('displays descriptive text', () => {
    render(<PaymentPage />);
    
    const description = screen.getByText('View and monitor payment transactions');
    expect(description).toBeInTheDocument();
  });

  test('renders without crashing', () => {
    const { container } = render(<PaymentPage />);
    expect(container).toBeInTheDocument();
  });

  test('component exports correctly', () => {
    expect(PaymentPage).toBeDefined();
  });
});
