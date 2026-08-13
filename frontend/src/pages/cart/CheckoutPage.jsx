import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { CreditCard, MapPin, Plus } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Badge, Button, Card, EmptyState, Skeleton } from '@/components/ui';
import { useFetch, usePost } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { fetchCart, selectCart } from '@/store/slices/cartSlice';
import { toastError } from '@/store/slices/uiSlice';
import { formatPlain, formatPrice, productImage, placeholderImage } from '@/utils/formatters';

/** Module 8 — checkout. Preview is recalculated whenever the address changes. */
export default function CheckoutPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const cart = useSelector(selectCart);

  const { data: addresses, loading: loadingAddr } = useFetch(endpoints.users.addresses);
  const { mutate: postPreview, loading: previewing } = usePost();
  const { mutate: postOrder, loading: placing } = usePost();

  const [shippingId, setShippingId] = useState(null);
  const [billingId, setBillingId] = useState(null);
  const [sameAsShipping, setSameAsShipping] = useState(true);
  const [preview, setPreview] = useState(null);

  useEffect(() => { dispatch(fetchCart()); }, [dispatch]);

  // Default to the address the user marked as default.
  useEffect(() => {
    if (addresses?.length && !shippingId) {
      const def = addresses.find((a) => a.isDefault) || addresses[0];
      setShippingId(def.addressId);
      setBillingId(def.addressId);
    }
  }, [addresses, shippingId]);

  // Ask the server for the totals — never compute money on the client.
  useEffect(() => {
    if (!shippingId || !cart.items?.length) return;
    (async () => {
      const { data, error } = await postPreview(endpoints.orders.preview, {
        shippingAddressId: shippingId,
        billingAddressId: sameAsShipping ? shippingId : billingId,
      });
      if (error) dispatch(toastError(error.message));
      else setPreview(data);
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [shippingId, billingId, sameAsShipping, cart.items?.length]);

  const placeOrder = async () => {
    const { data, error } = await postOrder(endpoints.orders.root, {
      shippingAddressId: shippingId,
      billingAddressId: sameAsShipping ? shippingId : billingId,
    });
    if (error) { dispatch(toastError(error.message)); return; }
    dispatch(fetchCart());
    navigate(`/payment/${data.orderId}`);
  };

  if (!cart.items?.length) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20">
        <EmptyState title="Your cart is empty"
          message="Add something before checking out."
          action={<Button as={Link} to="/products">Browse products</Button>} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-8 text-3xl font-bold text-slate-900">Checkout</h1>

      <div className="grid gap-8 lg:grid-cols-[1fr_380px]">
        <div className="space-y-6">
          <Card>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="flex items-center gap-2 text-lg font-semibold text-slate-900">
                <MapPin className="h-5 w-5 text-brand-500" /> Delivery address
              </h2>
              <Button as={Link} to="/account/addresses" variant="ghost" size="sm">
                <Plus className="h-3.5 w-3.5" /> Manage
              </Button>
            </div>

            {loadingAddr ? (
              <Skeleton className="h-24 w-full" />
            ) : addresses?.length ? (
              <div className="space-y-2">
                {addresses.map((a) => (
                  <label key={a.addressId}
                    className={`flex cursor-pointer gap-3 rounded-xl border p-3 transition-colors ${
                      shippingId === a.addressId
                        ? 'border-brand-500 bg-brand-50/50'
                        : 'border-slate-200 hover:border-slate-300'
                    }`}>
                    <input type="radio" name="shipping" checked={shippingId === a.addressId}
                      onChange={() => setShippingId(a.addressId)}
                      className="mt-1 h-4 w-4 text-brand-600" />
                    <div className="text-sm">
                      <p className="font-medium text-slate-800">
                        {a.addressLine1}
                        {a.isDefault && <Badge tone="brand" className="ml-2">Default</Badge>}
                      </p>
                      {a.addressLine2 && <p className="text-slate-500">{a.addressLine2}</p>}
                      <p className="text-slate-500">
                        {a.city}, {a.state} {a.zipCode} · {a.country}
                      </p>
                    </div>
                  </label>
                ))}

                <label className="mt-3 flex items-center gap-2 text-sm text-slate-600">
                  <input type="checkbox" checked={sameAsShipping}
                    onChange={(e) => setSameAsShipping(e.target.checked)}
                    className="h-4 w-4 rounded border-slate-300 text-brand-600" />
                  Billing address is the same as shipping
                </label>

                {!sameAsShipping && (
                  <div className="mt-2 space-y-2 border-t border-slate-100 pt-3">
                    <p className="text-sm font-medium text-slate-700">Billing address</p>
                    {addresses.map((a) => (
                      <label key={a.addressId} className="flex cursor-pointer gap-3 rounded-xl border border-slate-200 p-3 text-sm">
                        <input type="radio" name="billing" checked={billingId === a.addressId}
                          onChange={() => setBillingId(a.addressId)} className="mt-1 h-4 w-4" />
                        <span>{a.addressLine1}, {a.city}</span>
                      </label>
                    ))}
                  </div>
                )}
              </div>
            ) : (
              <EmptyState icon={MapPin} title="No saved addresses"
                message="Add a delivery address to continue."
                action={<Button as={Link} to="/account/addresses">Add an address</Button>} />
            )}
          </Card>

          <Card>
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Items</h2>
            <div className="space-y-3">
              {cart.items.map((i) => (
                <div key={i.cartItemId} className="flex items-center gap-3">
                  <img src={productImage(i.prodImagePath, i.prodName)} alt={i.prodName}
                    className="h-14 w-14 rounded-lg border border-slate-100 object-cover"  onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }} />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-slate-800">{i.prodName}</p>
                    <p className="text-xs text-slate-500">
                      Qty {i.quantity}
                      {i.priceOption && i.priceOption !== 'REGULAR' && (
                        <span className="ml-1.5 text-brand-600">
                          {i.priceOption === 'MEMBER' && 'eMcard price'}
                          {i.priceOption === 'POINTS' && 'paid with e-Points'}
                          {i.priceOption === 'HYBRID' && 'cash + e-Points'}
                        </span>
                      )}
                    </p>
                  </div>
                  <div className="text-right">
                    <div className="text-sm font-medium">{formatPrice(i.lineTotal)}</div>
                    {Number(i.pointsUsed) > 0 && (
                      <div className="text-xs text-accent-700">
                        + {formatPlain(i.pointsUsed)} e-Points
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </div>

        {/* ---- totals ---- */}
        <div>
          <motion.div initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
            <Card className="sticky top-24">
              <h2 className="mb-4 text-lg font-semibold text-slate-900">Summary</h2>

              {previewing && !preview ? (
                <Skeleton className="h-40 w-full" />
              ) : preview ? (
                <dl className="space-y-2.5 text-sm">
                  <div className="flex justify-between">
                    <dt className="text-slate-500">Total at regular price</dt>
                    <dd>{formatPrice(preview.subtotalMrp)}</dd>
                  </div>
                  {Number(preview.totalSavings) > 0 && (
                    <div className="flex justify-between text-emerald-600">
                      <dt>eMcard savings</dt><dd>-{formatPrice(preview.totalSavings)}</dd>
                    </div>
                  )}

                  {/* No tax row: this project charges exactly the listed price.
                      Points are NOT subtracted here either - a line bought with
                      points already has a cash price of zero inside the
                      subtotal, so subtracting again would double-count. */}
                  {preview.pointsRedeemed > 0 && (
                    <div className="flex justify-between text-accent-700">
                      <dt>e-Points you will spend</dt>
                      <dd className="font-semibold">{formatPlain(preview.pointsRedeemed)}</dd>
                    </div>
                  )}

                  <div className="flex justify-between border-t border-slate-100 pt-3 text-base font-bold">
                    <dt className="text-slate-900">Total to pay</dt>
                    <dd className="text-slate-900">{formatPrice(preview.totalAmount)}</dd>
                  </div>

                  {Number(preview.totalAmount) === 0 && (
                    <p className="rounded-lg bg-accent-500/10 px-3 py-2 text-xs font-medium text-accent-700">
                      Fully covered by e-Points — nothing to pay.
                    </p>
                  )}

                  {preview.pointsEarned > 0 && (
                    <p className="rounded-lg bg-emerald-50 px-3 py-2 text-xs text-emerald-800">
                      You will earn <strong>{formatPlain(preview.pointsEarned)} e-Points</strong> on this order.
                    </p>
                  )}
                </dl>
              ) : (
                <p className="text-sm text-slate-500">Select an address to see your total.</p>
              )}

              <Button fullWidth size="lg" className="mt-5" loading={placing}
                disabled={!shippingId || !preview} onClick={placeOrder}>
                <CreditCard className="h-4 w-4" /> Place order
              </Button>

              <p className="mt-2 text-center text-xs text-slate-400">
                You will pay on the next step.
              </p>
            </Card>
          </motion.div>
        </div>
      </div>
    </div>
  );
}
