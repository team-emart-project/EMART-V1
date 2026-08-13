/**
 * Every backend URL in ONE place.
 *
 * Nothing else in the app types a URL string. When a route changes on the
 * server, it changes here and nowhere else.
 *
 * Note how list and detail are just two functions returning different paths —
 * that is what lets a single useFetch hook serve both without duplicated code.
 */
export const endpoints = {
  // Module 1 — Home
  home: {
    featuredCategories: '/api/home/featured-categories',
    newArrivals: (limit = 8) => `/api/home/new-arrivals?limit=${limit}`,
  },

  // Module 2 — Auth
  auth: {
    register: '/api/auth/register',
    login: '/api/auth/login',
    google: '/api/auth/google',
    logout: '/api/auth/logout',
    forgotPassword: '/api/auth/forgot-password',
    resetPassword: '/api/auth/reset-password',
  },

  // Module 3 — Profile & addresses
  users: {
    me: '/api/users/me',
    addresses: '/api/users/me/addresses',
    addressById: (id) => `/api/users/me/addresses/${id}`,
    setDefaultAddress: (id) => `/api/users/me/addresses/${id}/default`,
  },

  // Module 4 — e-MART card
  card: {
    apply: '/api/emart-card/apply',
    me: '/api/emart-card/me',
    // Never 404s. Non-cardholders get { cardholder:false, pointsBalance:0 },
    // which is what the "Redeem e-Points" checkbox needs.
    balance: '/api/emart-card/balance',
  },

  // Module 5 — Catalog
  categories: {
    tree: '/api/categories',
    roots: '/api/categories?flat=true',
    byId: (id) => `/api/categories/${id}`,
    subCategories: (id) => `/api/categories/${id}/subcategories`,
    products: (id, qs = '') => `/api/categories/${id}/products${qs ? `?${qs}` : ''}`,
  },
  products: {
    search: (qs = '') => `/api/products${qs ? `?${qs}` : ''}`,
    byId: (id) => `/api/products/${id}`,
    variants: (id) => `/api/products/${id}/variants`,
  },

  // Module 6 — Cart
  cart: {
    root: '/api/cart',
    items: '/api/cart/items',
    itemById: (id) => `/api/cart/items/${id}`,
  },

  // Module 7 — Wishlist
  wishlist: {
    root: '/api/wishlist',
    byId: (id) => `/api/wishlist/${id}`,
  },

  // Module 8 — Orders
  orders: {
    preview: '/api/orders/checkout-preview',
    root: '/api/orders',
    byId: (id) => `/api/orders/${id}`,
    cancel: (id) => `/api/orders/${id}/cancel`,
    invoicePdf: (id) => `/api/orders/${id}/invoice-pdf`,
  },

  // Module 9 — Payment
  payments: {
    verify: (orderId) => `/api/payments/${orderId}/verify`,
    byOrder: (orderId) => `/api/payments/${orderId}`,
  },
};

export default endpoints;
