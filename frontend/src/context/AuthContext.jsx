import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { tokenStorage } from '../utils/tokenStorage';

const AuthContext = createContext(null);

/**
 * AuthProvider
 * Holds the current user + token in memory (mirrored to localStorage via
 * tokenStorage) so a page refresh doesn't log the user out.
 *
 * This context does NOT call the login/register API itself — that's the
 * Login/Register page's job, using services/authService.js once it exists.
 * Once that call succeeds, the page calls `login(token, user)` from here
 * to store the session app-wide.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);

  // Restore session on first load (e.g. after a page refresh)
  useEffect(() => {
    const storedToken = tokenStorage.getToken();
    const storedUser = tokenStorage.getUser();
    if (storedToken && storedUser) {
      setUser(storedUser);
    }
    setInitializing(false);
  }, []);

  const login = (token, userData, refreshToken) => {
    tokenStorage.setToken(token);
    tokenStorage.setUser(userData);
    if (refreshToken) tokenStorage.setRefreshToken(refreshToken);
    setUser(userData);
  };

  const logout = () => {
    tokenStorage.clearAll();
    setUser(null);
  };

  const value = useMemo(
    () => ({
      user,
      isAuthenticated: !!user,
      role: user?.role ?? null,
      initializing,
      login,
      logout,
    }),
    [user, initializing]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/**
 * useAuth
 * @returns {{ user, isAuthenticated, role, initializing, login, logout }}
 */
export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
