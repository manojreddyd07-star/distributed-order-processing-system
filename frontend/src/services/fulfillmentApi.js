// Fulfillment API Service
const API_BASE_URL = process.env.REACT_APP_FULFILLMENT_API_URL || 'http://localhost:8084/api/fulfillments';

/**
 * Get all fulfillments
 * @returns {Promise<Array>} List of fulfillments
 */
export const getAllFulfillments = async () => {
  try {
    const response = await fetch(API_BASE_URL);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching fulfillments:', error);
    throw error;
  }
};

/**
 * Get fulfillment by ID
 * @param {number|string} id - Fulfillment ID
 * @returns {Promise<Object>} Fulfillment details
 */
export const getFulfillmentById = async (id) => {
  try {
    const response = await fetch(`${API_BASE_URL}/${id}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching fulfillment ${id}:`, error);
    throw error;
  }
};

/**
 * Get fulfillments by order ID
 * @param {number|string} orderId - Order ID
 * @returns {Promise<Array>} List of fulfillments for the order
 */
export const getFulfillmentsByOrderId = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/order/${orderId}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching fulfillments for order ${orderId}:`, error);
    throw error;
  }
};

/**
 * Get fulfillment history
 * @returns {Promise<Array>} List of all fulfillments with history
 */
export const getFulfillmentHistory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/history`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error('Error fetching fulfillment history:', error);
    throw error;
  }
};

/**
 * Get fulfillments by customer ID
 * @param {string} customerId - Customer ID
 * @returns {Promise<Array>} List of fulfillments for the customer
 */
export const getFulfillmentsByCustomerId = async (customerId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/customer/${customerId}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching fulfillments for customer ${customerId}:`, error);
    throw error;
  }
};

/**
 * Get fulfillments by status
 * @param {string} status - Fulfillment status
 * @returns {Promise<Array>} List of fulfillments with the specified status
 */
export const getFulfillmentsByStatus = async (status) => {
  try {
    const response = await fetch(`${API_BASE_URL}/status/${status}`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching fulfillments with status ${status}:`, error);
    throw error;
  }
};

/**
 * Get audit history for a specific fulfillment
 * @param {number|string} fulfillmentId - Fulfillment ID
 * @returns {Promise<Array>} List of audit logs for the fulfillment
 */
export const getAuditHistoryByFulfillmentId = async (fulfillmentId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/${fulfillmentId}/history`);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    console.error(`Error fetching audit history for fulfillment ${fulfillmentId}:`, error);
    throw error;
  }
};

export default {
  getAllFulfillments,
  getFulfillmentById,
  getFulfillmentsByOrderId,
  getFulfillmentHistory,
  getFulfillmentsByCustomerId,
  getFulfillmentsByStatus,
  getAuditHistoryByFulfillmentId
};
