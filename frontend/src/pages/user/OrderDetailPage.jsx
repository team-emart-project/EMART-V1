import { useState } from 'react';
import { ArrowLeft, Download, XCircle } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { Badge, Button, Card, EmptyState, Modal, Skeleton } from '@/components/ui';
import { useFetch, usePut } from '@/hooks/useApi';
import axiosClient from '@/api/axiosClient';
import endpoints from '@/api/endpoints';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import { formatPrice, formatDateTime, statusColor } from '@/utils/formatters';

export default function OrderDetailPage() {
  const { orderId } = useParams();
  const dispatch = useDispatch();

  // Same useFetch as the list page — only the URL differs.
  const { data: order, loading, error, refetch } = useFetch(endpoints.orders.byId(orderId));
  const { data: payments } = useFetch(endpoints.payments.byOrder(orderId));
  const { mutate: put, loading: cancelling } = usePut();

  const [confirmOpen, setConfirmOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);

  const cancel = async () => {
    const { error: err } = await put(endpoints.orders.cancel(orderId));
    setConfirmOpen(false);
    if (err) dispatch(toastError(err.message));
    else { dispatch(toastSuccess('Order cancelled')); refetch(); }
  };

  const downloadInvoice = async () => {
    setDownloading(true);
    try {
      const response = await axiosClient.get(endpoints.orders.invoicePdf(orderId), {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.download = `invoice-${order.orderNo}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      dispatch(toastError('The invoice is only available once the order is paid'));
    } finally {
      setDownloading(false);
    }
  };

  if (loading) {
    return <div className="mx-auto max-w-4xl px-4 py-10"><Skeleton className="h-96 w-full" /></div>;
  }

  if (error || !order) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20">
        <EmptyState title="Order not found" message={error?.message}
          action={<Button as={Link} to="/account/orders">Back to orders</Button>} />
      </div>
    );
  }

  const canCancel = order.orderStatus === 'PLACED' && order.paymentStatus === 'PENDING';
  const isPaid = order.paymentStatus === 'PAID';

  return (
    <div className="mx-auto max-w-4xl px-4 py-10 sm:px-6">
      <Link to="/account/orders"
        className="mb-6 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-brand-600">
        <ArrowLeft className="h-4 w-4" /> Back to orders
      </Link>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">{order.orderNo}</h1>
          <p className="mt-1 text-sm text-slate-500">{formatDateTime(order.orderDate)}</p>
          <div className="mt-2 flex flex-wrap gap-2">
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusColor(order.orderStatus)}`}>
              {order.orderStatus}
            </span>
            <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${statusColor(order.paymentStatus)}`}>
              Payment: {order.paymentStatus}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-2">
          {isPaid && (
            <Button onClick={downloadInvoice} loading={downloading}>
              <Download className="h-4 w-4" /> Invoice PDF
            </Button>
          )}
          {order.paymentStatus === 'PENDING' && order.orderStatus !== 'CANCELLED' && (
            <Button as={Link} to={`/payment/${order.orderId}`} variant="accent">Pay now</Button>
          )}
          {canCancel && (
            <Button variant="outline" className="text-rose-600" onClick={() => setConfirmOpen(true)}>
              <XCircle className="h-4 w-4" /> Cancel
            </Button>
          )}
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Shipping address</h2>
          <address className="text-sm not-italic text-slate-600">
            {order.shippingAddress?.addressLine1}<br />
            {order.shippingAddress?.addressLine2 && <>{order.shippingAddress.addressLine2}<br /></>}
            {order.shippingAddress?.city}, {order.shippingAddress?.state} {order.shippingAddress?.zipCode}<br />
            {order.shippingAddress?.country}
          </address>
        </Card>

        <Card>
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Billing address</h2>
          <address className="text-sm not-italic text-slate-600">
            {order.billingAddress?.addressLine1}<br />
            {order.billingAddress?.addressLine2 && <>{order.billingAddress.addressLine2}<br /></>}
            {order.billingAddress?.city}, {order.billingAddress?.state} {order.billingAddress?.zipCode}<br />
            {order.billingAddress?.country}
          </address>
        </Card>
      </div>

      <Card className="mt-6">
        <h2 className="mb-4 text-sm font-semibold text-slate-900">Items</h2>
        <div className="space-y-3">
          {order.items?.map((item) => (
            <div key={item.orderDtlId} className="flex items-start justify-between gap-4 border-b border-slate-100 pb-3 last:border-0 last:pb-0">
              <div className="min-w-0">
                <p className="text-sm font-medium text-slate-800">{item.prodName}</p>
                <p className="text-xs text-slate-500">
                  {formatPrice(item.priceCharged)} × {item.quantity}
                  {Number(item.lineSavings) > 0 && (
                    <span className="ml-2 text-emerald-600">
                      saved {formatPrice(item.lineSavings)}
                    </span>
                  )}
                </p>
                {item.pointsRedeemed > 0 && (
                  <Badge tone="accent" className="mt-1">{item.pointsRedeemed} pts used</Badge>
                )}
              </div>
              <span className="shrink-0 text-sm font-semibold">{formatPrice(item.lineTotal)}</span>
            </div>
          ))}
        </div>

        <dl className="mt-5 space-y-2 border-t border-slate-100 pt-4 text-sm">
          <div className="flex justify-between">
            <dt className="text-slate-500">Subtotal</dt><dd>{formatPrice(order.subtotalAmount)}</dd>
          </div>
          {Number(order.totalSavings) > 0 && (
            <div className="flex justify-between text-emerald-600">
              <dt>Member savings</dt><dd>-{formatPrice(order.totalSavings)}</dd>
            </div>
          )}
          {Number(order.pointsRedeemed) > 0 && (
            <div className="flex justify-between text-accent-600">
              <dt>e-Points spent</dt><dd>{order.pointsRedeemed}</dd>
            </div>
          )}
          <div className="flex justify-between border-t border-slate-100 pt-2 text-base font-bold">
            <dt>Total</dt><dd>{formatPrice(order.totalAmount)}</dd>
          </div>
          {order.pointsEarned > 0 && (
            <p className="rounded-lg bg-accent-500/10 px-3 py-2 text-xs text-accent-700">
              Earned {order.pointsEarned} e-Points on this order.
            </p>
          )}
        </dl>
      </Card>

      {payments?.length > 0 && (
        <Card className="mt-6">
          <h2 className="mb-3 text-sm font-semibold text-slate-900">Payment attempts</h2>
          <div className="space-y-2">
            {payments.map((p) => (
              <div key={p.paymentId} className="flex items-center justify-between text-sm">
                <div>
                  <p className="text-slate-700">
                    {p.paymentMethod} ···· {p.cardLast4}
                  </p>
                  <p className="text-xs text-slate-400">{formatDateTime(p.transactionDate)}</p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusColor(p.status)}`}>
                  {p.status}
                </span>
              </div>
            ))}
          </div>
        </Card>
      )}

      <Modal open={confirmOpen} onClose={() => setConfirmOpen(false)} title="Cancel this order?"
        footer={
          <div className="flex justify-end gap-2">
            <Button variant="ghost" onClick={() => setConfirmOpen(false)}>Keep order</Button>
            <Button variant="danger" loading={cancelling} onClick={cancel}>Yes, cancel</Button>
          </div>
        }>
        <p className="text-sm text-slate-600">
          This cannot be undone. Only unpaid orders can be cancelled.
        </p>
      </Modal>
    </div>
  );
}
