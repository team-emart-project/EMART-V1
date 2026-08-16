import { Navigate, Outlet } from 'react-router-dom';
import useAuth from '@/hooks/useAuth';

/**
 * The mirror image of ProtectedRoute: keeps an already-logged-in user away from
 * the login and register screens.
 */
export default function PublicOnlyRoute({ redirectTo = '/' }) {
  const { isAuthenticated } = useAuth();
  return isAuthenticated ? <Navigate to={redirectTo} replace /> : <Outlet />;
}
