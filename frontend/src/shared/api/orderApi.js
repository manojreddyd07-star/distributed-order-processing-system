const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Create a new order
 * @param {Object} orderData - The order data (customerId, totalAmount)
 * @returns {Promise<Object>} The created order response
 */
export const createOrder = async (orderData) => {
  try {
    const response = await fetch(`${API_BASE_URL}/orders`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(orderData),
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to create order');
    }

    return await response.json();
  } catch (error) {
    console.error('Error creating order:', error);
    throw error;
  }
};

/**
 * Get all orders
 * @returns {Promise<Array>} List of all orders
 */
export const getAllOrders = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/orders`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch orders');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching orders:', error);
    throw error;
  }
};

/**
 * Get order by ID
 * @param {number} orderId - The order ID
 * @returns {Promise<Object>} The order details
 */
export const getOrderById = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/orders/${orderId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch order');
    }

    return await response.json();
  } catch (error) {
    console.error(`Error fetching order ${orderId}:`, error);
    throw error;
  }
};

/**
 * Search orders with filters and pagination
 * @param {Object} searchParams - Search parameters
 * @param {number} searchParams.customerId - Filter by customer ID (optional)
 * @param {string} searchParams.orderStatus - Filter by order status (optional)
 * @param {string} searchParams.startDate - Filter by start date in ISO format (optional)
 * @param {string} searchParams.endDate - Filter by end date in ISO format (optional)
 * @param {number} searchParams.page - Page number (default: 0)
 * @param {number} searchParams.size - Page size (default: 10)
 * @param {string} searchParams.sortBy - Sort field (default: createdAt)
 * @param {string} searchParams.sortDirection - Sort direction ASC/DESC (default: DESC)
 * @returns {Promise<Object>} Paginated order response
 */
export const searchOrders = async (searchParams = {}) => {
  try {
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
    
    const response = await fetch(`${API_BASE_URL}/orders/search?${params}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to search orders');
    }

    return await response.json();
  } catch (error) {
    console.error('Error searching orders:', error);
    throw error;
  }
};
