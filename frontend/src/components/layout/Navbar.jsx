import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ChevronDown, CreditCard, Heart, LogOut, Menu, Package, Search,
  ShoppingCart, User, X, Store, ArrowRight,
} from 'lucide-react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectCartCount } from '@/store/slices/cartSlice';
import { selectWishlistCount } from '@/store/slices/wishlistSlice';
import { useFetch } from '@/hooks/useApi';
import endpoints from '@/api/endpoints';
import useAuth from '@/hooks/useAuth';
import { cn, productImage, placeholderImage } from '@/utils/formatters';

/**
 * Category-driven navbar with a Stripe-style mega-menu.
 *
 * The categories are the REAL tree from /api/categories, so the menu reflects
 * whatever is in the database rather than a hard-coded list.
 *
 * Hover opens the panel on desktop; a short close delay stops it flickering
 * shut as the pointer travels from the trigger down into the panel.
 */
const IconLink = ({ to, icon: Icon, count, label }) => (
  <Link to={to} aria-label={label}
    className="relative rounded-xl p-2 text-slate-500 transition-colors hover:bg-slate-100 hover:text-brand-600">
    <Icon className="h-[18px] w-[18px]" />
    {count > 0 && (
      <motion.span key={count} initial={{ scale: 0.4 }} animate={{ scale: 1 }}
        className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-accent-500 px-1 text-[10px] font-bold text-white">
        {count}
      </motion.span>
    )}
  </Link>
);

export default function Navbar() {
  const { isAuthenticated, user, logout } = useAuth();
  const cartCount = useSelector(selectCartCount);
  const wishCount = useSelector(selectWishlistCount);
  const navigate = useNavigate();

  const { data: categories } = useFetch(endpoints.categories.tree);

  const [scrolled, setScrolled] = useState(false);
  const [openMenu, setOpenMenu] = useState(null);   // catmasterId of the open panel
  const [mobileOpen, setMobileOpen] = useState(false);
  const [mobileAccordion, setMobileAccordion] = useState(null);
  const [term, setTerm] = useState('');
  const closeTimer = useRef(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    window.addEventListener('scroll', onScroll, { passive: true });
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  // Escape closes whatever is open.
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === 'Escape') { setOpenMenu(null); setMobileOpen(false); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, []);

  // Only SIX categories go in the bar. There are 20 top-level categories now,
  // and cramming them all in is what made "Beauty & Health" and "Home
  // Appliances" wrap onto two lines and double the navbar height.
  // Everything else lives behind "All products".
  const allRoots = categories || [];
  const roots = allRoots.slice(0, 6);

  const openNow = (catmasterId) => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
    setOpenMenu(catmasterId);
  };

  const closeSoon = () => {
    closeTimer.current = setTimeout(() => setOpenMenu(null), 140);
  };

  const submitSearch = (e) => {
    e.preventDefault();
    navigate(term.trim() ? `/products?search=${encodeURIComponent(term.trim())}` : '/products');
    setMobileOpen(false);
    setOpenMenu(null);
  };

  return (
    <header
      className={cn('sticky top-0 z-50 transition-all duration-300',
        scrolled ? 'border-b border-slate-200/80 bg-white/85 backdrop-blur-xl' : 'bg-white')}
      onMouseLeave={closeSoon}
    >
      <div className="mx-auto flex h-16 max-w-7xl items-center gap-3 px-4 sm:px-6 lg:px-8">

        <Link to="/" className="flex shrink-0 items-center gap-2">
          <span className="rounded-xl bg-gradient-to-br from-brand-600 to-accent-500 p-1.5 shadow-lg shadow-brand-600/25">
            <Store className="h-[18px] w-[18px] text-white" />
          </span>
          <span className="font-display text-xl font-bold tracking-tight text-slate-900">
            e-<span className="text-gradient">MART</span>
          </span>
        </Link>

        {/* ---------- desktop category nav ---------- */}
        <nav className="ml-2 hidden items-center lg:flex">
          {roots.map((cat) => {
            const hasKids = cat.children?.length > 0;
            return (
              <div key={cat.catmasterId} className="relative"
                onMouseEnter={() => hasKids ? openNow(cat.catmasterId) : setOpenMenu(null)}>
                <Link to={`/categories/${cat.catmasterId}`}
                  className={cn('flex items-center gap-1 whitespace-nowrap rounded-lg px-2.5 py-2 text-[13px] font-medium uppercase tracking-wide transition-colors',
                    openMenu === cat.catmasterId ? 'text-brand-600' : 'text-slate-700 hover:text-brand-600')}>
                  {cat.catName}
                  {hasKids && (
                    <ChevronDown className={cn('h-3.5 w-3.5 transition-transform duration-200',
                      openMenu === cat.catmasterId && 'rotate-180')} />
                  )}
                </Link>
              </div>
            );
          })}
          <NavLink to="/products"
            className="whitespace-nowrap rounded-lg px-2.5 py-2 text-[13px] font-medium uppercase tracking-wide text-slate-700 transition-colors hover:text-brand-600">
            All products
          </NavLink>
        </nav>

        {/* ---------- search ---------- */}
        <form onSubmit={submitSearch} className="ml-auto hidden max-w-xs flex-1 md:block lg:max-w-sm">
          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input value={term} onChange={(e) => setTerm(e.target.value)}
              placeholder="Search products…" aria-label="Search products"
              className="w-full rounded-xl border border-slate-200 bg-slate-50/80 py-2 pl-9 pr-3 text-sm transition-all placeholder:text-slate-400 focus:border-brand-400 focus:bg-white focus:outline-none focus:ring-4 focus:ring-brand-500/10" />
          </div>
        </form>

        <div className="ml-auto flex items-center gap-1 md:ml-0">
          <IconLink to="/wishlist" icon={Heart} count={wishCount} label="Wishlist" />
          <IconLink to="/cart" icon={ShoppingCart} count={cartCount} label="Cart" />

          {isAuthenticated ? (
            <div className="group relative ml-1 hidden md:block">
              <button className="flex items-center gap-2 rounded-xl px-1.5 py-1.5 hover:bg-slate-100">
                <span className="flex h-8 w-8 items-center justify-center rounded-full bg-gradient-to-br from-brand-600 to-accent-500 text-xs font-bold text-white">
                  {user?.firstName?.[0]?.toUpperCase() || 'U'}
                </span>
              </button>
              <div className="invisible absolute right-0 top-full w-60 translate-y-1 pt-2 opacity-0 transition-all group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
                <div className="rounded-2xl border border-slate-200 bg-white p-1.5 shadow-xl shadow-slate-900/5">
                  <div className="border-b border-slate-100 px-3 py-2.5">
                    <p className="truncate text-sm font-semibold text-slate-800">
                      {user?.firstName} {user?.lastName}
                    </p>
                    <p className="truncate text-xs text-slate-500">{user?.email}</p>
                  </div>
                  {[{ to: '/account/profile', icon: User, label: 'My profile' },
                    { to: '/account/orders', icon: Package, label: 'My orders' },
                    { to: '/account/card', icon: CreditCard, label: 'e-MART card' }].map((i) => (
                    <Link key={i.to} to={i.to}
                      className="flex items-center gap-2.5 rounded-xl px-3 py-2 text-sm text-slate-700 hover:bg-slate-50">
                      <i.icon className="h-4 w-4 text-slate-400" />{i.label}
                    </Link>
                  ))}
                  <button onClick={logout}
                    className="flex w-full items-center gap-2.5 rounded-xl px-3 py-2 text-sm text-rose-600 hover:bg-rose-50">
                    <LogOut className="h-4 w-4" />Log out
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <Link to="/login"
              className="ml-1 hidden items-center gap-1 rounded-xl bg-slate-900 px-4 py-2 text-sm font-medium text-white transition-all hover:bg-brand-600 md:flex">
              Sign in <ArrowRight className="h-3.5 w-3.5" />
            </Link>
          )}

          <button onClick={() => setMobileOpen((v) => !v)} aria-label="Menu"
            className="rounded-xl p-2 text-slate-600 hover:bg-slate-100 lg:hidden">
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* ---------- MEGA MENU ---------- */}
      <AnimatePresence>
        {openMenu && (
          <motion.div
            initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
            transition={{ duration: 0.18, ease: [0.16, 1, 0.3, 1] }}
            onMouseEnter={() => openNow(openMenu)}
            className="absolute inset-x-0 top-full hidden border-b border-slate-200 bg-white/95 shadow-xl shadow-slate-900/5 backdrop-blur-xl lg:block"
          >
            {(() => {
              const cat = roots.find((c) => c.catmasterId === openMenu);
              if (!cat) return null;
              return (
                <div className="mx-auto max-w-7xl px-8 py-7">
                  <p className="mb-4 text-xs font-semibold uppercase tracking-wider text-slate-400">
                    Shop {cat.catName}
                  </p>
                  <div className="grid grid-cols-4 gap-1">
                    {cat.children.map((sub) => (
                      <Link key={sub.catmasterId} to={`/categories/${sub.catmasterId}`}
                        onClick={() => setOpenMenu(null)}
                        className="group flex items-center gap-3 rounded-xl p-2.5 transition-colors hover:bg-slate-50">
                        <img src={productImage(sub.catImagePath, sub.catName)} alt=""
                          className="h-11 w-11 shrink-0 rounded-lg bg-slate-50 object-cover ring-1 ring-slate-200"
                          onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }} />
                        <span className="min-w-0">
                          <span className="block truncate text-sm font-medium text-slate-800 group-hover:text-brand-600">
                            {sub.catName}
                          </span>
                          {sub.children?.length > 0 && (
                            <span className="block truncate text-xs text-slate-400">
                              {sub.children.map((g) => g.catName).join(' · ')}
                            </span>
                          )}
                        </span>
                      </Link>
                    ))}
                  </div>
                </div>
              );
            })()}
          </motion.div>
        )}
      </AnimatePresence>

      {/* ---------- MOBILE: hamburger + category accordion ---------- */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="overflow-hidden border-t border-slate-200 bg-white lg:hidden"
          >
            <div className="max-h-[70vh] space-y-1 overflow-y-auto px-4 py-4">
              <form onSubmit={submitSearch} className="mb-3">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                  <input value={term} onChange={(e) => setTerm(e.target.value)}
                    placeholder="Search products…"
                    className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm" />
                </div>
              </form>

              {roots.map((cat) => (
                <div key={cat.catmasterId} className="border-b border-slate-100 last:border-0">
                  <button
                    onClick={() => setMobileAccordion(
                      mobileAccordion === cat.catmasterId ? null : cat.catmasterId)}
                    className="flex w-full items-center justify-between py-3 text-sm font-medium text-slate-800">
                    {cat.catName}
                    {cat.children?.length > 0 && (
                      <ChevronDown className={cn('h-4 w-4 text-slate-400 transition-transform',
                        mobileAccordion === cat.catmasterId && 'rotate-180')} />
                    )}
                  </button>
                  <AnimatePresence>
                    {mobileAccordion === cat.catmasterId && cat.children?.length > 0 && (
                      <motion.div initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }}
                        className="overflow-hidden pb-2">
                        {cat.children.map((sub) => (
                          <Link key={sub.catmasterId} to={`/categories/${sub.catmasterId}`}
                            onClick={() => setMobileOpen(false)}
                            className="block rounded-lg py-2 pl-3 text-sm text-slate-600 hover:bg-slate-50">
                            {sub.catName}
                          </Link>
                        ))}
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              ))}

              <div className="!mt-4 space-y-1 border-t border-slate-100 pt-3">
                {[{ to: '/products', label: 'All products' },
                  { to: '/wishlist', label: 'Wishlist' },
                  { to: '/cart', label: 'Cart' },
                  ...(isAuthenticated
                    ? [{ to: '/account/profile', label: 'My profile' },
                       { to: '/account/orders', label: 'My orders' },
                       { to: '/account/card', label: 'e-MART card' }]
                    : [{ to: '/login', label: 'Sign in' },
                       { to: '/register', label: 'Create an account' }])].map((l) => (
                  <Link key={l.to} to={l.to} onClick={() => setMobileOpen(false)}
                    className="block rounded-lg px-1 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                    {l.label}
                  </Link>
                ))}
                {isAuthenticated && (
                  <button onClick={() => { setMobileOpen(false); logout(); }}
                    className="block w-full rounded-lg px-1 py-2 text-left text-sm font-medium text-rose-600">
                    Log out
                  </button>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
