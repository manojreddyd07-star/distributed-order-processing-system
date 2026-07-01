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
