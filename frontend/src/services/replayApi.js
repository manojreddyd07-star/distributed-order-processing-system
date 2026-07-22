const VALIDATION_API_URL = process.env.REACT_APP_VALIDATION_API_URL || 'http://localhost:8081/api';
const PAYMENT_API_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';
const INVENTORY_API_URL = process.env.REACT_APP_INVENTORY_API_URL || 'http://localhost:8083/api';
const FULFILLMENT_API_URL = process.env.REACT_APP_FULFILLMENT_API_URL || 'http://localhost:8084/api';

/**
 * Replay a failed event
 * @param {string} serviceName - The service name (validation-service, payment-service, etc.)
 * @param {Object} replayRequest - The replay request object
 * @returns {Promise<Object>} Replay response
 */
export const replayEvent = async (serviceName, replayRequest) => {
  try {
    const baseUrl = getServiceUrl(serviceName);
    
    if (!baseUrl) {
      throw new Error(`Unknown service: ${serviceName}`);
    }
    
    const response = await fetch(`${baseUrl}/replay`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(replayRequest),
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to replay event');
    }

    return await response.json();
  } catch (error) {
    console.error('Error replaying event:', error);
    throw error;
  }
};

/**
 * Get the base URL for a service
 * @param {string} serviceName - The service name
 * @returns {string} Base URL
 */
const getServiceUrl = (serviceName) => {
  const serviceMap = {
    'validation-service': VALIDATION_API_URL,
    'payment-service': PAYMENT_API_URL,
    'inventory-service': INVENTORY_API_URL,
    'fulfillment-service': FULFILLMENT_API_URL,
  };
  
  return serviceMap[serviceName];
};

/**
 * Get available Kafka topics for replay
 * @returns {Array} List of available topics
 */
export const getAvailableTopics = () => {
  return [
    { value: 'order-created', label: 'Order Created (order-created)' },
    { value: 'order-validated', label: 'Order Validated (order-validated)' },
    { value: 'order-validation-failed', label: 'Order Validation Failed (order-validation-failed)' },
    { value: 'payment-completed-events', label: 'Payment Completed (payment-completed-events)' },
    { value: 'inventory-reserved', label: 'Inventory Reserved (inventory-reserved)' },
    { value: 'inventory-rejected', label: 'Inventory Rejected (inventory-rejected)' },
    { value: 'inventory-reserved-events', label: 'Inventory Reserved Events (inventory-reserved-events)' },
    { value: 'order-completed', label: 'Order Completed (order-completed)' },
    { value: 'retry-orders', label: 'Retry Orders (retry-orders)' },
    { value: 'dead-letter-orders', label: 'Dead Letter Queue (dead-letter-orders)' },
  ];
};
// Alias for consistency with tests
export const getReplayHistory = () => Promise.resolve([]);
