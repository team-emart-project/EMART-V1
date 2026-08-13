import axios from 'axios';

/**
 * ONE axios instance for the whole app.
 *
 * Every call goes through here, so the JWT header, error shape and 401 handling
 * are written once rather than repeated in every component.
 */
const axiosClient = axios.create({
  // Empty in dev: vite.config.js proxies /api to :8080, so there is no CORS.
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

export const TOKEN_KEY = 'emart_token';

export const getToken = () => localStorage.getItem(TOKEN_KEY);
export const setToken = (token) => localStorage.setItem(TOKEN_KEY, token);
export const clearToken = () => localStorage.removeItem(TOKEN_KEY);

/* Request: attach the bearer token if we have one. */
axiosClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/* Response: unwrap the backend envelope and normalise every error. */
axiosClient.interceptors.response.use(
  (response) => {
    // The API always replies { success, message, data, timestamp }.
    // Unwrapping here means components deal with plain data, never the envelope.
    // Blobs (the invoice PDF) are passed through untouched.
    if (response.config.responseType === 'blob') return response;

    const body = response.data;
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return { ...response, data: body.data, message: body.message };
    }
    return response;
  },
  (error) => {
    const status = error.response?.status;
    const payload = error.response?.data;

    if (status === 401 && !error.config?.url?.includes('/api/auth/')) {
      // Token expired or invalid. There is no refresh token in this project,
      // so the only correct move is to drop it and let the router redirect.
      clearToken();
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login?expired=1';
      }
    }

    // Collapse everything into one predictable shape so callers never have to
    // dig through error.response?.data?.message themselves.
    return Promise.reject({
      status: status ?? 0,
      message:
        payload?.message ||
        (status === 0 || !error.response
          ? 'Cannot reach the server. Is the backend running on port 8080?'
          : 'Something went wrong. Please try again.'),
      fieldErrors: payload?.fieldErrors || null,
      raw: error,
    });
  }
);

export default axiosClient;
