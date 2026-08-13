import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axiosClient from '@/api/axiosClient';
import endpoints from '@/api/endpoints';

/**
 * The cart lives in Redux rather than in component state because the navbar
 * badge, the cart page and the checkout page all need the same number, and it
 * must stay in sync when any of them changes it.
 *
 * The server is the source of truth: every thunk returns the whole recalculated
 * cart, so totals can never drift from the backend's figures.
 */

export const fetchCart = createAsyncThunk('cart/fetch', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get(endpoints.cart.root);
    return data;
  } catch (err) {
    return rejectWithValue(err.message);
  }
});

export const addCartItem = createAsyncThunk(
  'cart/add',
  async (payload, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post(endpoints.cart.items, payload);
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const updateCartItem = createAsyncThunk(
  'cart/update',
  async ({ cartItemId, ...body }, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.put(endpoints.cart.itemById(cartItemId), body);
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const removeCartItem = createAsyncThunk(
  'cart/remove',
  async (cartItemId, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.delete(endpoints.cart.itemById(cartItemId));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const clearCart = createAsyncThunk('cart/clear', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.delete(endpoints.cart.root);
    return data;
  } catch (err) {
    return rejectWithValue(err.message);
  }
});

const emptyCart = {
  cartId: null, items: [], distinctItemCount: 0, totalQuantity: 0,
  subtotalMrp: 0, subtotalPayable: 0, totalSavings: 0, totalPointsUsed: 0,
  cardholder: false,
};

const cartSlice = createSlice({
  name: 'cart',
  initialState: { cart: emptyCart, status: 'idle', error: null, actionPending: false },
  reducers: {
    resetCart(state) {
      state.cart = emptyCart;
      state.status = 'idle';
    },
  },
  extraReducers: (builder) => {
    // Every cart endpoint returns the full cart, so one shared handler covers
    // add / update / remove / clear instead of four near-identical blocks.
    const applyCart = (state, action) => {
      state.cart = action.payload || emptyCart;
      state.status = 'succeeded';
      state.actionPending = false;
      state.error = null;
    };
    const pending = (state) => { state.actionPending = true; state.error = null; };
    const failed = (state, action) => {
      state.actionPending = false;
      state.status = 'failed';
      state.error = action.payload;
    };

    builder
      .addCase(fetchCart.pending, (state) => { state.status = 'loading'; })
      .addCase(fetchCart.fulfilled, applyCart)
      .addCase(fetchCart.rejected, failed);

    [addCartItem, updateCartItem, removeCartItem, clearCart].forEach((thunk) => {
      builder
        .addCase(thunk.pending, pending)
        .addCase(thunk.fulfilled, applyCart)
        .addCase(thunk.rejected, failed);
    });
  },
});

export const { resetCart } = cartSlice.actions;
export default cartSlice.reducer;

/* Selectors keep component code free of state-shape knowledge. */
export const selectCart = (state) => state.cart.cart;
export const selectCartCount = (state) => state.cart.cart?.totalQuantity ?? 0;
export const selectCartBusy = (state) => state.cart.actionPending;
