import { useRef } from 'react';
import { motion } from 'framer-motion';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import ProductCard from '@/components/common/ProductCard';
import { ProductCardSkeleton } from '@/components/ui';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';

/**
 * Horizontally scrollable product rail, one per category.
 *
 * Uses the SAME useFetch hook as everything else — the only thing that varies
 * is the URL, which is the whole point of having one hook rather than a
 * getAll/getById pair.
 *
 * Scrolling is native overflow-x with CSS snap points; the arrows just nudge
 * scrollLeft. That keeps touch/trackpad behaviour correct for free.
 */
export default function ProductCarousel({ categoryId, title, subtitle, size = 8 }) {
  const railRef = useRef(null);

  const url = categoryId
    ? endpoints.categories.products(
        categoryId,
        new URLSearchParams({ includeSubCategories: 'true', page: 0, size }).toString())
    : endpoints.products.search(new URLSearchParams({ page: 0, size }).toString());

  const { data, loading } = useFetch(url);
  const products = data?.content || [];

  const nudge = (dir) => {
    const rail = railRef.current;
    if (!rail) return;
    rail.scrollBy({ left: dir * (rail.clientWidth * 0.8), behavior: 'smooth' });
  };

  // Nothing in this category — render nothing rather than an empty rail.
  if (!loading && products.length === 0) return null;

  return (
    <section className="py-12">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <motion.div
          initial={{ opacity: 0, y: 14 }} whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="mb-6 flex items-end justify-between gap-4"
        >
          <div>
            <h2 className="text-2xl font-extrabold sm:text-3xl">{title}</h2>
            {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          </div>

          <div className="flex items-center gap-2">
            {categoryId && (
              <Link to={`/categories/${categoryId}`}
                className="hidden text-sm font-medium text-brand-600 hover:underline sm:block">
                View all
              </Link>
            )}
            <div className="hidden gap-1 sm:flex">
              <button onClick={() => nudge(-1)} aria-label="Scroll left"
                className="rounded-full border border-slate-200 p-2 text-slate-500 transition-colors hover:border-slate-300 hover:text-brand-600">
                <ChevronLeft className="h-4 w-4" />
              </button>
              <button onClick={() => nudge(1)} aria-label="Scroll right"
                className="rounded-full border border-slate-200 p-2 text-slate-500 transition-colors hover:border-slate-300 hover:text-brand-600">
                <ChevronRight className="h-4 w-4" />
              </button>
            </div>
          </div>
        </motion.div>

        <div ref={railRef}
          className="no-scrollbar -mx-4 flex snap-x snap-mandatory gap-4 overflow-x-auto scroll-smooth px-4 pb-2 sm:mx-0 sm:px-0">
          {loading
            ? Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="w-[240px] shrink-0 sm:w-[260px]">
                  <ProductCardSkeleton />
                </div>
              ))
            : products.map((p, i) => (
                <div key={p.prodId} className="w-[240px] shrink-0 snap-start sm:w-[260px]">
                  <ProductCard product={p} index={i} />
                </div>
              ))}
        </div>
      </div>
    </section>
  );
}
