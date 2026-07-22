const API_BASE_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';

/**
 * Get all payments (payment history)
 * @returns {Promise<Array>} List of all payments
 */
export const getPaymentHistory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/payments`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch payment history' }));
      throw new Error(error.message || 'Failed to fetch payment history');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching payment history:', error);
    throw error;
  }
};

/**
 * Get a payment by ID
 * @param {number} paymentId - The payment ID
 * @returns {Promise<Object>} The payment response
 */
export const getPaymentById = async (paymentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/payments/${paymentId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch payment' }));
      throw new Error(error.message || 'Failed to fetch payment');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching payment:', error);
    throw error;
  }
};

/**
 * Get payment by order ID
 * @param {string} orderId - The order ID
 * @returns {Promise<Object>} The payment response
 */
export const getPaymentByOrderId = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/payments/order/${orderId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch payment for order' }));
      throw new Error(error.message || 'Failed to fetch payment for order');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching payment for order:', error);
    throw error;
  }
};
// Alias for consistency with tests
export const getAllPayments = getPaymentHistory;
