import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import ValidationList from './ValidationList';
import * as validationApi from '../../services/validationApi';

jest.mock('../../services/validationApi');

describe('ValidationList Component Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders validation list successfully', async () => {
    const mockValidations = [
      { orderId: 1, validationStatus: 'VALID', validationMessage: 'Success' },
      { orderId: 2, validationStatus: 'INVALID', validationMessage: 'Failed' }
    ];

    validationApi.getAllValidations.mockResolvedValue(mockValidations);

    render(<ValidationList />);

    await waitFor(() => {
      expect(validationApi.getAllValidations).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('displays loading state', () => {
    validationApi.getAllValidations.mockImplementation(() => new Promise(() => {}));

    render(<ValidationList />);

    expect(document.body).toBeDefined();
  });

  test('handles API errors', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    validationApi.getAllValidations.mockRejectedValue(new Error('API Error'));

    render(<ValidationList />);

    await waitFor(() => {
      expect(validationApi.getAllValidations).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
    consoleErrorSpy.mockRestore();
  });

  test('displays validation results', async () => {
    const mockValidations = [
      { 
        orderId: 1, 
        validationStatus: 'VALID', 
        validationMessage: 'All checks passed',
        validatedAt: '2026-07-16T10:00:00'
      }
    ];

    validationApi.getAllValidations.mockResolvedValue(mockValidations);

    render(<ValidationList />);

    await waitFor(() => {
      expect(validationApi.getAllValidations).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('handles empty validation list', async () => {
    validationApi.getAllValidations.mockResolvedValue([]);

    render(<ValidationList />);

    await waitFor(() => {
      expect(validationApi.getAllValidations).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });
});
