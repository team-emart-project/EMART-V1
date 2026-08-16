import { useState } from 'react';
import { motion } from 'framer-motion';
import { CheckCircle2, CreditCard, Download, Lock, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Badge, Button, Card, Input, Skeleton } from '@/components/ui';
import { useFetch, usePost } from '@/hooks/useApi';
import axiosClient from '@/api/axiosClient';
import endpoints from '@/api/endpoints';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import { formatPlain, formatPrice } from '@/utils/formatters';

/** Module 9 — mock payment. A card ending in 0 is declined by the backend. */
export default function PaymentPage() {
  const { orderId } = useParams();
  const dispatch = useDispatch();

  const { data: order, loading, refetch } = useFetch(endpoints.orders.byId(orderId));
  const { mutate: pay, loading: paying } = usePost();

  const [form, setForm] = useState({
    cardNumber: '4242424242424242', cardHolderName: '', expiry: '12/29', cvv: '123',
  });
  const [result, setResult] = useState(null);
  const [downloading, setDownloading] = useState(false);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  // An order paid entirely with e-Points has nothing to charge. Asking for card
  // details to collect zero rupees is confusing, so the form is replaced by a
  // single confirm button. The request still goes to the same endpoint - the
  // backend settles the points and marks the order paid either way.
  const nothingToPay = order != null && Number(order.totalAmount) === 0;

  const submit = async (e) => {
    e.preventDefault();

    // The amount MUST match the server's total exactly — that check is what
    // catches a tampered client, so we send the order's own figure.
    //
    // On a zero-total order the card fields are omitted entirely rather than
    // sent blank: the backend treats a missing card as "nothing to charge",
    // and sending empty strings would only trip the format validation.
    const body = Number(order.totalAmount) === 0
      ? { amount: order.totalAmount }
      : { ...form, cardNumber: form.cardNumber.replace(/\s/g, ''), amount: order.totalAmount };

    const { data, error } = await pay(endpoints.payments.verify(orderId), body);

    if (error) { dispatch(toastError(error.message)); return; }

    setResult(data);
    refetch();
    dispatch(data.status === 'SUCCESS'
      ? toastSuccess('Payment successful')
      : toastError('Payment declined — try a different card'));
  };

  /** The invoice endpoint returns raw PDF bytes, so we ask axios for a blob. */
  const downloadInvoice = async () => {
    setDownloading(true);
    try {
      const response = await axiosClient.get(endpoints.orders.invoicePdf(orderId), {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${order.orderNo || orderId}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      dispatch(toastError('Could not download the invoice'));
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return <div className="mx-auto max-w-3xl px-4 py-10"><Skeleton className="h-96 w-full" /></div>;
  }

  const alreadyPaid = order?.paymentStatus === 'PAID';
  const succeeded = result?.status === 'SUCCESS' || alreadyPaid;

  return (
    <div className="mx-auto max-w-3xl px-4 py-10 sm:px-6">
      {succeeded ? (
        <motion.div initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}>
          <Card className="text-center">
            <motion.span initial={{ scale: 0 }} animate={{ scale: 1 }}
              transition={{ type: 'spring', delay: 0.15 }}
              className="mx-auto mb-4 inline-flex rounded-full bg-emerald-100 p-4">
              <CheckCircle2 className="h-10 w-10 text-emerald-600" />
            </motion.span>

            <h1 className="text-2xl font-bold text-slate-900">Payment successful</h1>
            <p className="mt-1 text-sm text-slate-500">
              Order <strong>{order.orderNo}</strong> is confirmed.
            </p>

            {result?.pointsEarned > 0 && (
              <Badge tone="accent" className="mt-3">
                +{result.pointsEarned} e-Points earned
                {result.pointsBalanceAfter != null && ` · balance ${result.pointsBalanceAfter}`}
              </Badge>
            )}

            <div className="mt-6 flex flex-wrap justify-center gap-3">
              <Button onClick={downloadInvoice} loading={downloading}>
                <Download className="h-4 w-4" /> Download invoice
              </Button>
              <Button as={Link} to={`/account/orders/${orderId}`} variant="outline">
                View order
              </Button>
              <Button as={Link} to="/products" variant="ghost">Keep shopping</Button>
            </div>
          </Card>
        </motion.div>
      ) : (
        <>
          <h1 className="mb-2 text-3xl font-bold text-slate-900">Payment</h1>
          <p className="mb-8 text-sm text-slate-500">
            Order <strong>{order?.orderNo}</strong> · {formatPrice(order?.totalAmount)}
            {Number(order?.pointsRedeemed) > 0 &&
              ` + ${formatPlain(order.pointsRedeemed)} e-Points`}
          </p>

          {result?.status === 'FAILED' && (
            <div className="mb-5 flex items-start gap-2 rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">
              <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
              <span>Payment was declined. Your order is still reserved — try another card.</span>
            </div>
          )}

          <div className="grid gap-6 md:grid-cols-[1fr_280px]">
            <Card>
              <h2 className="mb-4 flex items-center gap-2 text-lg font-semibold text-slate-900">
                <CreditCard className="h-5 w-5 text-brand-500" />
                {nothingToPay ? 'Confirm your order' : 'Card details'}
              </h2>

              {nothingToPay ? (
                <form onSubmit={submit} className="space-y-4">
                  <div className="rounded-xl bg-accent-500/10 px-4 py-3 text-sm text-accent-800">
                    This order is fully covered by{' '}
                    <strong>{formatPlain(order?.pointsRedeemed)} e-Points</strong>.
                    There is nothing to pay, so no card is needed.
                  </div>
                  <Button type="submit" fullWidth size="lg" loading={paying}>
                    <Lock className="h-4 w-4" /> Confirm and place order
                  </Button>
                </form>
              ) : (
              <form onSubmit={submit} className="space-y-4">
                <Input label="Card number" name="cardNumber" required inputMode="numeric"
                  value={form.cardNumber} onChange={onChange} placeholder="4242 4242 4242 4242" />
                <Input label="Name on card" name="cardHolderName" required
                  value={form.cardHolderName} onChange={onChange} placeholder="RISHI CHHALOTRE" />

                <div className="grid grid-cols-2 gap-4">
                  <Input label="Expiry" name="expiry" required placeholder="MM/YY"
                    value={form.expiry} onChange={onChange} />
                  <Input label="CVV" name="cvv" type="password" required inputMode="numeric"
                    value={form.cvv} onChange={onChange} placeholder="123" />
                </div>

                <Button type="submit" fullWidth size="lg" loading={paying}>
                  <Lock className="h-4 w-4" /> Pay {formatPrice(order?.totalAmount)}
                </Button>
              </form>
              )}

              {!nothingToPay && (
              <div className="mt-4 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-3 text-xs text-slate-500">
                <p className="font-medium text-slate-600">Mock gateway</p>
                <p className="mt-1">
                  <code>4242424242424242</code> succeeds.
                  Any card ending in <strong>0</strong> is declined — try{' '}
                  <code>4242424242424240</code> to see the failure path.
                </p>
              </div>
              )}
            </Card>

            <Card className="h-fit">
              <h3 className="mb-3 text-sm font-semibold text-slate-900">Order total</h3>
              <dl className="space-y-2 text-sm">
                <div className="flex justify-between">
                  <dt className="text-slate-500">Cash</dt>
                  <dd>{formatPrice(order?.subtotalAmount)}</dd>
                </div>
                {Number(order?.pointsRedeemed) > 0 && (
                  <div className="flex justify-between text-accent-700">
                    <dt>e-Points</dt>
                    <dd className="font-semibold">{formatPlain(order.pointsRedeemed)}</dd>
                  </div>
                )}
                <div className="flex justify-between border-t border-slate-100 pt-2 font-bold">
                  <dt>Total to pay</dt><dd>{formatPrice(order?.totalAmount)}</dd>
                </div>
              </dl>
            </Card>
          </div>
        </>
      )}
    </div>
  );
}
