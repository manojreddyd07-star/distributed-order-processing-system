import { get } from '../shared/api/apiClient';

const API_BASE_URL = process.env.REACT_APP_MONITORING_API_URL || 'http://localhost:8086/api/monitoring';

/**
 * Get health metrics for all services
 */
export const getHealthMetrics = async () => {
  try {
    return await get(API_BASE_URL, '/health');
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
    return await get(API_BASE_URL, '/metrics');
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
    const params = new URLSearchParams({ minutes: String(minutes) });
    return await get(API_BASE_URL, `/performance-metrics?${params}`);
  } catch (error) {
    console.error('Error fetching performance metrics:', error);
    throw error;
  }
};

/**
 * Get service health status for all services
 */
export const getServiceHealth = async () => {
  try {
    return await get(API_BASE_URL, '/health');
  } catch (error) {
    console.error('Error fetching service health:', error);
    throw error;
  }
};

export default {
  getHealthMetrics,
  getApplicationMetrics,
  getPerformanceMetrics,
  getServiceHealth,
};
// Aliases for consistency with tests
export const getSystemMetrics = getHealthMetrics;
export const getEventMetrics = getApplicationMetrics;
