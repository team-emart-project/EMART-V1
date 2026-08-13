import { useEffect, useState } from 'react';
import { Save } from 'lucide-react';
import { useDispatch } from 'react-redux';
import { Badge, Button, Card, Input, Skeleton } from '@/components/ui';
import AccountNav from './AccountNav';
import { useFetch, usePut } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { fetchCurrentUser } from '@/store/slices/authSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

export default function ProfilePage() {
  const dispatch = useDispatch();
  const { data: user, loading, refetch } = useFetch(endpoints.users.me);
  const { mutate, loading: saving } = usePut();
  const [form, setForm] = useState(null);

  useEffect(() => {
    if (user) {
      setForm({
        firstName: user.firstName || '', lastName: user.lastName || '',
        phone: user.phone || '', dob: user.dob || '', gender: user.gender || '',
        education: user.education || '', occupation: user.occupation || '',
        annualIncome: user.annualIncome ?? '', marketingConsent: user.marketingConsent ?? false,
      });
    }
  }, [user]);

  const onChange = (e) => {
    const { name, value, type, checked } = e.target;
    setForm({ ...form, [name]: type === 'checkbox' ? checked : value });
  };

  const onSubmit = async (e) => {
    e.preventDefault();
    const { error } = await mutate(endpoints.users.me, {
      ...form,
      dob: form.dob || null,
      annualIncome: form.annualIncome === '' ? null : Number(form.annualIncome),
    });
    if (error) dispatch(toastError(error.message));
    else {
      dispatch(toastSuccess('Profile updated'));
      dispatch(fetchCurrentUser());
      refetch();
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-3xl font-bold text-slate-900">My account</h1>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        <AccountNav />

        <Card>
          {loading || !form ? (
            <div className="space-y-3">
              <Skeleton className="h-6 w-1/3" /><Skeleton className="h-24 w-full" />
            </div>
          ) : (
            <>
              <div className="mb-6 flex flex-wrap items-center gap-3 border-b border-slate-100 pb-5">
                <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-brand-gradient text-lg font-bold text-white shadow-md shadow-brand-600/25">
                  {user.firstName?.[0]?.toUpperCase()}
                </span>
                <div>
                  <p className="font-semibold text-slate-900">
                    {user.firstName} {user.lastName}
                  </p>
                  <p className="text-sm text-slate-500">{user.email}</p>
                  <div className="mt-1 flex flex-wrap gap-2">
                    <Badge tone="brand">{user.membershipNo}</Badge>
                    {user.cardholder && <Badge tone="accent">e-MART cardholder</Badge>}
                  </div>
                </div>
              </div>

              <form onSubmit={onSubmit} className="grid gap-4 sm:grid-cols-2">
                <Input label="First name" name="firstName" required
                  value={form.firstName} onChange={onChange} />
                <Input label="Last name" name="lastName" value={form.lastName} onChange={onChange} />
                <Input label="Phone" name="phone" value={form.phone} onChange={onChange} />
                <Input label="Date of birth" name="dob" type="date" value={form.dob} onChange={onChange} />

                <div>
                  <label className="mb-1.5 block text-sm font-medium text-slate-700">Gender</label>
                  <select name="gender" value={form.gender} onChange={onChange}
                    className="w-full rounded-xl border border-slate-300 bg-white px-3.5 py-2.5 text-sm">
                    <option value="">Prefer not to say</option>
                    <option>Male</option><option>Female</option><option>Other</option>
                  </select>
                </div>

                <Input label="Education" name="education" value={form.education} onChange={onChange} />
                <Input label="Occupation" name="occupation" value={form.occupation} onChange={onChange} />
                <Input label="Annual income" name="annualIncome" type="number" min="0"
                  value={form.annualIncome} onChange={onChange} />

                <label className="flex items-start gap-2 sm:col-span-2">
                  <input type="checkbox" name="marketingConsent" checked={form.marketingConsent}
                    onChange={onChange} className="mt-1 h-4 w-4 rounded border-slate-300 text-brand-600" />
                  <span className="text-sm text-slate-600">Email me about promotions and offers.</span>
                </label>

                {/* Email, password and role are not editable here — the backend
                    DTO has no such fields, which is what prevents mass assignment. */}
                <p className="text-xs text-slate-400 sm:col-span-2">
                  Your email and membership number cannot be changed here.
                </p>

                <div className="sm:col-span-2">
                  <Button type="submit" loading={saving}>
                    <Save className="h-4 w-4" /> Save changes
                  </Button>
                </div>
              </form>
            </>
          )}
        </Card>
      </div>
    </div>
  );
}
