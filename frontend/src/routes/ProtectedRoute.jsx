import { Navigate, Outlet, useLocation } from 'react-router-dom';
import useAuth from '@/hooks/useAuth';

/**
 * REQUIREMENT 1 — Protected routes.
 *
 * Wraps any route that needs a logged-in user. If there is no token we send the
 * visitor to /login and remember where they were heading in location.state, so
 * after logging in they land on the page they originally wanted instead of the
 * home page.
 *
 * Used as a LAYOUT route, so one wrapper guards a whole group of children
 * rather than repeating a guard on each one.
 *
 * This is a UX guard, not a security boundary — the real enforcement is the
 * JWT check on the backend. A user who edits localStorage still gets 401s.
 */
export default function ProtectedRoute({ redirectTo = '/login' }) {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to={redirectTo} state={{ from: location }} replace />;
  }

  return <Outlet />;
}
