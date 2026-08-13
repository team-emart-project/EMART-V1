import { motion } from 'framer-motion';
import { ArrowRight } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * Three promotional tiles, the row Myntra runs below its deals strip.
 * Pure CSS gradients — no images to load, so nothing can 404.
 *
 * DELIBERATELY OUTSIDE THE BRAND PALETTE
 * --------------------------------------
 * The rest of the site is one colour (#1A4247). Merchandising is the
 * exception: a promotional tile has to look like a promotion, and three
 * tiles cut from the brand ramp read as three UI panels rather than three
 * offers. So these keep their own vivid gradients, the same way the banner
 * artwork does. Do not "fix" them to match the theme.
 */
const TILES = [
  { title: 'Fashion Fest',      sub: 'Min 40% off top brands',   to: '/products?search=shirt',
    from: 'from-pink-500',   via: 'via-rose-500',    to2: 'to-orange-400' },
  { title: 'Tech Upgrade',      sub: 'Phones, laptops & audio',   to: '/products?search=phone',
    from: 'from-indigo-600', via: 'via-violet-600',  to2: 'to-blue-500' },
  { title: 'Home Essentials',   sub: 'Kitchen & appliances',      to: '/products?search=kitchen',
    from: 'from-teal-500',   via: 'via-emerald-500', to2: 'to-lime-400' },
];

export default function PromoTiles() {
  return (
    <section className="bg-white pb-10">
      <div className="mx-auto grid max-w-7xl gap-4 px-4 sm:px-6 md:grid-cols-3 lg:px-8">
        {TILES.map((t, i) => (
          <motion.div key={t.title}
            initial={{ opacity: 0, y: 18 }} whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }} transition={{ duration: 0.4, delay: i * 0.08 }}
          >
            <Link to={t.to}
              className={`group relative flex h-40 flex-col justify-end overflow-hidden rounded-2xl bg-gradient-to-br ${t.from} ${t.via} ${t.to2} p-6 text-white transition-all duration-300 hover:-translate-y-1 hover:shadow-2xl hover:shadow-slate-900/25`}>
              <span className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-white/15" aria-hidden />
              <span className="absolute -bottom-10 -left-6 h-32 w-32 rounded-full bg-white/10" aria-hidden />
              <span className="relative">
                <span className="block font-display text-xl font-bold">{t.title}</span>
                <span className="mt-0.5 block text-sm text-white/85">{t.sub}</span>
                <span className="mt-2 inline-flex items-center gap-1 text-xs font-semibold">
                  Shop now
                  <ArrowRight className="h-3 w-3 transition-transform group-hover:translate-x-1" />
                </span>
              </span>
            </Link>
          </motion.div>
        ))}
      </div>
    </section>
  );
}
