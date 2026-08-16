import { useState } from 'react';
import { motion } from 'framer-motion';
import { Mail, User, Phone, Lock, Store } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Button, Card, Input } from '@/components/ui';
import GoogleSignInButton from '@/components/common/GoogleSignInButton';
import useAuth from '@/hooks/useAuth';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

/** Registration form — mirrors the fields the BRD asks for. */
export default function RegisterPage() {
  const { register, loading, error } = useAuth();
  const navigate = useNavigate();
  const dispatch = useDispatch();

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', password: '', phone: '',
    dob: '', gender: '', education: '', occupation: '',
    annualIncome: '', marketingConsent: true,
  });

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const payload = {
      ...form,
      // Send null rather than "" so backend validation treats them as absent.
      dob: form.dob || null,
      annualIncome: form.annualIncome ? Number(form.annualIncome) : null,
      lastName: form.lastName || null,
      phone: form.phone || null,
    };
    const ok = await register(payload);
    if (ok) {
      dispatch(toastSuccess('Account created. Your membership number was emailed to you.'));
      navigate('/login');
    } else {
      dispatch(toastError('Registration failed'));
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)] items-center justify-center px-4 py-12">
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }} className="w-full max-w-2xl">

        <div className="mb-6 text-center">
<<<<<<< HEAD
          <span className="mb-3 inline-flex rounded-2xl bg-brand-600 p-3">
=======
          <span className="mb-3 inline-flex rounded-2xl bg-brand-gradient p-3 shadow-lg shadow-brand-600/25">
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
            <Store className="h-6 w-6 text-white" />
          </span>
          <h1 className="text-2xl font-bold text-slate-900">Create your account</h1>
          <p className="mt-1 text-sm text-slate-500">
            Membership is free. You need an account to buy.
          </p>
        </div>

        <Card>
          {error && (
            <div className="mb-4 rounded-xl bg-rose-50 px-3 py-2 text-sm text-rose-700">{error}</div>
          )}

          <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2">
            <Input label="First name" name="firstName" required icon={User}
              value={form.firstName} onChange={onChange} />
            <Input label="Last name" name="lastName" icon={User}
              value={form.lastName} onChange={onChange} />

            <div className="sm:col-span-2">
              <Input label="Email" name="email" type="email" required icon={Mail}
                value={form.email} onChange={onChange} placeholder="you@example.com" />
            </div>

            <div className="sm:col-span-2">
              <Input label="Password" name="password" type="password" required icon={Lock}
                value={form.password} onChange={onChange}
                hint="At least 8 characters, with an uppercase letter, a lowercase letter and a digit." />
            </div>

            <Input label="Phone" name="phone" icon={Phone}
              value={form.phone} onChange={onChange} placeholder="9876543210" />
            <Input label="Date of birth" name="dob" type="date"
              value={form.dob} onChange={onChange} />

            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">Gender</label>
              <select name="gender" value={form.gender} onChange={onChange}
                className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/30">
                <option value="">Prefer not to say</option>
                <option>Male</option><option>Female</option><option>Other</option>
              </select>
            </div>

            <Input label="Education" name="education"
              value={form.education} onChange={onChange} placeholder="B.Tech" />
            <Input label="Occupation" name="occupation"
              value={form.occupation} onChange={onChange} placeholder="Software Engineer" />
            <Input label="Annual income" name="annualIncome" type="number" min="0"
              value={form.annualIncome} onChange={onChange} placeholder="800000" />

            <label className="flex items-start gap-2 sm:col-span-2">
              <input type="checkbox" name="marketingConsent" checked={form.marketingConsent}
                onChange={onChange} className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600" />
              <span className="text-sm text-slate-600">
                Email me about promotions and discount offers.
              </span>
            </label>

            <div className="sm:col-span-2">
              <Button type="submit" fullWidth size="lg" loading={loading}>Create account</Button>
            </div>
          </form>

          {/* Same endpoint as the login page: /api/auth/google registers on
              first use, so there is no separate "sign up with Google" path that
              could drift out of step with the sign-in one. */}
          <div className="my-5 flex items-center gap-3">
            <span className="h-px flex-1 bg-slate-200" />
            <span className="text-xs font-medium uppercase tracking-wide text-slate-400">or</span>
            <span className="h-px flex-1 bg-slate-200" />
          </div>

          <GoogleSignInButton text="signup_with" />

          <p className="mt-5 text-center text-sm text-slate-500">
            Already a member?{' '}
            <Link to="/login" className="font-medium text-brand-600 hover:underline">Log in</Link>
          </p>
        </Card>
      </motion.div>
    </div>
  );
}
