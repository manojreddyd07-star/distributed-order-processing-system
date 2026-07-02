const VALIDATION_API_URL = process.env.REACT_APP_VALIDATION_API_URL || 'http://localhost:8081/api';
const PAYMENT_API_URL = process.env.REACT_APP_PAYMENT_API_URL || 'http://localhost:8082/api';
const INVENTORY_API_URL = process.env.REACT_APP_INVENTORY_API_URL || 'http://localhost:8083/api';
const FULFILLMENT_API_URL = process.env.REACT_APP_FULFILLMENT_API_URL || 'http://localhost:8084/api';

/**
 * Get all idempotency records from all services
 * @returns {Promise<Array>} Combined list of all idempotency records
 */
export const getAllIdempotencyRecords = async () => {
  try {
    const [validationRecords, paymentRecords, inventoryRecords, fulfillmentRecords] = await Promise.all([
      fetchIdempotencyRecords(VALIDATION_API_URL),
      fetchIdempotencyRecords(PAYMENT_API_URL),
      fetchIdempotencyRecords(INVENTORY_API_URL),
      fetchIdempotencyRecords(FULFILLMENT_API_URL),
    ]);

    // Combine all records
    return [
      ...validationRecords,
      ...paymentRecords,
      ...inventoryRecords,
      ...fulfillmentRecords,
    ];
  } catch (error) {
    console.error('Error fetching idempotency records:', error);
    throw error;
  }
};

/**
 * Fetch idempotency records from a specific service
 * @param {string} baseUrl - The base URL of the service
 * @returns {Promise<Array>} List of idempotency records
 */
const fetchIdempotencyRecords = async (baseUrl) => {
  try {
    const response = await fetch(`${baseUrl}/idempotency`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      console.warn(`Failed to fetch idempotency records from ${baseUrl}`);
      return [];
    }

    return await response.json();
  } catch (error) {
    console.warn(`Error fetching idempotency records from ${baseUrl}:`, error);
    return [];
  }
};

/**
 * Get idempotency records filtered by status
 * @param {string} status - The processing status to filter by
 * @returns {Promise<Array>} Filtered list of idempotency records
 */
export const getIdempotencyRecordsByStatus = async (status) => {
  const allRecords = await getAllIdempotencyRecords();
  
  if (!status || status === 'ALL') {
    return allRecords;
  }
  
  return allRecords.filter(record => record.processingStatus === status);
};
