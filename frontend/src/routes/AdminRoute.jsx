import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { ROLES } from '../utils/constants';
import Loader from '../components/Loader/Loader';

/**
 * AdminRoute
 * Wrap the admin route tree, e.g.:
 *   <Route path="/admin" element={<AdminRoute />}>
 *     <Route path="dashboard" element={<Dashboard />} />
 *     ...
 *   </Route>
 *
 * Logged-in customers hitting an admin URL get sent to the storefront
 * home (not the login page, since they ARE authenticated — just not
 * authorized for this area).
 */
export default function AdminRoute() {
  const { isAuthenticated, role, initializing } = useAuth();

  if (initializing) {
    return <Loader label="Checking your session…" />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (role !== ROLES.ADMIN) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
