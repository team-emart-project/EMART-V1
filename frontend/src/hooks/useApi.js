import { useCallback, useEffect, useRef, useState } from 'react';
import axiosClient from '@/api/axiosClient';

/**
 * ============================================================================
 * THE API HOOKS.  Two hooks cover every call in the app.
 * ============================================================================
 *
 * Requirement: "custom hook to call API — do not repeat code for GET and
 * getById".
 *
 * The trick is that a list and a single record are NOT different operations.
 * They are the same GET against a different URL:
 *
 *     useFetch(endpoints.products.search())   ->  every product
 *     useFetch(endpoints.products.byId(5))    ->  product 5
 *     useFetch(endpoints.orders.root)         ->  my orders
 *     useFetch(endpoints.orders.byId(9))      ->  order 9
 *
 * So there is ONE `useFetch`, not a `useGetAll` plus a `useGetById`. The URL is
 * the only thing that varies, and `endpoints.js` already owns URL construction.
 *
 * `useMutation` is the write-side twin: POST / PUT / DELETE, triggered on
 * demand rather than on mount.
 */

/* -------------------------------------------------------------------------
 * useFetch — READ. Runs on mount and whenever `url` changes.
 * ------------------------------------------------------------------------- */
export function useFetch(url, options = {}) {
  const {
    immediate = true,   // set false to fetch manually via refetch()
    initialData = null,
    onSuccess,
    onError,
    skip = false,       // e.g. don't fetch until an id exists
  } = options;

  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(immediate && !skip);
  const [error, setError] = useState(null);

  // Guards against setting state after unmount, and against an older response
  // overwriting a newer one when the url changes rapidly (e.g. search typing).
  const requestIdRef = useRef(0);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => { mountedRef.current = false; };
  }, []);

  const execute = useCallback(async () => {
    if (!url || skip) return null;

    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);

    try {
      const response = await axiosClient.get(url);
      // A newer request has started since — discard this stale result.
      if (requestId !== requestIdRef.current || !mountedRef.current) return null;

      setData(response.data);
      onSuccess?.(response.data);
      return response.data;
    } catch (err) {
      if (requestId !== requestIdRef.current || !mountedRef.current) return null;
      setError(err);
      onError?.(err);
      return null;
    } finally {
      if (requestId === requestIdRef.current && mountedRef.current) setLoading(false);
    }
    // onSuccess/onError are intentionally excluded: callers usually pass inline
    // functions, and including them would refetch on every render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [url, skip]);

  useEffect(() => {
    if (immediate) execute();
  }, [execute, immediate]);

  return { data, loading, error, refetch: execute, setData };
}

/* -------------------------------------------------------------------------
 * useMutation — WRITE. Same hook for POST, PUT and DELETE.
 * ------------------------------------------------------------------------- */
export function useMutation(method = 'post') {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const mutate = useCallback(
    async (url, body = undefined, config = undefined) => {
      setLoading(true);
      setError(null);
      try {
        const verb = method.toLowerCase();
        const response =
          verb === 'delete'
            ? await axiosClient.delete(url, config)
            : await axiosClient[verb](url, body, config);

        return { data: response.data, message: response.message, error: null };
      } catch (err) {
        setError(err);
        // Returned rather than thrown so callers can branch without try/catch.
        return { data: null, message: err.message, error: err };
      } finally {
        setLoading(false);
      }
    },
    [method]
  );

  return { mutate, loading, error, reset: () => setError(null) };
}

export const usePost = () => useMutation('post');
export const usePut = () => useMutation('put');
export const useDelete = () => useMutation('delete');
