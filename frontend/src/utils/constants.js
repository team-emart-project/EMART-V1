/**
 * App-wide constants.
 * Change VITE_API_BASE_URL in your .env file per environment
 * (e.g. http://localhost:8080/api for local Spring Boot, or your
 * deployed/Docker host in production).
 */
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

export const STORAGE_KEYS = {
  TOKEN: 'emart_token',
  REFRESH_TOKEN: 'emart_refresh_token',
  USER: 'emart_user',
};

export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  ADMIN: 'ADMIN',
};
