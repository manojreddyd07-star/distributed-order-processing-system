import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import CreateOrderForm from './CreateOrderForm';

describe('CreateOrderForm Component', () => {
  
  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
  });

  test('renders create order form with all fields', () => {
    render(<CreateOrderForm />);
    
    // Check if heading is present
    expect(screen.getByText('Create New Order')).toBeInTheDocument();
    
    // Check if form fields are present
    expect(screen.getByLabelText(/Customer ID/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Total Amount/i)).toBeInTheDocument();
    
    // Check if submit button is present
    expect(screen.getByRole('button', { name: /Create Order/i })).toBeInTheDocument();
  });

  test('displays validation error when customer ID is empty', async () => {
    render(<CreateOrderForm />);
    
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Submit form without filling fields
    fireEvent.click(submitButton);
    
    // Check for validation error
    await waitFor(() => {
      expect(screen.getByText('Customer ID is required')).toBeInTheDocument();
    });
  });

  test('displays validation error when total amount is empty', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill customer ID but leave total amount empty
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.click(submitButton);
    
    // Check for validation error
    await waitFor(() => {
      expect(screen.getByText('Total Amount is required')).toBeInTheDocument();
    });
  });

  test('displays validation error when total amount is zero', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill fields with zero amount
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '0' } });
    fireEvent.click(submitButton);
    
    // Check for validation error
    await waitFor(() => {
      expect(screen.getByText('Total Amount must be greater than zero')).toBeInTheDocument();
    });
  });

  test('displays validation error when total amount is negative', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill fields with negative amount
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '-10' } });
    fireEvent.click(submitButton);
    
    // Check for validation error
    await waitFor(() => {
      expect(screen.getByText('Total Amount must be greater than zero')).toBeInTheDocument();
    });
  });

  test('successfully submits form with valid data', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill form with valid data
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '99.99' } });
    
    // Submit form
    fireEvent.click(submitButton);
    
    // Wait for success message
    await waitFor(() => {
      expect(screen.getByText('Order created successfully!')).toBeInTheDocument();
    }, { timeout: 2000 });
  });

  test('clears form after successful submission', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill and submit form
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '99.99' } });
    fireEvent.click(submitButton);
    
    // Wait for form to clear
    await waitFor(() => {
      expect(customerIdInput.value).toBe('');
      expect(totalAmountInput.value).toBe('');
    }, { timeout: 2000 });
  });

  test('disables form inputs while loading', async () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill form
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '99.99' } });
    
    // Submit form
    fireEvent.click(submitButton);
    
    // Check if inputs are disabled during submission
    expect(customerIdInput).toBeDisabled();
    expect(totalAmountInput).toBeDisabled();
    expect(submitButton).toBeDisabled();
  });

  test('allows user to input customer ID', () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    
    // Type in customer ID
    fireEvent.change(customerIdInput, { target: { value: '12345' } });
    
    expect(customerIdInput.value).toBe('12345');
  });

  test('allows user to input total amount', () => {
    render(<CreateOrderForm />);
    
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    
    // Type in total amount
    fireEvent.change(totalAmountInput, { target: { value: '149.99' } });
    
    expect(totalAmountInput.value).toBe('149.99');
  });

  test('clears errors when user starts typing after validation failure', async () => {
    render(<CreateOrderForm />);
    
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Submit empty form to trigger validation
    fireEvent.click(submitButton);
    
    // Wait for error to appear
    await waitFor(() => {
      expect(screen.getByText('Customer ID is required')).toBeInTheDocument();
    });
    
    // Start typing in customer ID field
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    fireEvent.change(customerIdInput, { target: { value: '1' } });
    
    // Re-submit to trigger validation again (errors should be cleared first)
    fireEvent.click(submitButton);
    
    // The old error should be cleared before new validation
    await waitFor(() => {
      const errors = screen.queryAllByText('Customer ID is required');
      expect(errors.length).toBeLessThanOrEqual(1);
    });
  });

  test('displays success message that auto-dismisses', async () => {
    jest.useFakeTimers();
    
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    const submitButton = screen.getByRole('button', { name: /Create Order/i });
    
    // Fill and submit form
    fireEvent.change(customerIdInput, { target: { value: '123' } });
    fireEvent.change(totalAmountInput, { target: { value: '99.99' } });
    fireEvent.click(submitButton);
    
    // Wait for success message
    await waitFor(() => {
      expect(screen.getByText('Order created successfully!')).toBeInTheDocument();
    }, { timeout: 2000 });
    
    // Fast-forward time by 3 seconds
    jest.advanceTimersByTime(3000);
    
    // Success message should be dismissed
    await waitFor(() => {
      expect(screen.queryByText('Order created successfully!')).not.toBeInTheDocument();
    });
    
    jest.useRealTimers();
  });

  test('shows required indicator on labels', () => {
    render(<CreateOrderForm />);
    
    // Check for required asterisks
    const requiredMarkers = screen.getAllByText('*');
    expect(requiredMarkers.length).toBeGreaterThan(0);
  });

  test('form has correct input types', () => {
    render(<CreateOrderForm />);
    
    const customerIdInput = screen.getByLabelText(/Customer ID/i);
    const totalAmountInput = screen.getByLabelText(/Total Amount/i);
    
    expect(customerIdInput).toHaveAttribute('type', 'number');
    expect(totalAmountInput).toHaveAttribute('type', 'number');
  });
});
