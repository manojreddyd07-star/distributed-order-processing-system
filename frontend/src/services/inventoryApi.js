import { get } from '../shared/api/apiClient';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8083/api';

/**
 * Get all inventory items
 * @returns {Promise<Array>} List of all inventory items
 */
export const getAllInventory = async () => {
  return get(API_BASE_URL, '/inventory');
};

/**
 * Get inventory by product ID
 * @param {number} productId - The product ID
 * @returns {Promise<Object>} The inventory item
 */
export const getInventoryByProductId = async (productId) => {
  return get(API_BASE_URL, `/inventory/product/${productId}`);
};
  }
};
// Alias for consistency with tests
export const getAllInventoryItems = getAllInventory;
