import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ArrowRight, ShoppingBag, Trash2 } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Badge, Button, Card, EmptyState, ListSkeleton } from '@/components/ui';
import QuantityStepper from '@/components/common/QuantityStepper';
import { buildPriceOptions } from '@/components/common/PriceOptions';
import {
  clearCart, fetchCart, removeCartItem, selectCart, selectCartBusy, updateCartItem,
} from '@/store/slices/cartSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import { formatPlain, formatPrice, productImage, placeholderImage } from '@/utils/formatters';

/**
 * One selectable payment option on a cart line.
 *
 * A button rather than a checkbox here: in the cart the choice is already made,
 * so this is "switch to this", not "tick to add". Buttons also give a single
 * unambiguous click target, which matters because each press hits the API.
 */
function OptionChip({ active, disabled, onClick, label }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled || active}
      className={`rounded-full border px-2.5 py-1 text-xs transition
        ${active
          ? 'border-brand-300 bg-brand-100 font-semibold text-brand-800'
          : 'border-slate-200 bg-white text-slate-600 hover:border-brand-300 hover:text-brand-700 disabled:opacity-50'}`}
    >
      {label}
    </button>
  );
}

export default function CartPage() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const cart = useSelector(selectCart);
  const busy = useSelector(selectCartBusy);
  const status = useSelector((s) => s.cart.status);

  useEffect(() => { dispatch(fetchCart()); }, [dispatch]);

  const changeQty = (item, quantity) =>
    dispatch(updateCartItem({
      // priceOption is omitted on purpose: the stepper only changes the
      // quantity, and the backend keeps whatever option the line already had.
      cartItemId: item.cartItemId, quantity,
    }));

  // Switching how a line is paid for, from inside the cart.
  //
  // Without this, a shopper who ticked the wrong box (or forgot to tick one)
  // has to delete the line and start again from the product page. The cart is
  // where people review a decision, so it is where the decision must be
  // changeable.
  const changeOption = async (item, priceOption) => {
    const result = await dispatch(updateCartItem({
      cartItemId: item.cartItemId, quantity: item.quantity, priceOption,
    }));
    if (!updateCartItem.fulfilled.match(result)) {
      // The backend refuses e.g. a points purchase you cannot afford, and says
      // exactly why. Surface its message rather than a generic failure.
      dispatch(toastError(result.payload || 'Could not change the price option'));
    }
  };

  const remove = async (item) => {
    const result = await dispatch(removeCartItem(item.cartItemId));
    dispatch(removeCartItem.fulfilled.match(result)
      ? toastSuccess('Removed from cart')
      : toastError('Could not remove item'));
  };

  if (status === 'loading' && !cart.items.length) {
    return <div className="mx-auto max-w-5xl px-4 py-10"><ListSkeleton rows={3} /></div>;
  }

  if (!cart.items?.length) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20">
        <EmptyState icon={ShoppingBag} title="Your cart is empty"
          message="Browse the catalog and add something you like."
          action={<Button as={Link} to="/products">Start shopping</Button>} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-8 flex items-end justify-between">
        <div>
          <h1 className="text-3xl font-bold text-slate-900">Your cart</h1>
          <p className="mt-1 text-sm text-slate-500">
            {cart.totalQuantity} item{cart.totalQuantity === 1 ? '' : 's'}
          </p>
        </div>
        <Button variant="ghost" size="sm" onClick={() => dispatch(clearCart())} disabled={busy}>
          <Trash2 className="h-3.5 w-3.5" /> Clear cart
        </Button>
      </div>

      <div className="grid gap-8 lg:grid-cols-[1fr_360px]">
        <div className="space-y-3">
          <AnimatePresence mode="popLayout">
            {cart.items.map((item) => (
              <motion.div key={item.cartItemId} layout
                initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, x: -30 }} transition={{ duration: 0.25 }}>
                <Card className="flex gap-4">
                  <Link to={`/products/${item.prodId}`} className="shrink-0">
                    <img src={productImage(item.prodImagePath, item.prodName)} alt={item.prodName}
                      className="h-24 w-24 rounded-xl border border-slate-100 object-cover"  onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }} />
                  </Link>

                  <div className="min-w-0 flex-1">
                    <Link to={`/products/${item.prodId}`}
                      className="line-clamp-2 font-medium text-slate-800 hover:text-brand-600">
                      {item.prodName}
                    </Link>

                    {/* What this line costs per unit, and what it would have
                        cost at the regular price. */}
                    <div className="mt-1 flex flex-wrap items-center gap-2 text-sm">
                      <span className="font-semibold text-slate-900">
                        {Number(item.unitPriceApplied) > 0
                          ? formatPrice(item.unitPriceApplied)
                          : 'No cash to pay'}
                      </span>
                      {Number(item.unitPointsApplied) > 0 && (
                        <span className="font-semibold text-accent-700">
                          {Number(item.unitPriceApplied) > 0 ? '+ ' : ''}
                          {formatPlain(item.unitPointsApplied)} e-Points
                        </span>
                      )}
                      {Number(item.lineSavings) > 0 && (
                        <>
                          <span className="text-slate-400 line-through">
                            {formatPrice(item.mrpPrice)}
                          </span>
                          <Badge tone="success">
                            saves {formatPrice(item.lineSavings)}
                          </Badge>
                        </>
                      )}
                    </div>

                    {/* How this line is being paid for, and how to change it. */}
                    <div className="mt-2.5 rounded-lg border border-slate-100 bg-slate-50/70 p-2">
                      <p className="mb-1 text-[11px] font-semibold uppercase tracking-wide text-slate-400">
                        Paying with
                      </p>
                      <div className="flex flex-wrap gap-1.5">
                        <OptionChip
                          active={!item.priceOption || item.priceOption === 'REGULAR'}
                          disabled={busy}
                          onClick={() => changeOption(item, 'REGULAR')}
                          label={`Regular ${formatPrice(item.mrpPrice)}`}
                        />
                        {buildPriceOptions(item).map((option) => (
                          <OptionChip
                            key={option.value}
                            active={item.priceOption === option.value}
                            disabled={busy}
                            onClick={() => changeOption(item, option.value)}
                            label={option.label}
                          />
                        ))}
                      </div>
                    </div>

                    <div className="mt-3 flex items-center justify-between">
                      <QuantityStepper value={item.quantity} disabled={busy}
                        onChange={(q) => changeQty(item, q)} />
                      <div className="flex items-center gap-3">
                        <div className="text-right">
                          <div className="font-semibold text-slate-900">
                            {Number(item.lineTotal) > 0 ? formatPrice(item.lineTotal) : formatPrice(0)}
                          </div>
                          {Number(item.pointsUsed) > 0 && (
                            <div className="text-xs font-medium text-accent-700">
                              + {formatPlain(item.pointsUsed)} e-Points
                            </div>
                          )}
                        </div>
                        <button onClick={() => remove(item)} disabled={busy}
                          className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-rose-50 hover:text-rose-600"
                          aria-label="Remove item">
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  </div>
                </Card>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>

        {/* ---- summary ---- */}
        <div>
          <Card className="sticky top-24">
            <h2 className="mb-4 text-lg font-semibold text-slate-900">Order summary</h2>

            <dl className="space-y-2.5 text-sm">
              <div className="flex justify-between">
                <dt className="text-slate-500">Total at regular price</dt>
                <dd className="text-slate-700">{formatPrice(cart.subtotalMrp)}</dd>
              </div>

              {Number(cart.totalSavings) > 0 && (
                <div className="flex justify-between text-emerald-600">
                  <dt>eMcard savings</dt>
                  <dd>-{formatPrice(cart.totalSavings)}</dd>
                </div>
              )}

              <div className="border-t border-slate-100 pt-3 flex justify-between text-base font-semibold">
                <dt className="text-slate-900">Cash to pay</dt>
                <dd className="text-slate-900">{formatPrice(cart.subtotalPayable)}</dd>
              </div>

              {cart.totalPointsUsed > 0 && (
                <div className="flex justify-between text-base font-semibold text-accent-700">
                  <dt>e-Points to spend</dt>
                  <dd>{formatPlain(cart.totalPointsUsed)}</dd>
                </div>
              )}
            </dl>

            {Number(cart.subtotalPayable) === 0 && cart.totalPointsUsed > 0 && (
              <p className="mt-3 rounded-lg bg-accent-500/10 px-3 py-2 text-xs font-medium text-accent-700">
                This order is fully covered by e-Points — there is nothing to pay.
              </p>
            )}

            <p className="mt-2 text-xs text-slate-400">
              No tax is added. The price shown is the price charged.
            </p>

            <Button fullWidth size="lg" className="mt-5" onClick={() => navigate('/checkout')}>
              Checkout <ArrowRight className="h-4 w-4" />
            </Button>

            <Link to="/products"
              className="mt-3 block text-center text-sm text-brand-600 hover:underline">
              Continue shopping
            </Link>
          </Card>
        </div>
      </div>
    </div>
  );
}
