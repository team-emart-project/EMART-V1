import { createSlice, nanoid } from '@reduxjs/toolkit';

/**
 * Global UI state: toasts and the mobile drawer.
 *
 * Toasts live in Redux because any slice or page can raise one — a thunk
 * failing deep in the tree should not have to thread a callback back up.
 */
const uiSlice = createSlice({
  name: 'ui',
  initialState: { toasts: [], mobileMenuOpen: false },
  reducers: {
    pushToast: {
      reducer(state, action) { state.toasts.push(action.payload); },
      prepare(message, type = 'info') {
        return { payload: { id: nanoid(), message, type } };
      },
    },
    dismissToast(state, action) {
      state.toasts = state.toasts.filter((t) => t.id !== action.payload);
    },
    toggleMobileMenu(state, action) {
      state.mobileMenuOpen = action.payload ?? !state.mobileMenuOpen;
    },
  },
});

export const { pushToast, dismissToast, toggleMobileMenu } = uiSlice.actions;
export default uiSlice.reducer;

export const toastSuccess = (msg) => pushToast(msg, 'success');
export const toastError = (msg) => pushToast(msg, 'error');
