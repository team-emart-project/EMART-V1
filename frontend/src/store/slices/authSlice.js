import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import axiosClient, { clearToken, getToken, setToken } from '@/api/axiosClient';
import endpoints from '@/api/endpoints';

const USER_KEY = 'emart_user';

/** Restores the session on a page refresh so the user is not logged out. */
const loadUser = () => {
  try {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post(endpoints.auth.login, credentials);
      setToken(data.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(data.user));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

/**
 * Sign in with Google.
 *
 * `credential` is the ID token Google handed the browser. We forward it and
 * receive OUR OWN JWT back, so from here on this session is identical to a
 * password login — same token, same storage, same reducer.
 *
 * Sharing loginUser's reducer cases below is deliberate: any future change to
 * how a session is stored applies to both routes automatically.
 */
export const googleLogin = createAsyncThunk(
  'auth/google',
  async (credential, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post(endpoints.auth.google, { credential });
      setToken(data.accessToken);
      localStorage.setItem(USER_KEY, JSON.stringify(data.user));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const registerUser = createAsyncThunk(
  'auth/register',
  async (payload, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.post(endpoints.auth.register, payload);
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

/** Refreshes the cached profile (e.g. after the user edits it). */
export const fetchCurrentUser = createAsyncThunk(
  'auth/me',
  async (_, { rejectWithValue }) => {
    try {
      const { data } = await axiosClient.get(endpoints.users.me);
      localStorage.setItem(USER_KEY, JSON.stringify(data));
      return data;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    user: loadUser(),
    token: getToken(),
    status: 'idle',
    error: null,
  },
  reducers: {
    logoutUser(state) {
      // Stateless JWT: logging out IS discarding the token client-side.
      clearToken();
      localStorage.removeItem(USER_KEY);
      state.user = null;
      state.token = null;
      state.status = 'idle';
      state.error = null;
    },
    clearAuthError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.token = action.payload.accessToken;
        state.user = action.payload.user;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      })
      // Google login stores a session exactly like a password login, so it
      // reuses the same three cases rather than duplicating them.
      .addCase(googleLogin.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(googleLogin.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.token = action.payload.accessToken;
        state.user = action.payload.user;
      })
      .addCase(googleLogin.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      })
      .addCase(registerUser.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(registerUser.fulfilled, (state) => {
        state.status = 'succeeded';
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.payload;
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.user = action.payload;
      });
  },
});

export const { logoutUser, clearAuthError } = authSlice.actions;
export default authSlice.reducer;
