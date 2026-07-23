/**
 * Common fetch wrapper with error handling and abort signal support
 * Shared across all API services
 */
export const fetchWithErrorHandling = async (url, options = {}) => {
  try {
    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    });

    if (!response.ok) {
      let error;
      try {
        error = await response.json();
      } catch {
        error = { message: `HTTP ${response.status}: ${response.statusText}` };
      }
      throw new Error(error.message || 'Request failed');
    }

    return await response.json();
  } catch (error) {
    // Re-throw abort errors as-is
    if (error.name === 'AbortError') {
      throw error;
    }
    console.error('API Error:', error);
    throw error;
  }
};

/**
 * Build API URL with base URL
 */
export const buildApiUrl = (baseUrl, endpoint) => {
  return `${baseUrl}${endpoint}`;
};

/**
 * Generic GET request
 */
export const get = async (baseUrl, endpoint, options = {}) => {
  const url = buildApiUrl(baseUrl, endpoint);
  return fetchWithErrorHandling(url, {
    method: 'GET',
    ...options,
  });
};

/**
 * Generic POST request
 */
export const post = async (baseUrl, endpoint, data, options = {}) => {
  const url = buildApiUrl(baseUrl, endpoint);
  return fetchWithErrorHandling(url, {
    method: 'POST',
    body: JSON.stringify(data),
    ...options,
  });
};

/**
 * Generic PUT request
 */
export const put = async (baseUrl, endpoint, data, options = {}) => {
  const url = buildApiUrl(baseUrl, endpoint);
  return fetchWithErrorHandling(url, {
    method: 'PUT',
    body: JSON.stringify(data),
    ...options,
  });
};

/**
 * Generic DELETE request
 */
export const deleteRequest = async (baseUrl, endpoint, options = {}) => {
  const url = buildApiUrl(baseUrl, endpoint);
  return fetchWithErrorHandling(url, {
    method: 'DELETE',
    ...options,
  });
};
