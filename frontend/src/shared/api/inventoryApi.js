const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8083/api';

/**
 * Get all inventory items
 * @returns {Promise<Array>} List of all inventory items
 */
export const getAllInventory = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/inventory`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch inventory');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching inventory:', error);
    throw error;
  }
};

/**
 * Get inventory by product ID
 * @param {number} productId - The product ID
 * @returns {Promise<Object>} The inventory item
 */
export const getInventoryByProductId = async (productId) => {
  try {
    const response = await fetch(`${API_BASE_URL}/inventory/product/${productId}`, {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'Failed to fetch inventory item');
    }

    return await response.json();
  } catch (error) {
    console.error('Error fetching inventory item:', error);
    throw error;
  }
};
