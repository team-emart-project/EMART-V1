import axios from 'axios';
import { API_BASE_URL } from '../utils/constants';
import { tokenStorage } from '../utils/tokenStorage';

/**
 * axiosInstance
 * Every service file imports THIS, never the raw `axios` package.
 * That's what makes the JWT-attach + 401-redirect logic automatic
 * and consistent across every single API call in the app.
 */
const axiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach JWT to every outgoing request, if we have one.
axiosInstance.interceptors.request.use(
  (config) => {
    const token = tokenStorage.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Normalize errors + handle expired/invalid tokens in one place.
axiosInstance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;

    if (status === 401) {
      // Token missing/expired/invalid — clear local session and
      // send the user to login. Adjust the redirect path if your
      // login route differs.
      tokenStorage.clearAll();
      if (window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }

    // Give components a consistent shape to read regardless of
    // whether it was a network failure or a backend error response.
    const normalizedMessage =
      error.response?.data?.message ||
      error.response?.data?.error ||
      (status === undefined ? 'Network error — please check your connection.' : 'Something went wrong. Please try again.');

    return Promise.reject({ ...error, normalizedMessage, status });
  }
);

export default axiosInstance;
