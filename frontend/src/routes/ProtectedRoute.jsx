import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Loader from '../components/Loader/Loader';

/**
 * ProtectedRoute
 * Wrap any route tree that requires a logged-in user, e.g.:
 *   <Route element={<ProtectedRoute />}>
 *     <Route path="/checkout" element={<Checkout />} />
 *     <Route path="/orders" element={<Orders />} />
 *   </Route>
 *
 * Redirects to /login and remembers where the user was headed, so
 * the Login page can send them back after a successful login.
 */
export default function ProtectedRoute() {
  const { isAuthenticated, initializing } = useAuth();
  const location = useLocation();

  if (initializing) {
    return <Loader label="Checking your session…" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
