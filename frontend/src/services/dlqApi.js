const VALIDATION_API_URL = process.env.REACT_APP_VALIDATION_API_URL || 'http://localhost:8081/api';
const PAYMENT_API_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';
const INVENTORY_API_URL = process.env.REACT_APP_INVENTORY_API_URL || 'http://localhost:8083/api';
const FULFILLMENT_API_URL = process.env.REACT_APP_FULFILLMENT_API_URL || 'http://localhost:8084/api';

/**
 * Get all failed events from all services
 * @returns {Promise<Array>} Combined list of all failed events
 */
export const getAllFailedEvents = async () => {
  try {
    const [validationFailed, paymentFailed, inventoryFailed, fulfillmentFailed] = await Promise.all([
      fetchFailedEvents(VALIDATION_API_URL),
      fetchFailedEvents(PAYMENT_API_URL),
      fetchFailedEvents(INVENTORY_API_URL),
      fetchFailedEvents(FULFILLMENT_API_URL),
    ]);

    // Combine all records
    return [
      ...validationFailed,
      ...paymentFailed,
      ...inventoryFailed,
      ...fulfillmentFailed,
    ];
  } catch (error) {
    console.error('Error fetching failed events:', error);
    throw error;
  }
};

/**
 * Fetch failed events from a specific service
 * @param {string} baseUrl - The base URL of the service
 * @returns {Promise<Array>} List of failed events
 */
const fetchFailedEvents = async (baseUrl) => {
  try {
    const response = await fetch(`${baseUrl}/dlq`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      console.warn(`Failed to fetch failed events from ${baseUrl}`);
      return [];
    }

    return await response.json();
  } catch (error) {
    console.warn(`Error fetching failed events from ${baseUrl}:`, error);
    return [];
  }
};

/**
 * Get failed events by service name
 * @param {string} serviceName - The service name to filter by
 * @returns {Promise<Array>} Filtered list of failed events
 */
export const getFailedEventsByService = async (serviceName) => {
  try {
    const allEvents = await getAllFailedEvents();
    return allEvents.filter(event => event.serviceName === serviceName);
  } catch (error) {
    console.error('Error filtering failed events by service:', error);
    throw error;
  }
};
