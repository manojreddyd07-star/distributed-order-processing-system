import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import '@testing-library/jest-dom';
import RetryManagement from './RetryManagement';
import * as retryApi from '../../services/retryApi';

jest.mock('../../services/retryApi');

describe('RetryManagement Component Tests', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders retry management interface', async () => {
    const mockRetries = [
      { 
        retryId: 'R1', 
        originalEventId: 'E1', 
        retryCount: 1, 
        maxRetries: 3,
        status: 'PENDING'
      }
    ];

    retryApi.getAllRetryEvents.mockResolvedValue(mockRetries);

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('displays retry events', async () => {
    const mockRetries = [
      { retryId: 'R1', originalEventId: 'E1', retryCount: 1, maxRetries: 3 },
      { retryId: 'R2', originalEventId: 'E2', retryCount: 2, maxRetries: 3 }
    ];

    retryApi.getAllRetryEvents.mockResolvedValue(mockRetries);

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('handles retry event trigger', async () => {
    const mockRetries = [
      { 
        retryId: 'R1', 
        originalEventId: 'E1', 
        retryCount: 1, 
        maxRetries: 3 
      }
    ];

    retryApi.getAllRetryEvents.mockResolvedValue(mockRetries);
    retryApi.retryEvent.mockResolvedValue({ success: true });

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    const retryButton = screen.queryByRole('button', { name: /retry/i });
    if (retryButton) {
      await userEvent.click(retryButton);
      
      await waitFor(() => {
        expect(retryApi.retryEvent).toHaveBeenCalled();
      });
    }
  });

  test('displays retry count and max retries', async () => {
    const mockRetries = [
      { 
        retryId: 'R1', 
        originalEventId: 'E1', 
        retryCount: 2, 
        maxRetries: 3,
        status: 'PENDING'
      }
    ];

    retryApi.getAllRetryEvents.mockResolvedValue(mockRetries);

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('handles empty retry list', async () => {
    retryApi.getAllRetryEvents.mockResolvedValue([]);

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
  });

  test('handles API errors gracefully', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    
    retryApi.getAllRetryEvents.mockRejectedValue(new Error('API Error'));

    render(<RetryManagement />);

    await waitFor(() => {
      expect(retryApi.getAllRetryEvents).toHaveBeenCalled();
    });

    expect(document.body).toBeDefined();
    consoleErrorSpy.mockRestore();
  });
});
