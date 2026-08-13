import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axiosClient from '@/api/axiosClient';
import endpoints from '@/api/endpoints';

export const fetchWishlist = createAsyncThunk('wishlist/fetch', async (_, { rejectWithValue }) => {
  try {
    const { data } = await axiosClient.get(endpoints.wishlist.root);
    return data;
  } catch (err) {
    return rejectWithValue(err.message);
  }
});

export const addToWishlist = createAsyncThunk(
  'wishlist/add',
  async (prodId, { dispatch, rejectWithValue }) => {
    try {
      await axiosClient.post(endpoints.wishlist.root, { prodId });
      // Re-read rather than guessing the new list — the server owns the truth.
      dispatch(fetchWishlist());
      return prodId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const removeFromWishlist = createAsyncThunk(
  'wishlist/remove',
  async (wishlistId, { rejectWithValue }) => {
    try {
      await axiosClient.delete(endpoints.wishlist.byId(wishlistId));
      return wishlistId;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

const wishlistSlice = createSlice({
  name: 'wishlist',
  initialState: { items: [], status: 'idle', error: null },
  reducers: {
    resetWishlist(state) { state.items = []; state.status = 'idle'; },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchWishlist.pending, (state) => { state.status = 'loading'; })
      .addCase(fetchWishlist.fulfilled, (state, action) => {
        state.items = action.payload || [];
        state.status = 'succeeded';
      })
      .addCase(fetchWishlist.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      })
      .addCase(removeFromWishlist.fulfilled, (state, action) => {
        state.items = state.items.filter((i) => i.wishlistId !== action.payload);
      });
  },
});

export const { resetWishlist } = wishlistSlice.actions;
export default wishlistSlice.reducer;

export const selectWishlist = (state) => state.wishlist.items;
export const selectWishlistCount = (state) => state.wishlist.items.length;
/** Lets a product card show a filled heart without another request. */
export const selectIsWishlisted = (prodId) => (state) =>
  state.wishlist.items.some((i) => i.prodId === prodId);
