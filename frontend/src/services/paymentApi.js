import { get } from '../shared/api/apiClient';

const API_BASE_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';

/**
 * Get all payments (payment history)
 * @returns {Promise<Array>} List of all payments
 */
export const getPaymentHistory = async () => {
  return get(API_BASE_URL, '/payments');
};

/**
 * Get a payment by ID
 * @param {number} paymentId - The payment ID
 * @returns {Promise<Object>} The payment response
 */
export const getPaymentById = async (paymentId) => {
  return get(API_BASE_URL, `/payments/${paymentId}`);
};

/**
 * Get payment by order ID
 * @param {string} orderId - The order ID
 * @returns {Promise<Object>} The payment response
 */
export const getPaymentByOrderId = async (orderId) => {
  return get(API_BASE_URL, `/payments/order/${orderId}`);
};

// Alias for consistency with tests
export const getAllPayments = getPaymentHistory;
