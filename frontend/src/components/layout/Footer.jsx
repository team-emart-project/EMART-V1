import { useState } from 'react';
import { Facebook, Github, Instagram, Linkedin, Send, Store, Twitter } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import { toastSuccess } from '@/store/slices/uiSlice';

/** Multi-column footer with a newsletter capture, Stripe-style. */
export default function Footer() {
  const dispatch = useDispatch();
  const [email, setEmail] = useState('');

  // Real root categories, so the footer never drifts from the catalogue.
  const { data: categories } = useFetch(endpoints.categories.roots);

  const COLUMNS = [
    {
      title: 'Company',
      links: [
        { label: 'About e-MART', to: '/' },
        { label: 'Careers', to: '/' },
        { label: 'Partners', to: '/' },
        { label: 'Newsroom', to: '/' },
      ],
    },
    {
      title: 'Support',
      links: [
        { label: 'Help centre', to: '/' },
        { label: 'Track an order', to: '/account/orders' },
        { label: 'Returns', to: '/' },
        { label: 'Contact us', to: '/' },
      ],
    },
    {
      title: 'Legal',
      links: [
        { label: 'Privacy policy', to: '/' },
        { label: 'Terms of service', to: '/' },
        { label: 'Cookie settings', to: '/' },
        { label: 'Licences', to: '/' },
      ],
    },
  ];

  const submit = (e) => {
    e.preventDefault();
    // No newsletter endpoint exists on the backend, so this is UI-only for now.
    dispatch(toastSuccess('Thanks! We will keep you posted.'));
    setEmail('');
  };

  return (
    <footer className="border-t border-slate-200 bg-white">
      <div className="mx-auto max-w-7xl px-4 py-16 sm:px-6 lg:px-8">
        <div className="grid gap-10 lg:grid-cols-[1.4fr_repeat(4,1fr)]">

          {/* brand + newsletter */}
          <div>
            <Link to="/" className="flex items-center gap-2">
              <span className="rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 p-1.5">
                <Store className="h-[18px] w-[18px] text-white" />
              </span>
              <span className="font-display text-xl font-bold text-slate-900">
                e-<span className="text-gradient">MART</span>
              </span>
            </Link>

            <p className="mt-4 max-w-xs text-sm leading-relaxed text-slate-500">
              Electronics, groceries, fashion and more. Join free — carry an
              e-MART card for member pricing and points on every order.
            </p>

            <form onSubmit={submit} className="mt-6">
              <label htmlFor="newsletter" className="block text-xs font-semibold uppercase tracking-wider text-slate-400">
                Stay in the loop
              </label>
              <div className="mt-2 flex gap-2">
                <input id="newsletter" type="email" required value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  className="min-w-0 flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3.5 py-2.5 text-sm transition-all placeholder:text-slate-400 focus:border-brand-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-brand-500/10" />
                <button type="submit" aria-label="Subscribe"
                  className="shrink-0 rounded-xl bg-brand-gradient px-3.5 text-white transition-all hover:brightness-110">
                  <Send className="h-4 w-4" />
                </button>
              </div>
            </form>
          </div>

          {/* categories, straight from the DB */}
          <div>
            <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
              Categories
            </h3>
            <ul className="mt-4 space-y-2.5">
              {(categories || []).slice(0, 6).map((c) => (
                <li key={c.catmasterId}>
                  <Link to={`/categories/${c.catmasterId}`}
                    className="text-sm text-slate-600 transition-colors hover:text-brand-600">
                    {c.catName}
                  </Link>
                </li>
              ))}
              <li>
                <Link to="/products" className="text-sm font-medium text-brand-600 hover:underline">
                  All products
                </Link>
              </li>
            </ul>
          </div>

          {COLUMNS.map((col) => (
            <div key={col.title}>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-slate-400">
                {col.title}
              </h3>
              <ul className="mt-4 space-y-2.5">
                {col.links.map((l) => (
                  <li key={l.label}>
                    <Link to={l.to}
                      className="text-sm text-slate-600 transition-colors hover:text-brand-600">
                      {l.label}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-14 flex flex-col items-center justify-between gap-4 border-t border-slate-100 pt-8 sm:flex-row">
          <p className="text-xs text-slate-400">
            © {new Date().getFullYear()} e-MART — student project built for the VITA BRD.
          </p>
          <div className="flex gap-1">
            {[Twitter, Instagram, Facebook, Linkedin, Github].map((Icon, i) => (
              <a key={i} href="#" aria-label="Social link"
                className="rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-100 hover:text-brand-600">
                <Icon className="h-4 w-4" />
              </a>
            ))}
          </div>
        </div>
      </div>
    </footer>
  );
}
