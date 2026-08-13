import { useEffect } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Heart, ShoppingCart, Trash2 } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { Badge, Button, Card, EmptyState, ListSkeleton } from '@/components/ui';
import { fetchWishlist, removeFromWishlist, selectWishlist } from '@/store/slices/wishlistSlice';
import { addCartItem } from '@/store/slices/cartSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import { formatPrice, productImage, placeholderImage } from '@/utils/formatters';
import useAuth from '@/hooks/useAuth';

export default function WishlistPage() {
  const dispatch = useDispatch();
  const items = useSelector(selectWishlist);
  const status = useSelector((s) => s.wishlist.status);
  const { isCardholder } = useAuth();

  useEffect(() => { dispatch(fetchWishlist()); }, [dispatch]);

  const moveToCart = async (item) => {
    const result = await dispatch(addCartItem({ prodId: item.prodId, quantity: 1 }));
    if (addCartItem.fulfilled.match(result)) {
      dispatch(removeFromWishlist(item.wishlistId));
      dispatch(toastSuccess('Moved to cart'));
    } else {
      dispatch(toastError(result.payload || 'Could not add to cart'));
    }
  };

  if (status === 'loading' && !items.length) {
    return <div className="mx-auto max-w-5xl px-4 py-10"><ListSkeleton rows={3} /></div>;
  }

  if (!items.length) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-20">
        <EmptyState icon={Heart} title="Your wishlist is empty"
          message="Tap the heart on any product to save it for later."
          action={<Button as={Link} to="/products">Browse products</Button>} />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-1 text-3xl font-bold text-slate-900">Wishlist</h1>
      <p className="mb-8 text-sm text-slate-500">{items.length} saved item{items.length === 1 ? '' : 's'}</p>

      <div className="space-y-3">
        <AnimatePresence mode="popLayout">
          {items.map((item) => (
            <motion.div key={item.wishlistId} layout
              initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, x: -30 }}>
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
                  {item.prodShortDesc && (
                    <p className="mt-0.5 line-clamp-1 text-xs text-slate-500">{item.prodShortDesc}</p>
                  )}

                  <div className="mt-2 flex flex-wrap items-center gap-2">
                    <span className="font-semibold text-slate-900">
                      {formatPrice(isCardholder ? item.cardholderPrice : item.mrpPrice)}
                    </span>
                    {item.pointsPrice > 0 && (
                      <Badge tone="accent">{item.pointsPrice} e-Points</Badge>
                    )}
                  </div>

                  <div className="mt-3 flex gap-2">
                    <Button size="sm" onClick={() => moveToCart(item)}>
                      <ShoppingCart className="h-3.5 w-3.5" /> Move to cart
                    </Button>
                    <Button size="sm" variant="ghost"
                      onClick={() => dispatch(removeFromWishlist(item.wishlistId))}>
                      <Trash2 className="h-3.5 w-3.5" /> Remove
                    </Button>
                  </div>
                </div>
              </Card>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </div>
  );
}
