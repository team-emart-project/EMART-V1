<<<<<<< HEAD
import { useState } from 'react';
import { motion } from 'framer-motion';
import { Heart } from 'lucide-react';
=======
import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { Check, Heart, Loader2, ShoppingCart } from 'lucide-react';
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
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
<<<<<<< HEAD
=======
 *
 * ADD TO CART IS A BUTTON, NOT A LINK
 * -----------------------------------
 * It used to render as an underlined blue text link, which read as navigation
 * — the one thing it does not do. It is now a real full-width button with the
 * three states an async action needs: idle, in-flight (spinner, disabled so it
 * cannot be double-fired) and added (a tick, for ~1.6s). Without the in-flight
 * state an impatient shopper clicks three times and gets three units.
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
 */
export default function ProductCard({ product, index = 0 }) {
  const dispatch = useDispatch();
  const { isAuthenticated } = useAuth();
  const wishlisted = useSelector(selectIsWishlisted(product.prodId));

  // 'REGULAR' until the shopper ticks one of the eMcard boxes.
  const [priceOption, setPriceOption] = useState('REGULAR');
<<<<<<< HEAD
=======
  const [adding, setAdding] = useState(false);
  const [added, setAdded] = useState(false);

  // A timer that outlives the component would setState on an unmounted card —
  // the classic React memory-leak warning. Cleared on unmount.
  const addedTimer = useRef(null);
  useEffect(() => () => clearTimeout(addedTimer.current), []);
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb

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
<<<<<<< HEAD
    requireLogin(async () => {
=======
    if (adding) return;
    requireLogin(async () => {
      setAdding(true);
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
      // Only the CHOICE is sent. The server looks the actual price up again,
      // so a tampered body cannot invent its own number.
      const result = await dispatch(
        addCartItem({ prodId: product.prodId, quantity: 1, priceOption })
      );
<<<<<<< HEAD
      dispatch(
        addCartItem.fulfilled.match(result)
          ? toastSuccess(`${product.prodName} added to cart`)
          : toastError(result.payload || 'Could not add to cart')
      );
=======
      setAdding(false);

      if (addCartItem.fulfilled.match(result)) {
        dispatch(toastSuccess(`${product.prodName} added to cart`));
        setAdded(true);
        addedTimer.current = setTimeout(() => setAdded(false), 1600);
      } else {
        dispatch(toastError(result.payload || 'Could not add to cart'));
      }
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
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
<<<<<<< HEAD
=======
  const discount = Math.round(Number(product.discountPercentage) || 0);
  const saving = product.cardholderPrice != null
    ? Math.max(0, Number(product.mrpPrice) - Number(product.cardholderPrice))
    : 0;
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb

  // What this card will actually charge if "Add to Cart" is pressed right now.
  // Shown back to the shopper so ticking a box has a visible consequence —
  // without it, the only feedback is a number that appears in the cart later.
  const chosen = buildPriceOptions(product).find((o) => o.value === priceOption);

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, delay: Math.min(index * 0.05, 0.4) }}
<<<<<<< HEAD
      className="group flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-all duration-300 hover:-translate-y-1 hover:border-brand-200 hover:shadow-xl"
    >
      <Link to={`/products/${product.prodId}`} className="block">
        <div className="relative aspect-square overflow-hidden bg-slate-50">
=======
      className="group relative flex h-full flex-col overflow-hidden rounded-2xl border border-slate-200 bg-white transition-all duration-300 hover:-translate-y-1.5 hover:border-brand-300 hover:shadow-[0_24px_48px_-24px_rgba(5,150,105,0.45)]"
    >
      <Link to={`/products/${product.prodId}`} className="block">
        <div className="relative aspect-square overflow-hidden bg-gradient-to-br from-brand-50 via-white to-slate-100">
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
          <img
            src={productImage(product.prodImagePath, product.prodName)}
            alt={product.prodName}
            loading="lazy"
<<<<<<< HEAD
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }}
          />

          <button
            onClick={handleWishlist}
            aria-label="Add to wishlist"
            className="absolute right-3 top-3 rounded-full bg-white/90 p-2 opacity-0 shadow-sm backdrop-blur transition-all duration-300 group-hover:opacity-100 hover:scale-110"
          >
            <Heart className={`h-4 w-4 ${wishlisted ? 'fill-rose-500 text-rose-500' : 'text-slate-600'}`} />
=======
            className={`h-full w-full object-cover transition-transform duration-[600ms] ease-out group-hover:scale-[1.07] ${
              outOfStock ? 'opacity-60 grayscale' : ''}`}
            onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }}
          />

          {/* A whisper of a scrim at the bottom so a white product photo does
              not bleed into the white card body. */}
          <div className="pointer-events-none absolute inset-x-0 bottom-0 h-14 bg-gradient-to-t from-slate-900/10 to-transparent" aria-hidden />

          {discount > 0 && (
            <span className="absolute left-3 top-3 rounded-full bg-red-600 px-2.5 py-1 text-[11px] font-bold tracking-wide text-white shadow-sm">
              {discount}% OFF
            </span>
          )}

          {outOfStock && (
            <span className="absolute inset-x-0 bottom-0 bg-slate-900/80 py-1.5 text-center text-[11px] font-semibold uppercase tracking-[0.14em] text-white">
              Out of stock
            </span>
          )}

          <button
            onClick={handleWishlist}
            aria-label="Add to wishlist"
            className="absolute right-3 top-3 rounded-full bg-white/95 p-2 shadow-sm backdrop-blur transition-all duration-300 hover:scale-110 md:opacity-0 md:group-hover:opacity-100"
          >
            <Heart className={`h-4 w-4 transition-colors ${
              wishlisted ? 'fill-rose-500 text-rose-500' : 'text-slate-600'}`} />
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
          </button>
        </div>
      </Link>

      <div className="flex flex-1 flex-col gap-2 p-3">
        {/* The bordered name box from the reference. min-h so a one-line name
            and a two-line name still leave every card the same height. */}
        <Link
          to={`/products/${product.prodId}`}
<<<<<<< HEAD
          className="block rounded-md border border-slate-800 px-2 py-1.5"
=======
          className="block rounded-md border border-slate-800 px-2 py-1.5 transition-colors group-hover:border-brand-500"
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
        >
          <h3 className="line-clamp-2 min-h-[2.25rem] text-sm font-bold leading-tight text-slate-900 group-hover:text-brand-700">
            {product.prodName}
          </h3>
        </Link>

<<<<<<< HEAD
        <div className="flex min-h-[1.25rem] items-center gap-2">
          <RatingBadge rating={product.rating} count={product.ratingCount} />
          {outOfStock && (
            <span className="text-[11px] font-semibold text-rose-600">Out of stock</span>
=======
        {/* The saving is DERIVED from two real prices the API already sent.
            No invented "was" figure: a struck-through number reverse-engineered
            out of a discount percentage is a made-up price, and made-up prices
            on a storefront are a legal problem, not a design flourish. */}
        <div className="flex min-h-[1.25rem] items-center gap-2">
          <RatingBadge rating={product.rating} count={product.ratingCount} />
          {saving > 0 && (
            <span className="rounded bg-brand-50 px-1.5 py-0.5 text-[11px] font-semibold text-brand-700">
              Save {formatPrice(saving)} with eMcard
            </span>
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
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
<<<<<<< HEAD
          <div className="rounded-md bg-emerald-50 px-2 py-1.5 text-xs text-emerald-800">
=======
          <div className="rounded-md border border-accent-500/25 bg-accent-500/10 px-2 py-1.5 text-xs text-accent-700">
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
            You pay{' '}
            <strong>
              {chosen.cash > 0 ? formatPrice(chosen.cash) : null}
              {chosen.cash > 0 && chosen.points > 0 ? ' + ' : null}
              {chosen.points > 0 ? `${formatPlain(chosen.points)} e-Points` : null}
            </strong>
          </div>
        )}

<<<<<<< HEAD
        {/* "Add to Cart" as an underlined link at the bottom, per the reference. */}
        <button
          type="button"
          onClick={handleAddToCart}
          disabled={outOfStock}
          className="mt-auto self-start text-sm font-semibold text-brand-600 underline underline-offset-2 transition hover:text-brand-800 disabled:cursor-not-allowed disabled:text-slate-400 disabled:no-underline"
        >
          {outOfStock ? 'Out of stock' : 'Add to Cart'}
=======
        {/* ---------- the real button ---------- */}
        <button
          type="button"
          onClick={handleAddToCart}
          disabled={outOfStock || adding}
          aria-live="polite"
          className={`mt-auto flex w-full items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold shadow-sm transition-all duration-200 focus-visible:outline-none focus-visible:ring-4 focus-visible:ring-brand-600/25 active:scale-[0.98] disabled:cursor-not-allowed disabled:active:scale-100 ${
            // The palette is one hue, so the confirmation cannot be "another
            // colour" — it INVERTS instead. Dark-on-light vs light-on-dark is
            // the one contrast a monochrome scheme always has available.
            added
              ? 'border border-brand-600 bg-white text-brand-700'
              : 'bg-brand-gradient text-white hover:shadow-lg hover:shadow-brand-600/30 hover:brightness-125 disabled:bg-slate-200 disabled:bg-none disabled:text-slate-400 disabled:shadow-none'
          }`}
        >
          <AnimatePresence mode="wait" initial={false}>
            {outOfStock ? (
              <motion.span key="oos" className="flex items-center gap-2">
                Out of stock
              </motion.span>
            ) : adding ? (
              <motion.span key="adding"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin" />
                Adding…
              </motion.span>
            ) : added ? (
              <motion.span key="added"
                initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0 }}
                className="flex items-center gap-2">
                <Check className="h-4 w-4" />
                Added to cart
              </motion.span>
            ) : (
              <motion.span key="idle"
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                className="flex items-center gap-2">
                <ShoppingCart className="h-4 w-4" />
                Add to Cart
              </motion.span>
            )}
          </AnimatePresence>
>>>>>>> d5373e2ef28bd43e67b12b3e8d1dcff71723abeb
        </button>
      </div>
    </motion.div>
  );
}
