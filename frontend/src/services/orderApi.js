import { cachedApiCall } from '../shared/utils/apiUtils';
import { get, post } from '../shared/api/apiClient';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Create a new order
 * @param {Object} orderData - The order data (customerId, totalAmount)
 * @returns {Promise<Object>} The created order response
 */
export const createOrder = async (orderData) => {
  return post(API_BASE_URL, '/orders', orderData);
};

/**
 * Get all orders (with caching)
 * @returns {Promise<Array>} List of all orders
 */
export const getAllOrders = async () => {
  return cachedApiCall(
    'orders:all',
    () => get(API_BASE_URL, '/orders')
  );
};

/**
 * Get an order by ID (with caching)
 * @param {number} orderId - The order ID
 * @returns {Promise<Object>} The order response
 */
export const getOrderById = async (orderId) => {
  return cachedApiCall(
    `orders:${orderId}`,
    () => get(API_BASE_URL, `/orders/${orderId}`)
  );
};

/**
 * Search orders with filters and pagination
 * Note: Search results are cached based on query params
 */
export const searchOrders = async (searchParams = {}, signal = null) => {
  const params = new URLSearchParams();
  
  if (searchParams.customerId) {
    params.append('customerId', searchParams.customerId);
  }
  if (searchParams.orderStatus) {
    params.append('orderStatus', searchParams.orderStatus);
  }
  if (searchParams.startDate) {
    params.append('startDate', searchParams.startDate);
  }
  if (searchParams.endDate) {
    params.append('endDate', searchParams.endDate);
  }
  
  params.append('page', searchParams.page || 0);
  params.append('size', searchParams.size || 10);
  params.append('sortBy', searchParams.sortBy || 'createdAt');
  params.append('sortDirection', searchParams.sortDirection || 'DESC');
  
  const queryString = params.toString();
  const cacheKey = `orders:search:${queryString}`;
  
  return cachedApiCall(
    cacheKey,
    () => get(API_BASE_URL, `/orders/search?${queryString}`, { signal }),
    true
  );
};
