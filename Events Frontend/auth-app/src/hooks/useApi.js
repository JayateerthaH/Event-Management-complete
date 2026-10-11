import { useState, useEffect, useCallback } from 'react';

/**
 * Generic hook for one-off API calls.
 *
 * Usage:
 *   const { data, loading, error, refetch } = useApi(() => fetchBasicInfo());
 *
 * @param {Function} apiFn  — A function returning a Promise (the service call).
 * @param {Array}    deps   — Dependency array; re-fetches when these change.
 * @param {boolean}  immediate — Whether to call on mount (default true).
 */
const useApi = (apiFn, deps = [], immediate = true) => {
  const [data, setData]       = useState(null);
  const [loading, setLoading] = useState(immediate);
  const [error, setError]     = useState(null);

  const execute = useCallback(async (...args) => {
    setLoading(true);
    setError(null);
    try {
      const result = await apiFn(...args);
      setData(result);
      return result;
    } catch (err) {
      // Don't surface session-expiry as a visible error
      if (err.message === 'SESSION_EXPIRED') return;
      setError(err.message || 'Something went wrong');
      throw err;
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps);

  useEffect(() => {
    if (immediate) execute();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [execute]);

  return { data, loading, error, refetch: execute };
};

export default useApi;
