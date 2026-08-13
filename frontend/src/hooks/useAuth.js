import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import { loginUser, logoutUser, registerUser } from '@/store/slices/authSlice';

/**
 * Thin wrapper over the auth slice so components never import raw actions
 * or reach into store internals.
 */
export default function useAuth() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { user, token, status, error } = useSelector((state) => state.auth);

  const login = async (credentials) => {
    const result = await dispatch(loginUser(credentials));
    return loginUser.fulfilled.match(result);
  };

  const register = async (payload) => {
    const result = await dispatch(registerUser(payload));
    return registerUser.fulfilled.match(result);
  };

  const logout = () => {
    dispatch(logoutUser());
    navigate('/login', { replace: true });
  };

  return {
    user,
    token,
    isAuthenticated: Boolean(token),
    isCardholder: Boolean(user?.cardholder),
    loading: status === 'loading',
    error,
    login,
    register,
    logout,
  };
}
