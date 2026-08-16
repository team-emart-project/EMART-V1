# Homepage redesign — Stripe-inspired

Built in the existing React + Vite + Tailwind v4 app. **Wired to the real API**,
not dummy data — every category, product and price on the page comes from your
Spring Boot backend.

```bash
cd frontend
npm install      # only if you have not already
npm run dev
```

---

## What was built

| # | Section | File |
|---|---|---|
| 1 | Category navbar + mega-menu | `components/layout/Navbar.jsx` |
| 2 | Gradient hero | `components/home/HeroSection.jsx` |
| 3 | Bento grid | `components/home/BentoGrid.jsx` |
| 4 | Stats / trust bar | `components/home/StatsBar.jsx` |
| 5 | Product carousels | `components/home/ProductCarousel.jsx` |
| 6 | Testimonials | `components/home/Testimonials.jsx` |
| 7 | Multi-column footer + newsletter | `components/layout/Footer.jsx` |
| — | Page composition | `pages/home/HomePage.jsx` |
| — | Design tokens & animations | `index.css` |

### 1. Navbar
Sticky, translucent-on-scroll. The nav items are the **real root categories**
from `GET /api/categories`, so the menu tracks the database instead of a
hard-coded list. Hovering a category with children opens a mega-menu: a
three-column grid of sub-categories with thumbnails, third-level names as
subtext, plus a gradient promo card for the e-MART card. A short close delay
stops the panel flickering shut as the pointer travels into it. Right side has
search, wishlist and cart (with live count badges from Redux), and an account
menu. Escape closes everything.

### 2. Hero
Three blurred gradient blobs on a 22-second drift loop, plus a faint grid that
masks out toward the bottom. Blurred divs rather than an image or canvas —
cheap, resolution-independent, and frozen entirely under
`prefers-reduced-motion`.

### 3. Bento grid
Asymmetric `auto-rows` grid: a tall 2×2 gradient card for the e-MART card, a
wide deals card, two small cards, then real featured categories from
`GET /api/home/featured-categories` as image tiles. Collapses to one column on
mobile — the spans only apply from `md:` up.

### 4. Stats bar
Four figures with gradient numerals. Marketing values, kept in one `STATS`
array at the top of the file so they are obvious to change.

### 5. Product carousels
One rail per root category, each calling
`GET /api/categories/{id}/products?includeSubCategories=true`. Native
`overflow-x` with CSS scroll-snap; the arrows just nudge `scrollLeft`, so
touch and trackpad behaviour comes for free. A rail with no products renders
nothing rather than an empty row. Cards show image, name, MRP, member price
(when applicable), points badge and Add to cart.

### 6. Testimonials
Three quote cards with gradient icon chips and star ratings. Static copy —
there is no reviews table in the schema.

### 7. Footer
Five columns. The Categories column is populated from
`GET /api/categories?flat=true`, so it never drifts from the catalogue. The
newsletter input is **UI-only** — there is no subscribe endpoint on the backend.

---

## Design system

Tailwind v4 keeps the theme in CSS, so everything lives in `@theme` in
`index.css`.

- **Type** — Sora for headings, Inter for body (loaded in `index.html`).
- **Colour** — one bold brand colour (indigo `#4f46e5`), neutral slate greys,
  and a pink/cyan gradient used only as an accent, never for body text.
- **Radius** — 12–16px on cards, 12px on controls.
- **Motion** — `fade-up` on scroll (fires once, not on every pass), hover-lift
  on cards, spring press on buttons, 22s drifting gradients.
- **Reduced motion** — every animation and transition collapses to ~0ms under
  `prefers-reduced-motion`.

Responsive throughout: mega-menu from `lg:` up, hamburger with a category
accordion below it, bento reflows to one column, carousels stay swipeable.

---

## One correctness fix made along the way

`PriceTag` used to decide whether to show the member price from the **cached**
`user.isCardholder` flag in Redux. That value is written at login and goes
stale the moment a card is approved mid-session — the user would have been
charged the member price at checkout while still seeing MRP on the page.

It now keys off whether the API actually sent `cardholderPrice`. The backend
omits that field entirely for non-cardholders (`@JsonInclude(NON_NULL)`), so
the payload is the authoritative signal and display can never disagree with
entitlement.

---

## Not wired up

- **Newsletter** — no backend endpoint exists.
- **Testimonials** — static; no reviews table.
- **Stats figures** — marketing copy, not live counts.
- **Product images** — `formatCurrency`/`productImage` fall back to a
  placeholder service, since the seeded `prod_image_path` values point at files
  that do not exist yet. Drop real images into `public/images/products/` and
  they will be used automatically.
