/**
 * Debounce utility function
 * Delays execution of a function until after a specified delay has passed
 * since the last time it was invoked
 * 
 * @param {Function} func - The function to debounce
 * @param {number} delay - The delay in milliseconds
 * @returns {Function} The debounced function
 */
export const debounce = (func, delay = 300) => {
  let timeoutId;
  
  return (...args) => {
    clearTimeout(timeoutId);
    
    timeoutId = setTimeout(() => {
      func.apply(this, args);
    }, delay);
  };
};

/**
 * Throttle utility function
 * Ensures a function is only called once in a specified time period
 * 
 * @param {Function} func - The function to throttle
 * @param {number} limit - The time limit in milliseconds
 * @returns {Function} The throttled function
 */
export const throttle = (func, limit = 300) => {
  let inThrottle;
  
  return function(...args) {
    if (!inThrottle) {
      func.apply(this, args);
      inThrottle = true;
      setTimeout(() => inThrottle = false, limit);
    }
  };
};

/**
 * Simple in-memory cache implementation
 */
class SimpleCache {
  constructor(ttl = 60000) { // Default TTL: 1 minute
    this.cache = new Map();
    this.ttl = ttl;
  }

  set(key, value) {
    const expiry = Date.now() + this.ttl;
    this.cache.set(key, { value, expiry });
  }

  get(key) {
    const item = this.cache.get(key);
    
    if (!item) {
      return null;
    }
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      return null;
    }
    
    return item.value;
  }

  has(key) {
    return this.get(key) !== null;
  }

  clear() {
    this.cache.clear();
  }

  delete(key) {
    this.cache.delete(key);
  }
}

// Create a global cache instance with 5 minute TTL
export const apiCache = new SimpleCache(300000);

/**
 * Cache wrapper for API calls
 * 
 * @param {string} cacheKey - The unique key for this API call
 * @param {Function} apiCall - The API call function to execute
 * @param {boolean} useCache - Whether to use caching (default: true)
 * @returns {Promise} The API response
 */
export const cachedApiCall = async (cacheKey, apiCall, useCache = true) => {
  if (useCache && apiCache.has(cacheKey)) {
    return apiCache.get(cacheKey);
  }
  
  const response = await apiCall();
  
  if (useCache) {
    apiCache.set(cacheKey, response);
  }
  
  return response;
};
