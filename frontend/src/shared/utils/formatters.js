/**
 * Format date to locale string
 */
export const formatDate = (dateString) => {
  if (!dateString) return 'N/A';
  try {
    return new Date(dateString).toLocaleString();
  } catch {
    return dateString;
  }
};

/**
 * Format currency amount
 */
export const formatCurrency = (amount, currency = 'USD') => {
  if (amount === null || amount === undefined) return 'N/A';
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency,
  }).format(amount);
};

/**
 * Format number with thousand separators
 */
export const formatNumber = (number) => {
  if (number === null || number === undefined) return 'N/A';
  return number.toLocaleString();
};

/**
 * Truncate text with ellipsis
 */
export const truncateText = (text, maxLength = 50) => {
  if (!text || text.length <= maxLength) return text;
  return text.substring(0, maxLength) + '...';
};

/**
 * Get status color based on status value
 */
export const getStatusColor = (status) => {
  const statusLower = String(status).toLowerCase();
  
  const colorMap = {
    success: 'green',
    completed: 'green',
    active: 'green',
    pending: 'orange',
    processing: 'orange',
    warning: 'orange',
    failed: 'red',
    error: 'red',
    cancelled: 'gray',
    inactive: 'gray',
  };
  
  for (const [key, color] of Object.entries(colorMap)) {
    if (statusLower.includes(key)) {
      return color;
    }
  }
  
  return 'blue'; // default color
};

/**
 * Debounce function
 */
export const debounce = (func, delay) => {
  let timeoutId;
  return (...args) => {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func(...args), delay);
  };
};

/**
 * Deep clone object
 */
export const deepClone = (obj) => {
  return JSON.parse(JSON.stringify(obj));
};

/**
 * Check if object is empty
 */
export const isEmpty = (obj) => {
  return Object.keys(obj).length === 0;
};
