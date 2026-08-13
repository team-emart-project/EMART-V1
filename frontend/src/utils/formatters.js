/** Formatting helpers, so currency and dates look the same everywhere. */

/**
 * A money amount with the rupee sign and Indian digit grouping.
 *
 *   1600    -> ₹1,600
 *   125000  -> ₹1,25,000
 *   99.5    -> ₹99.50
 *
 * A bare "100" on screen reads as a quantity, a rank, or nothing at all. The
 * symbol is what tells a reader the number is money. Prices in the database
 * are clean round numbers, so whole amounts print with no decimal part; a
 * value with paise still shows both places rather than being rounded away.
 */
export const formatPrice = (amount) => {
  const value = Number(amount ?? 0);
  if (!Number.isFinite(value)) return '₹0';
  const digits = Number.isInteger(value) ? 0 : 2;
  return `₹${value.toLocaleString('en-IN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  })}`;
};

/**
 * A bare number with grouping and no symbol. For e-Points, which are a count,
 * not currency — "₹450 e-Points" would be nonsense.
 */
export const formatPlain = (amount) => {
  const value = Number(amount ?? 0);
  if (!Number.isFinite(value)) return '0';
  const digits = Number.isInteger(value) ? 0 : 2;
  return value.toLocaleString('en-IN', {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  });
};

export const formatDate = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
};

export const formatDateTime = (value) => {
  if (!value) return '-';
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
};

/** Joins class names, dropping falsy ones — avoids a clsx dependency. */
export const cn = (...classes) => classes.filter(Boolean).join(' ');

export const statusColor = (status) => {
  const map = {
    PLACED: 'bg-blue-100 text-blue-700',
    PAID: 'bg-emerald-100 text-emerald-700',
    SUCCESS: 'bg-emerald-100 text-emerald-700',
    PENDING: 'bg-amber-100 text-amber-700',
    CANCELLED: 'bg-rose-100 text-rose-700',
    FAILED: 'bg-rose-100 text-rose-700',
    REJECTED: 'bg-rose-100 text-rose-700',
    APPROVED: 'bg-emerald-100 text-emerald-700',
    SHIPPED: 'bg-teal-100 text-teal-800',
    DELIVERED: 'bg-emerald-100 text-emerald-700',
  };
  return map[status] || 'bg-slate-100 text-slate-700';
};

/**
 * Resolve a product / category image URL.
 *
 * THE BUG THIS FIXES
 * ------------------
 * The old version only passed through paths starting with "http" and sent
 * EVERYTHING ELSE to an external placeholder service:
 *
 *     path.startsWith('http') ? path : `https://placehold.co/...`
 *
 * Seeded products stored "/images/products/p1.png" — an app-relative path — so
 * every card silently fell through to the placeholder, and when that external
 * service was slow or blocked, nothing rendered at all.
 *
 * Images now live in frontend/public/images/, so an app-relative path is the
 * NORMAL case and must be returned as-is. No network, no CDN, no CORS.
 */
export const productImage = (path, name = 'product') => {
  if (typeof path === 'string' && path.trim()) {
    const p = path.trim();
    // absolute URL, app-relative path, or inline data URI - all usable directly
    if (p.startsWith('http://') || p.startsWith('https://')
        || p.startsWith('/') || p.startsWith('data:')) {
      return p;
    }
    // bare filename from an older import - assume the products folder
    return `/images/products/${p}`;
  }
  return placeholderImage(name);
};

/**
 * Last-resort inline SVG for a product with no image at all.
 *
 * Deliberately a data URI rather than a call to an external placeholder
 * service: a fallback that itself depends on the network is not a fallback.
 */
export const placeholderImage = (name = 'e-MART') => {
  const label = String(name || 'e-MART').slice(0, 22);
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 600">
<defs><linearGradient id="p" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#ecfdf5"/><stop offset="100%" stop-color="#d1fae5"/></linearGradient></defs>
<rect width="600" height="600" fill="url(#p)"/>
<circle cx="300" cy="250" r="90" fill="#ffffff" opacity="0.7"/>
<path d="M270 220h60v60h-60z" fill="none" stroke="#059669" stroke-width="6"/>
<text x="300" y="400" font-family="Inter,sans-serif" font-size="30" font-weight="600"
      fill="#0f766e" text-anchor="middle">${label.replace(/[<>&]/g, '')}</text></svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};
