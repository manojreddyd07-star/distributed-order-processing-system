const VALIDATION_API_URL = process.env.REACT_APP_VALIDATION_API_URL || 'http://localhost:8081/api';
const PAYMENT_API_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';
const INVENTORY_API_URL = process.env.REACT_APP_INVENTORY_API_URL || 'http://localhost:8083/api';
const FULFILLMENT_API_URL = process.env.REACT_APP_FULFILLMENT_API_URL || 'http://localhost:8084/api';

/**
 * Get all retry records from all services
 * @returns {Promise<Array>} Combined list of all retry records
 */
export const getAllRetryRecords = async () => {
  try {
    const [validationRetries, paymentRetries, inventoryRetries, fulfillmentRetries] = await Promise.all([
      fetchRetryRecords(VALIDATION_API_URL),
      fetchRetryRecords(PAYMENT_API_URL),
      fetchRetryRecords(INVENTORY_API_URL),
      fetchRetryRecords(FULFILLMENT_API_URL),
    ]);

    // Combine all records
    return [
      ...validationRetries,
      ...paymentRetries,
      ...inventoryRetries,
      ...fulfillmentRetries,
    ];
  } catch (error) {
    console.error('Error fetching retry records:', error);
    throw error;
  }
};

/**
 * Fetch retry records from a specific service
 * @param {string} baseUrl - The base URL of the service
 * @returns {Promise<Array>} List of retry records
 */
const fetchRetryRecords = async (baseUrl) => {
  try {
    const response = await fetch(`${baseUrl}/retry`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      console.warn(`Failed to fetch retry records from ${baseUrl}`);
      return [];
    }

    return await response.json();
  } catch (error) {
    console.warn(`Error fetching retry records from ${baseUrl}:`, error);
    return [];
  }
};

/**
 * Get retry records filtered by status
 * @param {string} status - The retry status to filter by
 * @returns {Promise<Array>} Filtered list of retry records
 */
export const getRetryRecordsByStatus = async (status) => {
  const allRecords = await getAllRetryRecords();
  
  if (!status || status === 'ALL') {
    return allRecords;
  }
  
  return allRecords.filter(record => record.retryStatus === status);
};

/**
 * Get retry records for a specific service
 * @param {string} serviceName - The name of the service
 * @returns {Promise<Array>} List of retry records for the service
 */
export const getRetryRecordsByService = async (serviceName) => {
  const allRecords = await getAllRetryRecords();
  
  if (!serviceName || serviceName === 'ALL') {
    return allRecords;
  }
  
  return allRecords.filter(record => record.serviceName === serviceName);
};
