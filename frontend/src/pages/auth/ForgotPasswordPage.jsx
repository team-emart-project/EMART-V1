import { useState } from 'react';
import { motion } from 'framer-motion';
import { KeyRound, Mail } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Button, Card, Input } from '@/components/ui';
import { usePost } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';

export default function ForgotPasswordPage() {
  const { mutate, loading } = usePost();
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    await mutate(endpoints.auth.forgotPassword, { email });
    // The backend always returns 200 (it will not reveal whether the email
    // exists), so we always show the same confirmation.
    setSent(true);
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md">
        <div className="mb-6 text-center">
          <span className="mb-3 inline-flex rounded-2xl bg-brand-600 p-3">
            <KeyRound className="h-6 w-6 text-white" />
          </span>
          <h1 className="text-2xl font-bold text-slate-900">Forgot your password?</h1>
          <p className="mt-1 text-sm text-slate-500">
            We will send a reset link to your registered email.
          </p>
        </div>

        <Card>
          {sent ? (
            <div className="text-center">
              <p className="rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
                If that email is registered, a reset link has been sent to it.
              </p>
              <p className="mt-3 text-xs text-slate-500">
                This project logs email instead of sending it — copy the token from the
                backend console, then continue below.
              </p>
              <Button as={Link} to="/reset-password" variant="outline" fullWidth className="mt-4">
                I have my reset token
              </Button>
            </div>
          ) : (
            <form onSubmit={onSubmit} className="space-y-4">
              <Input label="Email" type="email" icon={Mail} required value={email}
                onChange={(e) => setEmail(e.target.value)} placeholder="you@example.com" />
              <Button type="submit" fullWidth size="lg" loading={loading}>Send reset link</Button>
            </form>
          )}

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
