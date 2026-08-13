import { motion } from 'framer-motion';
import { Link } from 'react-router-dom';
import { productImage, placeholderImage } from '@/utils/formatters';

/**
 * The round category strip Myntra puts directly under its banner.
 *
 * Horizontally scrollable on mobile, evenly spread on desktop.
 */
export default function CategoryCircles({ categories = [], loading }) {
  const items = categories.slice(0, 12);

  if (loading) {
    return (
      <div className="mx-auto flex max-w-7xl gap-6 overflow-hidden px-4 py-8 sm:px-6 lg:px-8">
        {Array.from({ length: 10 }).map((_, i) => (
          <div key={i} className="flex shrink-0 flex-col items-center gap-2">
            <div className="shimmer h-20 w-20 rounded-full" />
            <div className="shimmer h-3 w-14 rounded" />
          </div>
        ))}
      </div>
    );
  }

  if (!items.length) return null;

  return (
    <section className="border-b border-slate-100 bg-white">
      <div className="no-scrollbar mx-auto flex max-w-7xl gap-5 overflow-x-auto px-4 py-7 sm:gap-7 sm:px-6 lg:justify-between lg:px-8">
        {items.map((cat, i) => (
          <motion.div key={cat.catmasterId}
            initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35, delay: Math.min(i * 0.04, 0.35) }}
          >
            <Link to={`/categories/${cat.catmasterId}`}
              className="group flex w-[76px] shrink-0 flex-col items-center gap-2 sm:w-[88px]">
              <span className="relative flex h-[68px] w-[68px] items-center justify-center overflow-hidden rounded-full bg-brand-50 ring-1 ring-brand-100 transition-all duration-300 group-hover:-translate-y-1 group-hover:shadow-lg group-hover:shadow-brand-600/20 group-hover:ring-2 group-hover:ring-brand-500 sm:h-20 sm:w-20">
                <img src={productImage(cat.catImagePath, cat.catName)} alt=""
                  loading="lazy"
                  className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-110"
                  onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(cat.catName); }} />
              </span>
              <span className="line-clamp-2 text-center text-[11px] font-semibold uppercase leading-tight tracking-wide text-slate-700 group-hover:text-brand-600 sm:text-xs">
                {cat.catName}
              </span>
            </Link>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
