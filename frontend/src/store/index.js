import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import cartReducer from './slices/cartSlice';
import wishlistReducer from './slices/wishlistSlice';
import uiReducer from './slices/uiSlice';

/**
 * Redux Toolkit's configureStore wires up the Redux DevTools, thunk middleware
 * and immutability checks for us — no manual middleware assembly.
 */
export const store = configureStore({
  reducer: {
    auth: authReducer,
    cart: cartReducer,
    wishlist: wishlistReducer,
    ui: uiReducer,
  },
  devTools: import.meta.env.MODE !== 'production',
});

export default store;
