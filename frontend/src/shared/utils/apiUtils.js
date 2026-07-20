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
 * Enhanced in-memory cache implementation with LRU eviction
 */
class EnhancedCache {
  constructor(ttl = 60000, maxSize = 100) {
    this.cache = new Map();
    this.ttl = ttl;
    this.maxSize = maxSize;
    this.accessOrder = [];
  }

  set(key, value) {
    // Remove oldest entry if cache is full
    if (this.cache.size >= this.maxSize && !this.cache.has(key)) {
      const oldestKey = this.accessOrder.shift();
      this.cache.delete(oldestKey);
    }

    const expiry = Date.now() + this.ttl;
    this.cache.set(key, { value, expiry });
    
    // Update access order
    const index = this.accessOrder.indexOf(key);
    if (index > -1) {
      this.accessOrder.splice(index, 1);
    }
    this.accessOrder.push(key);
  }

  get(key) {
    const item = this.cache.get(key);
    
    if (!item) {
      return null;
    }
    
    if (Date.now() > item.expiry) {
      this.cache.delete(key);
      const index = this.accessOrder.indexOf(key);
      if (index > -1) {
        this.accessOrder.splice(index, 1);
      }
      return null;
    }
    
    // Update access order for LRU
    const index = this.accessOrder.indexOf(key);
    if (index > -1) {
      this.accessOrder.splice(index, 1);
      this.accessOrder.push(key);
    }
    
    return item.value;
  }

  has(key) {
    return this.get(key) !== null;
  }

  clear() {
    this.cache.clear();
    this.accessOrder = [];
  }

  delete(key) {
    this.cache.delete(key);
    const index = this.accessOrder.indexOf(key);
    if (index > -1) {
      this.accessOrder.splice(index, 1);
    }
  }

  // Clear expired entries
  clearExpired() {
    const now = Date.now();
    for (const [key, item] of this.cache.entries()) {
      if (now > item.expiry) {
        this.cache.delete(key);
        const index = this.accessOrder.indexOf(key);
        if (index > -1) {
          this.accessOrder.splice(index, 1);
        }
      }
    }
  }
}

// Create a global cache instance with 5 minute TTL and max 100 entries
export const apiCache = new EnhancedCache(300000, 100);

// Clear expired entries every minute
setInterval(() => apiCache.clearExpired(), 60000);

/**
 * Request deduplication - prevents duplicate requests for the same key
 */
const pendingRequests = new Map();

/**
 * Cache wrapper for API calls with request deduplication
 * 
 * @param {string} cacheKey - The unique key for this API call
 * @param {Function} apiCall - The API call function to execute
 * @param {boolean} useCache - Whether to use caching (default: true)
 * @returns {Promise} The API response
 */
export const cachedApiCall = async (cacheKey, apiCall, useCache = true) => {
  // Check cache first
  if (useCache && apiCache.has(cacheKey)) {
    return apiCache.get(cacheKey);
  }
  
  // Check for pending request (deduplication)
  if (pendingRequests.has(cacheKey)) {
    return pendingRequests.get(cacheKey);
  }
  
  // Create new request
  const requestPromise = apiCall()
    .then(response => {
      if (useCache) {
        apiCache.set(cacheKey, response);
      }
      pendingRequests.delete(cacheKey);
      return response;
    })
    .catch(error => {
      pendingRequests.delete(cacheKey);
      throw error;
    });
  
  pendingRequests.set(cacheKey, requestPromise);
  
  return requestPromise;
  
  if (useCache) {
    apiCache.set(cacheKey, response);
  }
  
  return response;
};
