import { useCallback, useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

/**
 * Myntra/Flipkart-style promotional banner slider.
 *
 *   - auto-advances every 5s
 *   - pauses on hover and while the tab is hidden (an unseen carousel
 *     burning CPU is pure waste)
 *   - swipe on touch, arrows on desktop, dots for position
 *
 * Banners are local SVGs in public/images/banners, so there is no CMS to wire
 * up and nothing to fetch. Add a slide by dropping a file in and adding a row
 * to SLIDES.
 */
const SLIDES = [
  { img: '/images/banners/mega-sale.svg',   to: '/products',                 alt: 'Mega savings days — up to 70% off' },
  { img: '/images/banners/electronics.svg', to: '/products?search=phone',    alt: 'Latest smartphones and laptops' },
  { img: '/images/banners/fashion.svg',     to: '/products?search=shirt',    alt: 'Fashion fest — min 40% off' },
  { img: '/images/banners/home.svg',        to: '/products?search=kitchen',  alt: 'Home and kitchen upgrades' },
  { img: '/images/banners/grocery.svg',     to: '/products?search=grocery',  alt: 'Grocery super saver' },
];

const INTERVAL = 5000;

export default function BannerCarousel() {
  const [index, setIndex] = useState(0);
  const [dir, setDir] = useState(1);
  const [paused, setPaused] = useState(false);
  const navigate = useNavigate();
  const timer = useRef(null);

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

        <button onClick={() => go(index - 1)} aria-label="Previous banner"
          className="absolute left-4 top-1/2 -translate-y-1/2 rounded-full bg-white/90 p-2.5 text-slate-700 opacity-0 shadow-lg transition-opacity group-hover:opacity-100">
          <ChevronLeft className="h-5 w-5" />
        </button>
        <button onClick={() => go(index + 1)} aria-label="Next banner"
          className="absolute right-4 top-1/2 -translate-y-1/2 rounded-full bg-white/90 p-2.5 text-slate-700 opacity-0 shadow-lg transition-opacity group-hover:opacity-100">
          <ChevronRight className="h-5 w-5" />
        </button>

        <div className="absolute bottom-4 left-1/2 flex -translate-x-1/2 gap-2">
          {SLIDES.map((s, i) => (
            <button key={s.img} onClick={() => go(i)} aria-label={`Go to slide ${i + 1}`}
              className={`h-1.5 rounded-full transition-all duration-300 ${
                i === index ? 'w-7 bg-white' : 'w-1.5 bg-white/55 hover:bg-white/80'}`} />
          ))}
        </div>
      </div>
    </section>
  );
}
