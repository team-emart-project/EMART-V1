import { useState } from 'react';
import { motion } from 'framer-motion';
import { ChevronRight, Heart, ShieldCheck, ShoppingCart, Sparkles, Truck } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Badge, Button, EmptyState, Skeleton } from '@/components/ui';
import PriceOptions from '@/components/common/PriceOptions';
import ProductGallery from '@/components/common/ProductGallery';
import RatingBadge from '@/components/common/RatingBadge';
import QuantityStepper from '@/components/common/QuantityStepper';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { addCartItem, selectCartBusy } from '@/store/slices/cartSlice';
import { addToWishlist, selectIsWishlisted } from '@/store/slices/wishlistSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import useAuth from '@/hooks/useAuth';
import useCardBalance from '@/hooks/useCardBalance';

export default function ProductDetailPage() {
  const { prodId } = useParams();
  const dispatch = useDispatch();
  const { isAuthenticated } = useAuth();
  // Live from /api/emart-card/balance, so a card approved in another tab
  // is reflected without needing to log out and back in.
  const { cardholder: isCardholder, refresh: refreshPoints } = useCardBalance();
  const busy = useSelector(selectCartBusy);
  const wishlisted = useSelector(selectIsWishlisted(Number(prodId)));

  const [qty, setQty] = useState(1);
  // 'REGULAR' until the shopper ticks one of the eMcard options.
  const [priceOption, setPriceOption] = useState('REGULAR');

  // Same hook as the list page — only the URL differs.
  const { data: product, loading, error } = useFetch(endpoints.products.byId(prodId));
  const { data: variants } = useFetch(endpoints.products.variants(prodId));

  const guard = () => {
    if (!isAuthenticated) { dispatch(toastError('Please log in to shop')); return false; }
    return true;
  };

  const handleAdd = async () => {
    if (!guard()) return;
    const result = await dispatch(addCartItem({
      prodId: Number(prodId), quantity: qty, priceOption,
    }));

    if (addCartItem.fulfilled.match(result)) {
      dispatch(toastSuccess('Added to cart'));
      // A points purchase moves the balance, so re-read it rather than let the
      // header show a stale number.
      if (priceOption !== 'REGULAR' && priceOption !== 'MEMBER') refreshPoints();
    } else {
      // The backend re-validates the balance and returns the exact reason.
      dispatch(toastError(result.payload || 'Could not add to cart'));
    }
  };

  const handleWishlist = async () => {
    if (!guard()) return;
    const result = await dispatch(addToWishlist(Number(prodId)));
    dispatch(addToWishlist.fulfilled.match(result)
      ? toastSuccess('Saved to wishlist')
      : toastError(result.payload || 'Already in your wishlist'));
  };

  if (loading) {
    return (
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-10 sm:px-6 lg:grid-cols-2 lg:gap-12 lg:px-8">
        <Skeleton className="aspect-square w-full rounded-2xl" />
        <div className="space-y-4">
          <Skeleton className="h-8 w-3/4" /><Skeleton className="h-4 w-1/2" />
          <Skeleton className="h-10 w-1/3" /><Skeleton className="h-24 w-full" />
        </div>
      </div>
    );
  }

  if (error || !product) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20 sm:px-6 lg:px-8">
        <EmptyState title="Product not found"
          message={error?.message || 'This product may have been removed.'}
          action={<Button as={Link} to="/products">Back to shop</Button>} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8 lg:py-12">
      <nav className="mb-6 flex flex-wrap items-center gap-1 text-sm text-slate-500">
        <Link to="/" className="hover:text-brand-600">Home</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to="/products" className="hover:text-brand-600">Shop</Link>
        {product.categoryName && (
          <>
            <ChevronRight className="h-3.5 w-3.5" />
            <Link to={`/categories/${product.catmasterId}`} className="hover:text-brand-600">
              {product.categoryName}
            </Link>
          </>
        )}
      </nav>

      <div className="grid gap-8 lg:grid-cols-2 lg:items-start lg:gap-12">
        {/* Sticky so the gallery stays visible while the long description
            scrolls past on desktop — the behaviour Myntra has. */}
        <div className="lg:sticky lg:top-24">
          <ProductGallery
            images={product.images}
            fallback={product.prodImagePath}
            productName={product.prodName}
          />
        </div>

        <motion.div initial={{ opacity: 0, x: 16 }} animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.4, delay: 0.1 }} className="min-w-0">

          {product.categoryName && (
            <Badge tone="brand" className="mb-3">{product.categoryName}</Badge>
          )}

          {product.brand && (
            <p className="mb-1 text-sm font-semibold uppercase tracking-wide text-brand-600">
              {product.brand}
            </p>
          )}

          <h1 className="text-2xl font-bold leading-tight text-slate-900 sm:text-3xl">
            {product.prodName}
          </h1>

          <div className="mt-2 flex flex-wrap items-center gap-3">
            <RatingBadge rating={product.rating} count={product.ratingCount} size="md" />
            {product.inStock === false
              ? <span className="text-sm font-semibold text-rose-600">Out of stock</span>
              : product.stockQuantity != null && product.stockQuantity <= 10 && (
                  <span className="text-sm font-medium text-amber-600">
                    Only {product.stockQuantity} left
                  </span>
                )}
          </div>
          {product.prodShortDesc && (
            <p className="mt-2 text-slate-600">{product.prodShortDesc}</p>
          )}

          <div className="mt-5">
            <PriceOptions product={product} value={priceOption} onChange={setPriceOption} />
          </div>

          {!isCardholder && (
            <div className="mt-3 rounded-xl bg-brand-50 px-4 py-3 text-sm text-brand-800">
              <Sparkles className="mr-1 inline h-4 w-4" />
              An e-MART card unlocks member pricing and earns you 2%–5% back in e-Points
              on every order.{' '}
              <Link to="/account/card" className="font-medium underline">Apply now</Link>
            </div>
          )}

          {variants?.length > 0 && (
            <div className="mt-6 space-y-4">
              {variants.map((group) => (
                <div key={group.configId}>
                  <p className="mb-2 text-sm font-medium text-slate-700">{group.configName}</p>
                  <div className="flex flex-wrap gap-2">
                    {group.values.map((v) => (
                      <span key={v.prodDtlId}
                        className="rounded-lg border border-slate-300 bg-white px-3 py-1.5 text-sm text-slate-700">
                        {v.value}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          <div className="mt-6 flex items-center gap-4">
            <span className="text-sm font-medium text-slate-700">Quantity</span>
            <QuantityStepper value={qty} onChange={setQty} />
          </div>

          <div className="mt-6 flex flex-wrap gap-3">
            <Button size="lg" className="min-w-[180px] flex-1" loading={busy}
              disabled={product.inStock === false} onClick={handleAdd}>
              <ShoppingCart className="h-4 w-4" />
              {product.inStock === false ? 'Out of stock' : 'Add to cart'}
            </Button>
            <Button size="lg" variant="outline" onClick={handleWishlist} aria-label="Save to wishlist">
              <Heart className={`h-4 w-4 ${wishlisted ? 'fill-rose-500 text-rose-500' : ''}`} />
            </Button>
          </div>

          <div className="mt-6 grid grid-cols-2 gap-3 text-sm">
            <div className="flex items-center gap-2 text-slate-600">
              <Truck className="h-4 w-4 text-brand-500" /> Courier delivery
            </div>
            <div className="flex items-center gap-2 text-slate-600">
              <ShieldCheck className="h-4 w-4 text-brand-500" /> Secure checkout
            </div>
          </div>

          {product.prodLongDesc && (
            <div className="mt-8 border-t border-slate-200 pt-6">
              <h2 className="mb-2 text-lg font-semibold text-slate-900">About this product</h2>
              <p className="whitespace-pre-line text-sm leading-relaxed text-slate-600">
                {product.prodLongDesc}
              </p>
            </div>
          )}
        </motion.div>
      </div>
    </div>
  );
}
