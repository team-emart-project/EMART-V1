import { useState } from 'react';
import { motion } from 'framer-motion';
import { Lock, Mail, Store } from 'lucide-react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Button, Card, Input } from '@/components/ui';
import GoogleSignInButton from '@/components/common/GoogleSignInButton';
import useAuth from '@/hooks/useAuth';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

export default function LoginPage() {
  const { login, loading, error } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();

  const [form, setForm] = useState({ email: '', password: '' });
  const expired = new URLSearchParams(location.search).get('expired');

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const onSubmit = async (e) => {
    e.preventDefault();
    const ok = await login(form);
    if (ok) {
      dispatch(toastSuccess('Welcome back!'));
      // Return them to wherever ProtectedRoute intercepted them.
      navigate(location.state?.from?.pathname || '/', { replace: true });
    } else {
      dispatch(toastError('Login failed'));
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
      <motion.div
        initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }} className="w-full max-w-md"
      >
        <div className="mb-6 text-center">
<<<<<<< HEAD
          <span className="mb-3 inline-flex rounded-2xl bg-brand-600 p-3">
=======
          <span className="mb-3 inline-flex rounded-2xl bg-brand-gradient p-3 shadow-lg shadow-brand-600/25">
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
            <Store className="h-6 w-6 text-white" />
          </span>
          <h1 className="text-2xl font-bold text-slate-900">Welcome back</h1>
          <p className="mt-1 text-sm text-slate-500">Log in to shop, track orders and earn e-Points.</p>
        </div>

        <Card>
          {expired && (
            <div className="mb-4 rounded-xl bg-amber-50 px-3 py-2 text-sm text-amber-800">
              Your session expired. Please log in again.
            </div>
          )}
          {error && (
            <div className="mb-4 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700 animate-fade-in">
              {error}
            </div>
          )}

          <form onSubmit={onSubmit} className="space-y-4">
            <Input label="Email" name="email" type="email" icon={Mail} required
              value={form.email} onChange={onChange} placeholder="you@example.com" autoComplete="email" />
            <Input label="Password" name="password" type="password" icon={Lock} required
              value={form.password} onChange={onChange} placeholder="••••••••" autoComplete="current-password" />

            <div className="flex justify-end">
              <Link to="/forgot-password" className="text-sm text-brand-600 hover:underline">
                Forgot password?
              </Link>
            </div>

            <Button type="submit" fullWidth size="lg" loading={loading}>Log in</Button>
          </form>

          {/* Divider, then the Google route. Placed BELOW the password form so
              the primary path stays primary — a returning password user should
              not have to look past an alternative to find their own. */}
          <div className="my-5 flex items-center gap-3">
            <span className="h-px flex-1 bg-slate-200" />
            <span className="text-xs font-medium uppercase tracking-wide text-slate-400">or</span>
            <span className="h-px flex-1 bg-slate-200" />
          </div>

          <GoogleSignInButton text="signin_with" />

          <p className="mt-5 text-center text-sm text-slate-500">
            New here?{' '}
            <Link to="/register" className="font-medium text-brand-600 hover:underline">
              Create an account
            </Link>
          </p>
        </Card>

        <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-white/60 p-3 text-xs text-slate-500">
          <p className="font-medium text-slate-600">Demo accounts (password <code>Password@123</code>)</p>
          <p className="mt-1">rishi.chhalotre@example.com — cardholder, member pricing</p>
          <p>ananya.sharma@example.com — normal member, MRP pricing</p>
        </div>
      </motion.div>
    </div>
  );
}
