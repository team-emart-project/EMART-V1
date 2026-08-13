import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import BannerCarousel from '@/components/home/BannerCarousel';
import CategoryCircles from '@/components/home/CategoryCircles';
import DealStrip from '@/components/home/DealStrip';
import PromoTiles from '@/components/home/PromoTiles';
import ProductCarousel from '@/components/home/ProductCarousel';
import ProductCard from '@/components/common/ProductCard';
import { ProductCardSkeleton } from '@/components/ui';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';

/**
 * Module 1 — Home page, Myntra/Flipkart style.
 *
 * Order matters here: banner slider first, then the round category strip,
 * then deals, promo tiles and product rails. That is the pattern shoppers
 * expect from an Indian marketplace, and it puts merchandising above the fold
 * instead of a marketing hero.
 *
 * Everything is driven by the real API — the categories, the deals and every
 * rail come from the database.
 */
export default function HomePage() {
  const { data: categoryTree, loading: loadingCats } = useFetch(endpoints.categories.tree);
  const { data: arrivals, loading: loadingArrivals } = useFetch(endpoints.home.newArrivals(12));

  // Rails for the first few TOP-LEVEL categories, so the page reflects
  // whatever is actually in the catalogue.
  const rails = (categoryTree || []).slice(0, 4);

  return (
    <>
      {/* 1 — promotional banner slider */}
      <BannerCarousel />

      {/* 2 — round category strip */}
      <CategoryCircles categories={categoryTree || []} loading={loadingCats} />

      {/* 3 — deals of the day, from the biggest real discounts */}
      <DealStrip products={arrivals || []} loading={loadingArrivals} />

      {/* 4 — promotional tiles */}
      <PromoTiles />

      {/* 5 — new arrivals grid */}
      <section className="bg-brand-wash py-12">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 12 }} whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="mb-7 flex items-end justify-between gap-4"
          >
            <div>
              <h2 className="text-2xl font-extrabold text-slate-900 sm:text-3xl">New arrivals</h2>
              <p className="mt-1 text-sm text-slate-500">Fresh additions to the catalogue.</p>
            </div>
            <Link to="/products"
              className="group hidden items-center gap-1 text-sm font-semibold text-brand-600 hover:underline sm:flex">
              View all
              <ArrowRight className="h-3.5 w-3.5 transition-transform group-hover:translate-x-1" />
            </Link>
          </motion.div>

          <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-4">
            {loadingArrivals
              ? Array.from({ length: 8 }).map((_, i) => <ProductCardSkeleton key={i} />)
              : (arrivals || []).slice(0, 8).map((p, i) => (
                  <ProductCard key={p.prodId} product={p} index={i} />
                ))}
          </div>
        </div>
      </section>

      {/* 6 — one rail per top-level category */}
      {rails.map((cat, i) => (
        <ProductCarousel
          key={cat.catmasterId}
          categoryId={cat.catmasterId}
          title={`Trending in ${cat.catName}`}
          subtitle={i === 0 ? 'What members are buying this week.' : undefined}
        />
      ))}
    </>
  );
}
