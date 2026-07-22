const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

/**
 * Get all audit events with pagination
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Number of items per page
 * @returns {Promise<Object>} Paginated audit events
 */
export const getAllAuditEvents = async (page = 0, size = 20) => {
  try {
    const response = await fetch(
      `${API_BASE_URL}/audit?page=${page}&size=${size}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch audit events');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching audit events:', error);
    throw error;
  }
};

/**
 * Get audit events by order ID
 * @param {number} orderId - The order ID to filter by
 * @returns {Promise<Array>} List of audit events for the order
 */
export const getAuditEventsByOrderId = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/audit/order/${orderId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch audit events');
    }

    return await response.json();
  } catch (error) {
    console.error(`Error fetching audit events for order ${orderId}:`, error);
    throw error;
  }
};

/**
 * Get filtered audit events with pagination
 * @param {Object} filters - Filter criteria
 * @param {string} filters.eventType - Event type to filter by
 * @param {string} filters.serviceName - Service name to filter by
 * @param {string} filters.status - Status to filter by
 * @param {number} page - Page number (0-indexed)
 * @param {number} size - Number of items per page
 * @returns {Promise<Object>} Paginated filtered audit events
 */
export const getFilteredAuditEvents = async (filters = {}, page = 0, size = 20) => {
  try {
    const params = new URLSearchParams();
    
    if (filters.eventType) params.append('eventType', filters.eventType);
    if (filters.serviceName) params.append('serviceName', filters.serviceName);
    if (filters.status) params.append('status', filters.status);
    params.append('page', page);
    params.append('size', size);

    const response = await fetch(
      `${API_BASE_URL}/audit/filter?${params.toString()}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch filtered audit events');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching filtered audit events:', error);
    throw error;
  }
};

/**
 * Check audit service health
 * @returns {Promise<Object>} Health status
 */
export const checkAuditHealth = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/audit/health`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      throw new Error('Audit service is down');
    }

    return await response.json();
  } catch (error) {
    console.error('Error checking audit health:', error);
    throw error;
  }
};
// Alias for consistency with tests
export const getAuditLogs = getAllAuditEvents;
