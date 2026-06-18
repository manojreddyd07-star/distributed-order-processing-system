const API_BASE_URL = process.env.REACT_APP_VALIDATION_API_URL || 'http://localhost:8081/api';

/**
 * Get all validations (validation history)
 * @returns {Promise<Array>} List of all validations
 */
export const getValidationHistory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/validations`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch validation history' }));
      throw new Error(error.message || 'Failed to fetch validation history');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching validation history:', error);
    throw error;
  }
};

/**
 * Get a validation by ID
 * @param {number} validationId - The validation ID
 * @returns {Promise<Object>} The validation response
 */
export const getValidationById = async (validationId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/validations/${validationId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch validation' }));
      throw new Error(error.message || 'Failed to fetch validation');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching validation:', error);
    throw error;
  }
};

/**
 * Get validations by order ID
 * @param {number} orderId - The order ID
 * @returns {Promise<Array>} List of validations for the order
 */
export const getValidationsByOrderId = async (orderId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/validations?orderId=${orderId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'Failed to fetch validations' }));
      throw new Error(error.message || 'Failed to fetch validations');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching validations by order ID:', error);
    throw error;
  }
};
