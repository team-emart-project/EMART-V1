import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import { productImage, placeholderImage, formatPrice } from '@/utils/formatters';

/**
 * "Deals of the Day" strip — Myntra/Flipkart put a compact promotional row
 * between the banner and the product rails.
 *
 * Driven by real products (the biggest discounts in the catalogue) rather than
 * hard-coded copy, so it stays true as the data changes.
 */
export default function DealStrip({ products = [], loading }) {
  const deals = [...products]
    .filter((p) => Number(p.discountPercentage) > 0)
    .sort((a, b) => Number(b.discountPercentage) - Number(a.discountPercentage))
    .slice(0, 6);

  if (loading) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <div className="shimmer h-52 w-full rounded-2xl" />
      </div>
    );
  }
  if (deals.length < 2) return null;

  return (
    <section className="bg-white py-8">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="overflow-hidden rounded-2xl border border-slate-200">
          <div className="flex items-center justify-between border-b border-slate-100 bg-slate-50/70 px-5 py-3.5">
            <div className="flex items-baseline gap-3">
              <h2 className="text-lg font-extrabold text-slate-900">Deals of the Day</h2>
              <span className="hidden text-xs font-medium text-slate-500 sm:block">
                Biggest savings across the catalogue
              </span>
            </div>
            <Link to="/products"
              className="group inline-flex items-center gap-1 rounded-lg bg-brand-600 px-3.5 py-1.5 text-xs font-semibold text-white transition-colors hover:bg-brand-700">
              View all
              <ArrowRight className="h-3 w-3 transition-transform group-hover:translate-x-0.5" />
            </Link>
          </div>

          <div className="no-scrollbar flex divide-x divide-slate-100 overflow-x-auto">
            {deals.map((p, i) => (
              <motion.div key={p.prodId}
                initial={{ opacity: 0, y: 12 }} whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }} transition={{ duration: 0.35, delay: i * 0.05 }}
                className="min-w-[168px] flex-1 sm:min-w-0"
              >
                <Link to={`/products/${p.prodId}`}
                  className="group flex h-full flex-col items-center p-4 text-center transition-colors hover:bg-slate-50">
                  <div className="mb-3 h-24 w-24 overflow-hidden rounded-xl bg-slate-50">
                    <img src={productImage(p.prodImagePath, p.prodName)} alt={p.prodName}
                      loading="lazy"
                      className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
                      onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(p.prodName); }} />
                  </div>
                  <p className="line-clamp-2 min-h-[2.25rem] text-xs font-medium text-slate-700 group-hover:text-brand-600">
                    {p.prodName}
                  </p>
                  <p className="mt-1.5 text-sm font-bold text-emerald-600">
                    {Math.round(Number(p.discountPercentage))}% OFF
                  </p>
                  <p className="text-[11px] text-slate-400">
                    from {formatPrice(p.cardholderPrice ?? p.mrpPrice)}
                  </p>
                </Link>
              </motion.div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
