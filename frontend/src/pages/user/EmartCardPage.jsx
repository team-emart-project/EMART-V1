import { useState } from 'react';
import { motion } from 'framer-motion';
import { CreditCard, Sparkles } from 'lucide-react';
import { useDispatch } from 'react-redux';
import { Badge, Button, Card, Input, Skeleton } from '@/components/ui';
import AccountNav from './AccountNav';
import { useFetch, usePost } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { formatDate } from '@/utils/formatters';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';

/** Module 4 — apply for the loyalty card and view its points balance. */
export default function EmartCardPage() {
  const dispatch = useDispatch();
  // 404 here just means "no card yet" — not an error worth shouting about.
  const { data: card, loading, error, refetch } = useFetch(endpoints.card.me);
  const { mutate, loading: applying } = usePost();

  const [form, setForm] = useState({ employmentDetails: '', bankAccountNo: '', panNumber: '' });

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  const apply = async (e) => {
    e.preventDefault();
    const { error: err } = await mutate(endpoints.card.apply, {
      ...form, panNumber: form.panNumber.toUpperCase(),
    });
    if (err) dispatch(toastError(err.message));
    else { dispatch(toastSuccess('Application submitted')); refetch(); }
  };

  const hasCard = Boolean(card && !error);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-6 text-3xl font-bold text-slate-900">My account</h1>

      <div className="grid gap-6 lg:grid-cols-[220px_1fr]">
        <AccountNav />

        <div className="space-y-6">
          {loading ? (
            <Skeleton className="h-48 w-full" />
          ) : hasCard ? (
            <>
              <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }}>
                <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-brand-700 via-brand-600 to-brand-800 p-6 text-white shadow-xl">
                  <div className="absolute -right-8 -top-8 h-40 w-40 rounded-full bg-white/10" />
                  <div className="absolute -bottom-12 -left-6 h-40 w-40 rounded-full bg-accent-400/20" />

                  <div className="relative">
                    <div className="flex items-start justify-between">
                      <CreditCard className="h-8 w-8" />
                      <Badge tone={card.status === 'APPROVED' ? 'success' : 'accent'}>
                        {card.status}
                      </Badge>
                    </div>

                    <p className="mt-6 font-mono text-lg tracking-widest">{card.cardNumber}</p>

                    <div className="mt-6 flex items-end justify-between">
                      <div>
                        <p className="text-xs uppercase tracking-wide text-brand-100">Applied</p>
                        <p className="text-sm">{formatDate(card.applicationDate)}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-xs uppercase tracking-wide text-brand-100">e-Points</p>
                        <p className="text-2xl font-bold">{card.pointsBalance}</p>
                      </div>
                    </div>
                  </div>
                </div>
              </motion.div>

              {card.status === 'PENDING' && (
                <Card className="border-amber-200 bg-amber-50">
                  <p className="text-sm text-amber-800">
                    Your application is <strong>pending review</strong>. Member pricing and
                    e-Points activate once it is approved.
                  </p>
                  <p className="mt-2 text-xs text-amber-700">
                    Note: approval is an admin action, and the admin module is out of scope for
                    this phase of the project — so cards stay PENDING for now.
                  </p>
                </Card>
              )}

              <Card>
                <h3 className="mb-3 text-sm font-semibold text-slate-900">Application details</h3>
                <dl className="grid gap-3 text-sm sm:grid-cols-2">
                  <div>
                    <dt className="text-slate-500">Employment</dt>
                    <dd className="text-slate-800">{card.employmentDetails || '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Bank account</dt>
                    <dd className="font-mono text-slate-800">{card.bankAccountMasked || '—'}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Approved on</dt>
                    <dd className="text-slate-800">{formatDate(card.approvalDate)}</dd>
                  </div>
                </dl>
                <p className="mt-3 text-xs text-slate-400">
                  Your PAN is stored securely and is never returned by the API.
                </p>
              </Card>
            </>
          ) : (
            <>
              <Card className="border-brand-200 bg-brand-50/50">
                <div className="flex gap-3">
                  <Sparkles className="h-5 w-5 shrink-0 text-brand-600" />
                  <div className="text-sm text-brand-900">
                    <p className="font-semibold">Why get an e-MART card?</p>
                    <ul className="mt-2 list-inside list-disc space-y-1 text-brand-800">
                      <li>Member pricing on every eligible product</li>
                      <li>Earn e-Points worth 10% of what you spend</li>
                      <li>Redeem points against future orders</li>
                    </ul>
                  </div>
                </div>
              </Card>

              <Card>
                <h2 className="mb-4 text-lg font-semibold text-slate-900">Apply for a card</h2>
                <form onSubmit={apply} className="space-y-4">
                  <Input label="Employment details" name="employmentDetails" required
                    value={form.employmentDetails} onChange={onChange}
                    placeholder="Software Engineer, Infosys Ltd." />
                  <Input label="Bank account number" name="bankAccountNo" required inputMode="numeric"
                    value={form.bankAccountNo} onChange={onChange}
                    placeholder="123456789012" hint="9 to 18 digits" />
                  <Input label="PAN" name="panNumber" required
                    value={form.panNumber} onChange={onChange}
                    placeholder="ABCDE1234F" hint="Format: 5 letters, 4 digits, 1 letter"
                    className="uppercase" />
                  <Button type="submit" fullWidth size="lg" loading={applying}>
                    Submit application
                  </Button>
                </form>
              </Card>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
