import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronLeft, ChevronRight, Expand, X } from 'lucide-react';
import { productImage, placeholderImage } from '@/utils/formatters';

/**
 * Myntra-style product gallery.
 *
 *   - vertical thumbnail rail on desktop, horizontal strip on mobile
 *   - main image changes on thumbnail HOVER (preview) and stays on CLICK
 *   - sliding transition that respects direction (next slides left, prev right)
 *   - drag / swipe on touch, arrow keys on desktop
 *   - dot indicators on small screens where the rail is hidden
 *   - click the main image for a full-screen lightbox
 *
 * Hover-preview needs care: if you only track `activeIndex`, moving the mouse
 * away leaves the wrong image showing. So `pinnedIndex` is what the user
 * actually chose and `hoverIndex` is a temporary override that clears on
 * mouse-leave.
 */
export default function ProductGallery({ images = [], fallback, productName = 'Product' }) {

  // Normalise: use the gallery if present, otherwise the single listing thumbnail.
  const slides = useMemo(() => {
    if (images?.length) {
      return images
        .slice()
        .sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0))
        .map((img, i) => ({
          key: img.prodImageId ?? i,
          url: img.imageUrl,
          alt: img.altText || `${productName} — view ${i + 1}`,
        }));
    }
    return [{ key: 'fallback', url: productImage(fallback, productName), alt: productName }];
  }, [images, fallback, productName]);

  const [pinnedIndex, setPinnedIndex] = useState(0);
  const [hoverIndex, setHoverIndex] = useState(null);
  const [direction, setDirection] = useState(0);
  const [lightbox, setLightbox] = useState(false);
  const railRef = useRef(null);

  const activeIndex = hoverIndex ?? pinnedIndex;
  const active = slides[activeIndex] ?? slides[0];

  // A different product mounted into the same component — reset.
  useEffect(() => { setPinnedIndex(0); setHoverIndex(null); }, [slides.length, productName]);

  const goTo = useCallback((next) => {
    const total = slides.length;
    const wrapped = (next + total) % total;
    setDirection(wrapped > pinnedIndex ? 1 : -1);
    setPinnedIndex(wrapped);
    setHoverIndex(null);
  }, [pinnedIndex, slides.length]);

  // Keep the active thumbnail scrolled into view on the mobile strip.
  useEffect(() => {
    const rail = railRef.current;
    const thumb = rail?.querySelector(`[data-index="${pinnedIndex}"]`);
    thumb?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' });
  }, [pinnedIndex]);

  // Arrow keys, and Escape to close the lightbox.
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') setLightbox(false);
      if (slides.length < 2) return;
      if (e.key === 'ArrowRight') goTo(pinnedIndex + 1);
      if (e.key === 'ArrowLeft') goTo(pinnedIndex - 1);
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [goTo, pinnedIndex, slides.length]);

  const multiple = slides.length > 1;

  // Slide in from whichever side matches the direction of travel.
  const variants = {
    enter: (dir) => ({ opacity: 0, x: dir > 0 ? 48 : -48 }),
    center: { opacity: 1, x: 0 },
    exit: (dir) => ({ opacity: 0, x: dir > 0 ? -48 : 48 }),
  };

  return (
    <>
      <div className="flex flex-col-reverse gap-3 lg:flex-row lg:gap-4">

        {/* ---------------- thumbnail rail ---------------- */}
        {multiple && (
          <div
            ref={railRef}
            className="no-scrollbar flex shrink-0 gap-2.5 overflow-x-auto lg:max-h-[520px] lg:flex-col lg:overflow-y-auto lg:overflow-x-hidden"
          >
            {slides.map((s, i) => (
              <button
                key={s.key}
                data-index={i}
                onMouseEnter={() => setHoverIndex(i)}
                onMouseLeave={() => setHoverIndex(null)}
                onFocus={() => setHoverIndex(i)}
                onBlur={() => setHoverIndex(null)}
                onClick={() => goTo(i)}
                aria-label={`View image ${i + 1} of ${slides.length}`}
                aria-current={i === pinnedIndex}
                className={`relative h-16 w-16 shrink-0 overflow-hidden rounded-xl border-2 transition-all duration-200 sm:h-[72px] sm:w-[72px] ${
                  i === activeIndex
                    ? 'border-brand-500 ring-2 ring-brand-500/20'
                    : 'border-slate-200 opacity-70 hover:border-slate-300 hover:opacity-100'
                }`}
              >
                <img src={s.url} alt="" loading="lazy"
                  className="h-full w-full object-cover"  onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }} />
              </button>
            ))}
          </div>
        )}

        {/* ---------------- main stage ---------------- */}
        <div className="relative min-w-0 flex-1">
          <div className="group relative aspect-square w-full overflow-hidden rounded-2xl border border-slate-200 bg-slate-50">
            <AnimatePresence initial={false} custom={direction} mode="popLayout">
              <motion.img
                key={active.key}
                src={active.url}
                alt={active.alt}
                custom={direction}
                variants={variants}
                initial="enter" animate="center" exit="exit"
                transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
                drag={multiple ? 'x' : false}
                dragConstraints={{ left: 0, right: 0 }}
                dragElastic={0.15}
                onDragEnd={(_, info) => {
                  // 60px of travel, or a fast flick, counts as a swipe.
                  if (info.offset.x < -60 || info.velocity.x < -450) goTo(pinnedIndex + 1);
                  else if (info.offset.x > 60 || info.velocity.x > 450) goTo(pinnedIndex - 1);
                }}
                className="absolute inset-0 h-full w-full cursor-zoom-in object-cover"
                onClick={() => setLightbox(true)}
                draggable={false}
              />
            </AnimatePresence>

            {/* expand hint */}
            <button onClick={() => setLightbox(true)} aria-label="Open full screen"
              className="absolute right-3 top-3 rounded-full bg-white/85 p-2 text-slate-600 opacity-0 shadow-sm backdrop-blur transition-opacity duration-200 group-hover:opacity-100">
              <Expand className="h-4 w-4" />
            </button>

            {multiple && (
              <>
                <button onClick={() => goTo(pinnedIndex - 1)} aria-label="Previous image"
                  className="absolute left-3 top-1/2 -translate-y-1/2 rounded-full bg-white/85 p-2 text-slate-700 opacity-0 shadow-md backdrop-blur transition-all duration-200 hover:bg-white group-hover:opacity-100">
                  <ChevronLeft className="h-4 w-4" />
                </button>
                <button onClick={() => goTo(pinnedIndex + 1)} aria-label="Next image"
                  className="absolute right-3 top-1/2 -translate-y-1/2 rounded-full bg-white/85 p-2 text-slate-700 opacity-0 shadow-md backdrop-blur transition-all duration-200 hover:bg-white group-hover:opacity-100">
                  <ChevronRight className="h-4 w-4" />
                </button>

                <span className="absolute bottom-3 right-3 rounded-full bg-slate-900/60 px-2.5 py-1 text-xs font-medium text-white backdrop-blur">
                  {activeIndex + 1} / {slides.length}
                </span>
              </>
            )}
          </div>

          {/* dots — the rail is a scroll strip on mobile, so these give position at a glance */}
          {multiple && (
            <div className="mt-3 flex justify-center gap-1.5 lg:hidden">
              {slides.map((s, i) => (
                <button key={s.key} onClick={() => goTo(i)}
                  aria-label={`Go to image ${i + 1}`}
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    i === activeIndex ? 'w-5 bg-brand-600' : 'w-1.5 bg-slate-300'
                  }`} />
              ))}
            </div>
          )}
        </div>
      </div>

      {/* ---------------- lightbox ---------------- */}
      <AnimatePresence>
        {lightbox && (
          <motion.div
            initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}
            className="fixed inset-0 z-[60] flex items-center justify-center bg-slate-900/90 p-4 backdrop-blur-sm"
            onClick={() => setLightbox(false)}
            role="dialog" aria-modal="true" aria-label="Product image viewer"
          >
            <button onClick={() => setLightbox(false)} aria-label="Close"
              className="absolute right-4 top-4 rounded-full bg-white/10 p-2.5 text-white transition-colors hover:bg-white/20">
              <X className="h-5 w-5" />
            </button>

            <motion.img
              key={active.key}
              src={active.url} alt={active.alt}
              initial={{ scale: 0.94, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.96, opacity: 0 }}
              transition={{ duration: 0.22 }}
              onClick={(e) => e.stopPropagation()}
              className="max-h-[86vh] max-w-[92vw] rounded-2xl object-contain shadow-2xl"
            />

            {multiple && (
              <>
                <button
                  onClick={(e) => { e.stopPropagation(); goTo(pinnedIndex - 1); }}
                  aria-label="Previous image"
                  className="absolute left-4 rounded-full bg-white/10 p-3 text-white transition-colors hover:bg-white/20">
                  <ChevronLeft className="h-5 w-5" />
                </button>
                <button
                  onClick={(e) => { e.stopPropagation(); goTo(pinnedIndex + 1); }}
                  aria-label="Next image"
                  className="absolute right-4 rounded-full bg-white/10 p-3 text-white transition-colors hover:bg-white/20">
                  <ChevronRight className="h-5 w-5" />
                </button>
              </>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
}
