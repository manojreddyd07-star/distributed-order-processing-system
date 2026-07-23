import { useState, useEffect, useCallback } from 'react';

/**
 * Custom hook for data fetching with loading and error states
 * @param {Function} fetchFn - The fetch function to call
 * @param {Array} dependencies - Dependencies to trigger re-fetch
 * @param {boolean} immediate - Whether to fetch immediately on mount
 */
export const useFetch = (fetchFn, dependencies = [], immediate = true) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await fetchFn();
      setData(result);
      return result;
    } catch (err) {
      setError(err.message || 'An error occurred');
      throw err;
    } finally {
      setLoading(false);
    }
  }, [fetchFn]);

  useEffect(() => {
    if (immediate) {
      fetchData();
    }
  }, dependencies);

  const refetch = useCallback(() => {
    return fetchData();
  }, [fetchData]);

  return { data, loading, error, refetch };
};

/**
 * Custom hook for handling form submission with loading state
 * @param {Function} submitFn - The submission function
 * @param {Function} onSuccess - Success callback
 * @param {Function} onError - Error callback
 */
export const useSubmit = (submitFn, onSuccess, onError) => {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  const submit = useCallback(async (...args) => {
    setSubmitting(true);
    setError(null);
    try {
      const result = await submitFn(...args);
      if (onSuccess) {
        onSuccess(result);
      }
      return result;
    } catch (err) {
      const errorMsg = err.message || 'Submission failed';
      setError(errorMsg);
      if (onError) {
        onError(err);
      }
      throw err;
    } finally {
      setSubmitting(false);
    }
  }, [submitFn, onSuccess, onError]);

  return { submit, submitting, error };
};
