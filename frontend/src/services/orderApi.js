import { cachedApiCall } from '../shared/utils/apiUtils';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Common fetch wrapper with error handling and abort signal support
 */
const fetchWithErrorHandling = async (url, options = {}) => {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      let error;
      try {
        error = await response.json();
      } catch {
        error = { message: `HTTP ${response.status}: ${response.statusText}` };
      }
      throw new Error(error.message || 'Request failed');
    }

    return await response.json();
  } catch (error) {
    // Re-throw abort errors as-is
    if (error.name === 'AbortError') {
      throw error;
    }
    console.error('API Error:', error);
    throw error;
  }
};

/**
 * Create a new order
 * @param {Object} orderData - The order data (customerId, totalAmount)
 * @returns {Promise<Object>} The created order response
 */
export const createOrder = async (orderData) => {
  return fetchWithErrorHandling(`${API_BASE_URL}/orders`, {
    method: 'POST',
    body: JSON.stringify(orderData),
  });
};

/**
 * Get all orders (with caching)
 * @returns {Promise<Array>} List of all orders
 */
export const getAllOrders = async () => {
  return cachedApiCall(
    'orders:all',
    () => fetchWithErrorHandling(`${API_BASE_URL}/orders`)
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
    () => fetchWithErrorHandling(`${API_BASE_URL}/orders/${orderId}`)
  );
};

/**
 * Search orders with filters and pagination
 * Note: Search results are cached based on query params
 * @param {Object} searchParams - Search parameters
 * @param {number} searchParams.customerId - Filter by customer ID (optional)
 * @param {string} searchParams.orderStatus - Filter by order status (optional)
 * @param {string} searchParams.startDate - Filter by start date in ISO format (optional)
 * @param {string} searchParams.endDate - Filter by end date in ISO format (optional)
 * @param {number} searchParams.page - Page number (default: 0)
 * @param {number} searchParams.size - Page size (default: 10)
 * @param {string} searchParams.sortBy - Sort field (default: createdAt)
 * @param {string} searchParams.sortDirection - Sort direction ASC/DESC (default: DESC)
 * @param {AbortSignal} signal - Optional abort signal for request cancellation
 * @returns {Promise<Object>} Paginated order response
 */
export const searchOrders = async (searchParams = {}, signal = null) => {
  const params = new URLSearchParams();
  
  // Add search parameters
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
  
  // Add pagination parameters
  params.append('page', searchParams.page || 0);
  params.append('size', searchParams.size || 10);
  params.append('sortBy', searchParams.sortBy || 'createdAt');
  params.append('sortDirection', searchParams.sortDirection || 'DESC');
  
  const queryString = params.toString();
  const cacheKey = `orders:search:${queryString}`;
  
  return cachedApiCall(
    cacheKey,
    () => fetchWithErrorHandling(`${API_BASE_URL}/orders/search?${queryString}`, { signal }),
    // Cache search results for 30 seconds only
    true
  );
};
