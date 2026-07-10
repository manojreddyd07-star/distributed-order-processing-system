import axios from 'axios';

const API_BASE_URL = 'http://localhost:8086/api/monitoring';

/**
 * Get health metrics for all services
 */
export const getHealthMetrics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/health`);
    return response.data;
  } catch (error) {
    console.error('Error fetching health metrics:', error);
    throw error;
  }
};

/**
 * Get application metrics
 */
export const getApplicationMetrics = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/metrics`);
    return response.data;
  } catch (error) {
    console.error('Error fetching application metrics:', error);
    throw error;
  }
};

export default {
  getHealthMetrics,
  getApplicationMetrics,
};
