import { useEffect, useMemo, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import {
  ChevronDown, CreditCard, Heart, LogOut, Menu, Package, Search,
  ShoppingBag, User, X,
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
 * Minimal editorial navbar — the Finery/COS layout.
 *
 *   [ wordmark ]   NEW IN  CLOTHING  ...  SALE          [search ___]  ♡  bag
 *
 * Everything is flat: no pill backgrounds, no gradient logo badge, no filled
 * buttons. Weight and letter-spacing carry the hierarchy instead of colour,
 * which is what makes this style read as "fashion retail" rather than
 * "dashboard". The only colour in the bar is the red SALE link and the cart
 * count bubble.
 *
 * Functionality is unchanged from the previous version: the real category tree
 * still drives the links and the hover mega-menu, the account dropdown and the
 * mobile accordion both still work.
 */

/**
 * Thin outline icon button, with an optional count bubble.
 *
 * The bar is #1A4247 now, so the bubble INVERTS: white pill, brand text.
 * A dark bubble on a dark bar would disappear, which defeats the one thing a
 * cart count exists to do.
 */
const IconLink = ({ to, icon: Icon, count, label, onClick }) => (
  <Link
    to={to}
    onClick={onClick}
    aria-label={label}
    className="relative p-1.5 text-white transition-opacity hover:opacity-70"
  >
    <Icon className="h-[21px] w-[21px]" strokeWidth={1.3} />
    {count > 0 && (
      <motion.span
        key={count}
        initial={{ scale: 0.4 }}
        animate={{ scale: 1 }}
        className="absolute -right-1 -top-1 flex h-[17px] min-w-[17px] items-center justify-center rounded-full bg-white px-1 text-[10px] font-bold text-brand-700 shadow-sm"
      >
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

  // ---------------------------------------------------------------------
  // WHICH CATEGORIES GET A SLOT IN THE BAR
  //
  // The tree comes from the database, so the bar cannot be a hard-coded list —
  // but it can be curated. HIDDEN drops roots that do not belong in a fashion-
  // led navbar, and CLOTHING_RE promotes the apparel root to the front, since
  // that is the entry point most shoppers want.
  //
  // Only FIVE make it in: this layout lives or dies on white space, and a
  // wrapped second row would ruin it. Everything else sits behind SHOP ALL.
  // ---------------------------------------------------------------------
  const HIDDEN = /^(automotive|auto|vehicles?)\b/i;
  const CLOTHING_RE = /(cloth|apparel|fashion|wear)/i;

  const roots = useMemo(() => {
    const visible = (categories || []).filter((c) => !HIDDEN.test(c.catName || ''));
    const clothing = visible.filter((c) => CLOTHING_RE.test(c.catName || ''));
    const rest = visible.filter((c) => !CLOTHING_RE.test(c.catName || ''));
    const ordered = [...clothing, ...rest].slice(0, 5);

    // No apparel root in the catalogue yet? Still give the bar a CLOTHING
    // entry — it searches instead of linking to a category that isn't there.
    if (clothing.length === 0) {
      ordered.pop();
      ordered.unshift({
        catmasterId: '__clothing',
        catName: 'Clothing',
        to: '/products?search=clothing',
        children: [],
      });
    }
    return ordered;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [categories]);

  /** Categories carry an id; the synthetic Clothing entry carries a url. */
  const hrefFor = (cat) => cat.to || `/categories/${cat.catmasterId}`;

  /**
   * Database category names are written for a catalogue page, not a nav bar:
   * "Bags, Wallets & Belts" and "Beauty and Personal Care" are 21 and 24
   * characters, and five of those in a row are wider than the viewport — which
   * is what pushed the search box and the cart icon off the right edge.
   *
   * So the bar shows the LEAD noun and drops the rest. The full name is still
   * on the link title and in the mega-menu heading, and the destination is
   * unchanged, so nothing is actually lost.
   */
  const shortName = (name = '') => {
    let s = String(name).split(/[,/|]/)[0].trim();          // "Bags, Wallets" -> "Bags"
    if (s.length > 14) s = s.split(/\s+(?:and|&)\s+/i)[0].trim(); // "Beauty and Personal Care" -> "Beauty"
    if (s.length > 16) s = `${s.slice(0, 15).trimEnd()}…`;
    return s;
  };

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

  // Every link is a hover target for the sliding underline, so they all share
  // one class string. `group` + an absolutely positioned rule = an underline
  // that grows from the left instead of appearing all at once.
  // Everything in the bar is white on #1A4247. The underline is white too, and
  // hover dims rather than darkens — on a dark surface, "less ink" has to mean
  // less opacity, since there is no lighter shade of white to move to.
  const linkClass = 'group relative whitespace-nowrap py-2 text-[13px] font-medium uppercase tracking-[0.08em] text-white transition-opacity hover:opacity-75';
  const underline = 'pointer-events-none absolute -bottom-0.5 left-0 h-px w-full origin-left scale-x-0 bg-white transition-transform duration-300 ease-out group-hover:scale-x-100';

  return (
    <header
      className={cn('sticky top-0 z-50 bg-brand-600 transition-shadow duration-300',
        scrolled ? 'shadow-lg shadow-brand-900/25' : '')}
      onMouseLeave={closeSoon}
    >
      {/* ---------- announcement ticker ----------
          A shade DEEPER than the bar, not a different colour — the only thing
          separating the two strips is depth. Collapses to nothing on scroll so
          the bar stays compact once the shopper is reading the page. */}
      <div className={cn('overflow-hidden bg-brand-800 transition-all duration-300',
        scrolled ? 'h-0 opacity-0' : 'h-8 opacity-100')}>
        <div className="mx-auto flex h-8 max-w-7xl items-center justify-center gap-8 px-4 text-[11px] font-medium uppercase tracking-[0.16em] text-white sm:px-6 lg:px-8">
          <span>Free delivery over ₹499</span>
          <span className="hidden text-white/40 sm:inline">/</span>
          <span className="hidden sm:inline">Extra 5% back on the e-MART card</span>
          <span className="hidden text-white/40 md:inline">/</span>
          <span className="hidden md:inline">Easy 7-day returns</span>
        </div>
      </div>

      {/* max-w-7xl + the same padding scale the home page sections use, so the
          wordmark sits exactly above the first product card rather than
          floating out in the margin. */}
      <div className="mx-auto flex h-[72px] max-w-7xl items-center gap-4 px-4 sm:px-6 lg:px-8 xl:gap-6">

        {/* ---------- wordmark ---------- */}
        <Link to="/" className="group shrink-0" aria-label="e-MART home">
          <span className="font-display text-[24px] font-bold uppercase leading-none tracking-[0.16em] text-white">
            e<span className="inline-block text-white/60 transition-transform duration-300 group-hover:rotate-90">·</span>mart
          </span>
        </Link>

        {/* ---------- desktop category nav ----------
            min-w-0 + overflow-hidden is the load-bearing part: without it a
            flex child with nowrap text refuses to shrink, so a long category
            name grows the row past the container and shoves the search box and
            cart icon off the right edge instead of being clipped itself. */}
        <nav className="hidden min-w-0 flex-1 items-center gap-5 overflow-hidden lg:flex xl:gap-7">
          {roots.map((cat) => {
            const hasKids = cat.children?.length > 0;
            const open = openMenu === cat.catmasterId;
            return (
              <div key={cat.catmasterId} className="shrink-0"
                onMouseEnter={() => (hasKids ? openNow(cat.catmasterId) : setOpenMenu(null))}>
                <Link to={hrefFor(cat)} title={cat.catName} className={linkClass}>
                  {shortName(cat.catName)}
                  <span className={cn(underline, open && 'scale-x-100')} aria-hidden />
                </Link>
              </div>
            );
          })}
          <NavLink to="/products" onMouseEnter={() => setOpenMenu(null)}
            className={cn(linkClass, 'shrink-0')}>
            Shop all
            <span className={underline} aria-hidden />
          </NavLink>
        </nav>

        {/* ---------- right cluster: underlined search + icons ----------
            shrink-0 so the icons keep their size and stay inside the container
            no matter how many categories the nav is trying to show. */}
        <div className="ml-auto flex shrink-0 items-center gap-3 sm:gap-4 xl:gap-5">
          <form onSubmit={submitSearch} className="hidden w-36 md:block lg:w-40 xl:w-52">
            <div className="relative">
              <input value={term} onChange={(e) => setTerm(e.target.value)}
                placeholder="Search" aria-label="Search products"
                className="w-full border-0 border-b border-white/35 bg-transparent pb-1.5 pr-7 text-sm text-white transition-colors placeholder:text-white/55 focus:border-white focus:outline-none focus:ring-0" />
              <button type="submit" aria-label="Search"
                className="absolute right-0 top-0 text-white/75 transition-opacity hover:opacity-100">
                <Search className="h-[18px] w-[18px]" strokeWidth={1.4} />
              </button>
            </div>
          </form>

          {/* account — icon only, dropdown when signed in, straight to login when not */}
          {isAuthenticated ? (
            <div className="group relative hidden md:block">
              <button aria-label="Account"
                className="p-1.5 text-white transition-opacity hover:opacity-70">
                <User className="h-[21px] w-[21px]" strokeWidth={1.3} />
              </button>
              {/* The panel is a DEEPER step of the same colour, not a white
                  card: a white card hanging off a dark bar reads as a browser
                  popup rather than part of the header. */}
              <div className="invisible absolute right-0 top-full w-60 translate-y-1 pt-3 opacity-0 transition-all group-hover:visible group-hover:translate-y-0 group-hover:opacity-100">
                <div className="border border-white/10 bg-brand-700 p-1 shadow-xl shadow-brand-900/40">
                  <div className="border-b border-white/12 px-3 py-2.5">
                    <p className="truncate text-sm font-semibold text-white">
                      {user?.firstName} {user?.lastName}
                    </p>
                    <p className="truncate text-xs text-white/60">{user?.email}</p>
                  </div>
                  {[{ to: '/account/profile', icon: User, label: 'My profile' },
                    { to: '/account/orders', icon: Package, label: 'My orders' },
                    { to: '/account/card', icon: CreditCard, label: 'e-MART card' }].map((i) => (
                    <Link key={i.to} to={i.to}
                      className="flex items-center gap-2.5 px-3 py-2 text-sm text-white/85 transition-colors hover:bg-white/10 hover:text-white">
                      <i.icon className="h-4 w-4 text-white/50" strokeWidth={1.5} />{i.label}
                    </Link>
                  ))}
                  <button onClick={logout}
                    className="flex w-full items-center gap-2.5 px-3 py-2 text-sm text-rose-300 transition-colors hover:bg-rose-500/15 hover:text-rose-200">
                    <LogOut className="h-4 w-4" strokeWidth={1.5} />Log out
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <Link to="/login" aria-label="Sign in"
              className="hidden p-1.5 text-white transition-opacity hover:opacity-70 md:block">
              <User className="h-[21px] w-[21px]" strokeWidth={1.3} />
            </Link>
          )}

          <IconLink to="/wishlist" icon={Heart} count={wishCount} label="Wishlist" />
          <IconLink to="/cart" icon={ShoppingBag} count={cartCount} label="Cart" />

          <button onClick={() => setMobileOpen((v) => !v)} aria-label="Menu"
            className="p-1.5 text-white lg:hidden">
            {mobileOpen
              ? <X className="h-5 w-5" strokeWidth={1.4} />
              : <Menu className="h-5 w-5" strokeWidth={1.4} />}
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
            className="absolute inset-x-0 top-full hidden border-t border-white/10 bg-brand-700 shadow-2xl shadow-brand-900/40 lg:block"
          >
            {(() => {
              const cat = roots.find((c) => c.catmasterId === openMenu);
              if (!cat) return null;
              return (
                <div className="mx-auto max-w-7xl px-4 py-9 sm:px-6 lg:px-8">
                  <p className="mb-5 text-[11px] font-medium uppercase tracking-[0.18em] text-white/50">
                    {cat.catName}
                  </p>
                  <div className="grid grid-cols-4 gap-x-8 gap-y-1">
                    {cat.children.map((sub) => (
                      <Link key={sub.catmasterId} to={`/categories/${sub.catmasterId}`}
                        onClick={() => setOpenMenu(null)}
                        className="group flex items-center gap-3 rounded-lg px-2 py-2.5 transition-colors hover:bg-white/8">
                        {/* the thumbnails are light artwork, so they keep a
                            white plate to sit on rather than being punched
                            straight onto the dark panel */}
                        <img src={productImage(sub.catImagePath, sub.catName)} alt=""
                          className="h-12 w-12 shrink-0 rounded bg-white object-cover"
                          onError={(e) => { e.currentTarget.onerror = null; e.currentTarget.src = placeholderImage(); }} />
                        <span className="min-w-0">
                          <span className="block truncate text-sm text-white/90 transition-colors group-hover:text-white">
                            {sub.catName}
                          </span>
                          {sub.children?.length > 0 && (
                            <span className="block truncate text-xs text-white/45">
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
            className="overflow-hidden border-t border-white/10 bg-brand-700 lg:hidden"
          >
            <div className="mx-auto max-h-[70vh] max-w-7xl overflow-y-auto px-4 py-4 sm:px-6 lg:px-8">
              <form onSubmit={submitSearch} className="mb-4">
                <div className="relative">
                  <input value={term} onChange={(e) => setTerm(e.target.value)}
                    placeholder="Search"
                    className="w-full border-0 border-b border-white/35 bg-transparent pb-2 pr-7 text-sm text-white placeholder:text-white/55 focus:border-white focus:outline-none" />
                  <Search className="absolute right-0 top-0 h-[18px] w-[18px] text-white/75" strokeWidth={1.4} />
                </div>
              </form>

              {roots.map((cat) => (
                <div key={cat.catmasterId} className="border-b border-white/10">
                  {/* A root with no children (the synthetic Clothing entry) has
                      nothing to expand, so it navigates instead of toggling —
                      a dead accordion button is worse than no accordion. */}
                  {cat.children?.length > 0 ? (
                    <button
                      onClick={() => setMobileAccordion(
                        mobileAccordion === cat.catmasterId ? null : cat.catmasterId)}
                      className="flex w-full items-center justify-between py-3.5 text-[13px] font-medium uppercase tracking-[0.08em] text-white">
                      {cat.catName}
                      <ChevronDown className={cn('h-4 w-4 text-white/55 transition-transform',
                        mobileAccordion === cat.catmasterId && 'rotate-180')} strokeWidth={1.5} />
                    </button>
                  ) : (
                    <Link to={hrefFor(cat)} onClick={() => setMobileOpen(false)}
                      className="block py-3.5 text-[13px] font-medium uppercase tracking-[0.08em] text-white">
                      {cat.catName}
                    </Link>
                  )}
                  <AnimatePresence>
                    {mobileAccordion === cat.catmasterId && cat.children?.length > 0 && (
                      <motion.div initial={{ height: 0 }} animate={{ height: 'auto' }} exit={{ height: 0 }}
                        className="overflow-hidden pb-2">
                        {cat.children.map((sub) => (
                          <Link key={sub.catmasterId} to={`/categories/${sub.catmasterId}`}
                            onClick={() => setMobileOpen(false)}
                            className="block py-2 pl-3 text-sm text-white/70">
                            {sub.catName}
                          </Link>
                        ))}
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              ))}

              <div className="mt-4 space-y-1 pt-1">
                {[{ to: '/products', label: 'Shop all' },
                  { to: '/wishlist', label: 'Wishlist' },
                  { to: '/cart', label: 'Cart' },
                  ...(isAuthenticated
                    ? [{ to: '/account/profile', label: 'My profile' },
                       { to: '/account/orders', label: 'My orders' },
                       { to: '/account/card', label: 'e-MART card' }]
                    : [{ to: '/login', label: 'Sign in' },
                       { to: '/register', label: 'Create an account' }])].map((l) => (
                  <Link key={l.to} to={l.to} onClick={() => setMobileOpen(false)}
                    className="block py-2 text-[13px] font-medium uppercase tracking-[0.08em] text-white/85">
                    {l.label}
                  </Link>
                ))}
                {isAuthenticated && (
                  <button onClick={() => { setMobileOpen(false); logout(); }}
                    className="block w-full py-2 text-left text-[13px] font-medium uppercase tracking-[0.08em] text-rose-300">
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
