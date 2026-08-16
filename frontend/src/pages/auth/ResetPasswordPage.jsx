import { useState } from 'react';
import { motion } from 'framer-motion';
import { KeyRound, Lock } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Button, Card, Input } from '@/components/ui';
import { usePost } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

export default function ResetPasswordPage() {
  const { mutate, loading } = usePost();
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const [form, setForm] = useState({ token: '', newPassword: '', confirm: '' });
  const [fieldError, setFieldError] = useState(null);

  const onSubmit = async (e) => {
    e.preventDefault();
    if (form.newPassword !== form.confirm) {
      setFieldError('The two passwords do not match');
      return;
    }
    setFieldError(null);

    const { error } = await mutate(endpoints.auth.resetPassword, {
      token: form.token, newPassword: form.newPassword,
    });

    if (error) {
      dispatch(toastError(error.message));
    } else {
      dispatch(toastSuccess('Password reset. You can log in now.'));
      navigate('/login');
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md">
        <div className="mb-6 text-center">
<<<<<<< HEAD
          <span className="mb-3 inline-flex rounded-2xl bg-brand-600 p-3">
=======
          <span className="mb-3 inline-flex rounded-2xl bg-brand-gradient p-3 shadow-lg shadow-brand-600/25">
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
            <KeyRound className="h-6 w-6 text-white" />
          </span>
          <h1 className="text-2xl font-bold text-slate-900">Set a new password</h1>
          <p className="mt-1 text-sm text-slate-500">
            Paste the token from your reset email (or the backend console).
          </p>
        </div>

        <Card>
          <form onSubmit={onSubmit} className="space-y-4">
            <Input label="Reset token" required value={form.token}
              onChange={(e) => setForm({ ...form, token: e.target.value })}
              placeholder="0e5c9a1b-…" />
            <Input label="New password" type="password" icon={Lock} required
              value={form.newPassword}
              onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
              hint="At least 8 characters, with upper, lower and a digit." />
            <Input label="Confirm password" type="password" icon={Lock} required
              value={form.confirm} error={fieldError}
              onChange={(e) => setForm({ ...form, confirm: e.target.value })} />
            <Button type="submit" fullWidth size="lg" loading={loading}>Reset password</Button>
          </form>

          <p className="mt-5 text-center text-sm text-slate-500">
            <Link to="/login" className="font-medium text-brand-600 hover:underline">
              Back to log in
            </Link>
          </p>
        </Card>
      </motion.div>
    </div>
  );
}
