import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { productImage, placeholderImage, formatPrice } from '@/utils/formatters';

/**
 * Promotional banner slider with REAL products on the slide.
 *
 *   - auto-advances every 5s
 *   - pauses on hover and while the tab is hidden (an unseen carousel
 *     burning CPU is pure waste)
 *   - swipe on touch, arrows on desktop, dots for position
 *   - each slide carries three promotional product cards pulled from the API
 *
 * WHY THE PRODUCTS ARE HERE
 * -------------------------
 * A banner that only says "up to 70% off" gives a shopper nothing to click.
 * Overlaying the actual discounted products turns the slide from an
 * advertisement into a shortcut: the price is real, the link goes to the
 * product page, and the numbers change as the catalogue changes.
 *
 * The whole carousel makes ONE request (new-arrivals) and deals the results
 * out across the slides, biggest discount first — five separate per-slide
 * searches would be five round-trips for the same pixels.
 */
const SLIDES = [
  { img: '/images/banners/mega-sale.svg',   to: '/products',                 alt: 'Mega savings days — up to 70% off' },
  { img: '/images/banners/electronics.svg', to: '/products?search=phone',    alt: 'Latest smartphones and laptops' },
  { img: '/images/banners/fashion.svg',     to: '/products?search=shirt',    alt: 'Fashion fest — min 40% off' },
  { img: '/images/banners/home.svg',        to: '/products?search=kitchen',  alt: 'Home and kitchen upgrades' },
  { img: '/images/banners/grocery.svg',     to: '/products?search=grocery',  alt: 'Grocery super saver' },
];

const INTERVAL = 5000;
const PER_SLIDE = 3;

/** One promotional product card sitting on top of the banner artwork. */
function PromoProductCard({ product, delay = 0 }) {
  const discount = Math.round(Number(product.discountPercentage) || 0);
  const price = product.cardholderPrice ?? product.mrpPrice;

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.35, delay, ease: [0.16, 1, 0.3, 1] }}
      className="pointer-events-auto"
    >
      <Link
        to={`/products/${product.prodId}`}
        onClick={(e) => e.stopPropagation()}
        className="group flex w-[132px] flex-col overflow-hidden rounded-xl bg-white/95 shadow-lg shadow-slate-900/10 backdrop-blur transition-transform duration-300 hover:-translate-y-1 lg:w-[152px]"
      >
        <div className="relative aspect-square overflow-hidden bg-slate-50">
          <img
            src={productImage(product.prodImagePath, product.prodName)}
            alt={product.prodName}
            loading="lazy"
            draggable={false}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(product.prodName); }}
          />
          {discount > 0 && (
            <span className="absolute left-1.5 top-1.5 rounded-md bg-red-600 px-1.5 py-0.5 text-[10px] font-bold text-white">
              {discount}% OFF
            </span>
          )}
        </div>
        <div className="px-2.5 py-2">
          <p className="line-clamp-1 text-[11px] font-medium text-slate-700">{product.prodName}</p>
          <p className="mt-0.5 text-sm font-bold text-slate-900">{formatPrice(price)}</p>
        </div>
      </Link>
    </motion.div>
  );
}

export default function BannerCarousel() {
  const [index, setIndex] = useState(0);
  const [dir, setDir] = useState(1);
  const [paused, setPaused] = useState(false);
  const navigate = useNavigate();
  const timer = useRef(null);

  // One fetch feeds every slide.
  const { data: pool } = useFetch(endpoints.home.newArrivals(SLIDES.length * PER_SLIDE));

  // Discounted products first — a promo card with no saving on it is just a
  // product card in an expensive position.
  const promoBySlide = useMemo(() => {
    const list = [...(pool || [])].sort(
      (a, b) => (Number(b.discountPercentage) || 0) - (Number(a.discountPercentage) || 0)
    );
    return SLIDES.map((_, i) => list.slice(i * PER_SLIDE, i * PER_SLIDE + PER_SLIDE));
  }, [pool]);

  const go = useCallback((next) => {
    setDir(next > index ? 1 : -1);
    setIndex((next + SLIDES.length) % SLIDES.length);
  }, [index]);

  // Auto-advance. Cleared on hover, and while the tab is in the background.
  useEffect(() => {
    if (paused) return undefined;
    timer.current = setInterval(() => {
      setDir(1);
      setIndex((i) => (i + 1) % SLIDES.length);
    }, INTERVAL);
    return () => clearInterval(timer.current);
  }, [paused]);

  useEffect(() => {
    const onVisibility = () => setPaused(document.hidden);
    document.addEventListener('visibilitychange', onVisibility);
    return () => document.removeEventListener('visibilitychange', onVisibility);
  }, []);

  const variants = {
    enter: (d) => ({ opacity: 0, x: d > 0 ? 60 : -60 }),
    center: { opacity: 1, x: 0 },
    exit: (d) => ({ opacity: 0, x: d > 0 ? -60 : 60 }),
  };

  const current = promoBySlide[index] || [];

  return (
    <section
      className="relative bg-slate-100"
      onMouseEnter={() => setPaused(true)}
      onMouseLeave={() => setPaused(false)}
      aria-roledescription="carousel"
      aria-label="Promotions"
    >
      <div className="group relative mx-auto aspect-[1600/560] w-full max-w-[1600px] overflow-hidden">
        <AnimatePresence initial={false} custom={dir} mode="popLayout">
          <motion.img
            key={SLIDES[index].img}
            src={SLIDES[index].img}
            alt={SLIDES[index].alt}
            custom={dir}
            variants={variants}
            initial="enter" animate="center" exit="exit"
            transition={{ duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
            drag="x"
            dragConstraints={{ left: 0, right: 0 }}
            dragElastic={0.12}
            onDragEnd={(_, info) => {
              if (info.offset.x < -70 || info.velocity.x < -450) go(index + 1);
              else if (info.offset.x > 70 || info.velocity.x > 450) go(index - 1);
            }}
            onClick={() => navigate(SLIDES[index].to)}
            className="absolute inset-0 h-full w-full cursor-pointer object-cover"
            draggable={false}
          />
        </AnimatePresence>

        {/* ---------- promotional products, on the right of the artwork ----------
            pointer-events-none on the wrapper so dragging and clicking the
            banner still works everywhere except on a card itself. */}
        <div className="pointer-events-none absolute inset-y-0 right-4 hidden items-center md:flex lg:right-10">
          <AnimatePresence mode="wait">
            {current.length > 0 && (
              <motion.div
                key={index}
                initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
                transition={{ duration: 0.25 }}
                className="flex flex-col items-end gap-2"
              >
                {/* Neutral, not brand-coloured: this chip sits on top of the
                    banner artwork, which keeps its own vivid palette. A teal
                    chip would clash with whichever slide is showing. */}
                <span className="pointer-events-none rounded-full bg-slate-900/80 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.14em] text-white shadow-md">
                  Featured picks
                </span>
                <div className="flex gap-3">
                  {current.map((p, i) => (
                    <PromoProductCard key={p.prodId} product={p} delay={i * 0.07} />
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        <button onClick={() => go(index - 1)} aria-label="Previous banner"
          className="absolute left-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-white/90 p-2.5 text-slate-700 opacity-0 shadow-lg transition-opacity group-hover:opacity-100">
          <ChevronLeft className="h-5 w-5" />
        </button>
        <button onClick={() => go(index + 1)} aria-label="Next banner"
          className="absolute right-4 top-1/2 z-10 -translate-y-1/2 rounded-full bg-white/90 p-2.5 text-slate-700 opacity-0 shadow-lg transition-opacity group-hover:opacity-100">
          <ChevronRight className="h-5 w-5" />
        </button>

        <div className="absolute bottom-4 left-1/2 z-10 flex -translate-x-1/2 gap-2">
          {SLIDES.map((s, i) => (
            <button key={s.img} onClick={() => go(i)} aria-label={`Go to slide ${i + 1}`}
              className={`h-1.5 rounded-full transition-all duration-300 ${
                i === index ? 'w-7 bg-white' : 'w-1.5 bg-white/55 hover:bg-white/80'}`} />
          ))}
        </div>
      </div>

      {/* On phones the cards would cover the headline, so they run as a
          scrollable strip directly under the banner instead. */}
      {current.length > 0 && (
        <div className="no-scrollbar flex gap-3 overflow-x-auto bg-white px-4 py-3 md:hidden">
          {current.map((p, i) => (
            <PromoProductCard key={p.prodId} product={p} delay={i * 0.05} />
          ))}
        </div>
      )}
    </section>
  );
}
