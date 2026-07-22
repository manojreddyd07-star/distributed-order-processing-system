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

/**
 * Get performance metrics (throughput, latency, failure rate)
 * @param {number} minutes - Time window in minutes (default: 5)
 */
export const getPerformanceMetrics = async (minutes = 5) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/performance-metrics`, {
      params: { minutes }
    });
    return response.data;
  } catch (error) {
    console.error('Error fetching performance metrics:', error);
    throw error;
  }
};

export default {
  getHealthMetrics,
  getApplicationMetrics,
  getPerformanceMetrics,
};
// Aliases for consistency with tests
export const getSystemMetrics = getHealthMetrics;
export const getEventMetrics = getApplicationMetrics;
