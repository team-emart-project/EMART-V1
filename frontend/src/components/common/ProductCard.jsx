import { useState } from 'react';
import { motion } from 'framer-motion';
import { Heart } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import PriceOptions, { buildPriceOptions } from './PriceOptions';
import RatingBadge from './RatingBadge';
import { addCartItem } from '@/store/slices/cartSlice';
import { addToWishlist, selectIsWishlisted } from '@/store/slices/wishlistSlice';
import { toastError, toastSuccess } from '@/store/slices/uiSlice';
import { productImage, placeholderImage, formatPlain, formatPrice } from '@/utils/formatters';
import useAuth from '@/hooks/useAuth';

/**
 * The single product tile used on the home page, search results and category
 * pages. Written once, reused in four places.
 *
 * LAYOUT follows the reference wireframe, top to bottom:
 *   image -> bordered name box -> normal price -> eMcard checkboxes -> Add to Cart
 *
 * NOTE ON THE LINK
 * ----------------
 * Only the image and the name box are inside the <Link>. The price block sits
 * OUTSIDE it, because a checkbox nested in a link navigates away instead of
 * ticking. The old version wrapped the whole card, which would have made the
 * new checkboxes unusable.
 */
export default function ProductCard({ product, index = 0 }) {
  const dispatch = useDispatch();
  const { isAuthenticated } = useAuth();
  const wishlisted = useSelector(selectIsWishlisted(product.prodId));

  // 'REGULAR' until the shopper ticks one of the eMcard boxes.
  const [priceOption, setPriceOption] = useState('REGULAR');

  const requireLogin = (action) => {
    if (!isAuthenticated) {
      dispatch(toastError('Please log in first'));
      return false;
    }
    action();
    return true;
  };

  const handleAddToCart = (e) => {
    e.preventDefault();
    e.stopPropagation();
    requireLogin(async () => {
      // Only the CHOICE is sent. The server looks the actual price up again,
      // so a tampered body cannot invent its own number.
      const result = await dispatch(
        addCartItem({ prodId: product.prodId, quantity: 1, priceOption })
      );
      dispatch(
        addCartItem.fulfilled.match(result)
          ? toastSuccess(`${product.prodName} added to cart`)
          : toastError(result.payload || 'Could not add to cart')
      );
    });
  };

  const handleWishlist = (e) => {
    e.preventDefault();
    e.stopPropagation();
    requireLogin(async () => {
      const result = await dispatch(addToWishlist(product.prodId));
      dispatch(
        addToWishlist.fulfilled.match(result)
          ? toastSuccess('Saved to wishlist')
          : toastError(result.payload || 'Already in your wishlist')
      );
    });
  };

  const outOfStock = product.inStock === false;

  // What this card will actually charge if "Add to Cart" is pressed right now.
  // Shown back to the shopper so ticking a box has a visible consequence —
  // without it, the only feedback is a number that appears in the cart later.
  const chosen = buildPriceOptions(product).find((o) => o.value === priceOption);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: Math.min(index * 0.05, 0.4) }}
      className="group flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-all duration-300 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl"
    >
      <Link to={`/products/${product.prodId}`} className="block">
        <div className="relative aspect-square overflow-hidden bg-slate-50">
          <img
            src={productImage(product.prodImagePath, product.prodName)}
            alt={product.prodName}
            loading="lazy"
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }}
          />

          <button
            onClick={handleWishlist}
            aria-label="Add to wishlist"
            className="absolute right-3 top-3 rounded-full bg-white/90 p-2 opacity-0 shadow-sm backdrop-blur transition-all duration-300 group-hover:opacity-100 hover:scale-110"
          >
            <Heart className={`h-4 w-4 ${wishlisted ? 'fill-rose-500 text-rose-500' : 'text-slate-600'}`} />
          </button>
        </div>
      </Link>

      <div className="flex flex-1 flex-col gap-2 p-3">
        {/* The bordered name box from the reference. min-h so a one-line name
            and a two-line name still leave every card the same height. */}
        <Link
          to={`/products/${product.prodId}`}
          className="block rounded-md border border-slate-800 px-2 py-1.5"
        >
          <h3 className="line-clamp-2 min-h-[2.25rem] text-sm font-bold leading-tight text-slate-900 group-hover:text-brand-700">
            {product.prodName}
          </h3>
        </Link>

        <div className="flex min-h-[1.25rem] items-center gap-2">
          <RatingBadge rating={product.rating} count={product.ratingCount} />
          {outOfStock && (
            <span className="text-[11px] font-semibold text-rose-600">Out of stock</span>
          )}
        </div>

        {/* min-h reserves room for up to three option rows, so a product with
            every offer and one with none still end at the same height. */}
        <div className="min-h-[5.5rem]">
          <PriceOptions
            product={product}
            value={priceOption}
            onChange={setPriceOption}
            compact
          />
        </div>

        {chosen && (
          <div className="rounded-md bg-emerald-50 px-2 py-1.5 text-xs text-emerald-800">
            You pay{' '}
            <strong>
              {chosen.cash > 0 ? formatPrice(chosen.cash) : null}
              {chosen.cash > 0 && chosen.points > 0 ? ' + ' : null}
              {chosen.points > 0 ? `${formatPlain(chosen.points)} e-Points` : null}
            </strong>
          </div>
        )}

        {/* "Add to Cart" as an underlined link at the bottom, per the reference. */}
        <button
          type="button"
          onClick={handleAddToCart}
          disabled={outOfStock}
          className="mt-auto self-start text-sm font-semibold text-brand-600 underline underline-offset-2 transition hover:text-brand-800 disabled:cursor-not-allowed disabled:text-slate-400 disabled:no-underline"
        >
          {outOfStock ? 'Out of stock' : 'Add to Cart'}
        </button>
      </div>
    </motion.div>
  );
}
