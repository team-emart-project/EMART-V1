# e-MART frontend

React 19 + Vite + Tailwind v4 + Redux Toolkit. Talks to the Spring Boot backend
in `../backend`.

## Run it

```bash
cd frontend
npm install          # required - new dependencies were added
npm run dev          # http://localhost:5173
```

Start the backend first (`cd backend && mvnw.cmd clean spring-boot:run`), and
load `emart_schema.sql` + `emart_seed_data.sql` so there is data to show.

**No CORS setup is needed.** `vite.config.js` proxies `/api` to
`http://localhost:8080`, so the browser only ever sees one origin.

### Demo logins (password `Password@123`)

| Email | What it shows |
|---|---|
| `rishi.chhalotre@example.com` | Approved cardholder — member pricing, 350 e-Points |
| `ananya.sharma@example.com` | Normal member — MRP pricing, no points |

Mock payment: `4242424242424242` succeeds, any card ending in **0** is declined.

---

## How each requirement is met

### 1. Protected routes
`src/routes/ProtectedRoute.jsx` — used as a **layout route**, so one guard
covers a whole group of pages instead of being repeated on each:

```jsx
<Route element={<ProtectedRoute />}>
  <Route path="/cart" element={<CartPage />} />
  <Route path="/checkout" element={<CheckoutPage />} />
  ...
</Route>
```

It remembers where the visitor was heading in `location.state`, so after
logging in they land on that page rather than the home page.
`PublicOnlyRoute.jsx` is the mirror image — it keeps a logged-in user off the
login/register screens.

> This is a UX guard, not a security boundary. The real enforcement is the JWT
> check on the server; someone who edits localStorage just gets 401s.

### 2. Lazy loading
`src/routes/AppRoutes.jsx` — every page is `React.lazy()` + one `<Suspense>`.
Vite emits a separate chunk per route, so landing on the home page never
downloads the checkout or payment code.

### 3. Reusable components
`src/components/ui/` — `Button`, `Input`, `Card`, `Badge`, `Modal`, `Spinner`,
`Skeleton`, `EmptyState`, `Toast`, `Pagination`, with a barrel export so
`import { Button, Card } from '@/components/ui'` works.

`src/components/common/` — the domain pieces reused across pages: `ProductCard`
(home, search, category), `PriceTag`, `CategoryTree`, `QuantityStepper`,
`Reveal`, `ErrorBoundary`. `pages/user/AccountNav.jsx` is shared by all four
account pages.

### 4. One API hook — no duplicated GET vs getById
`src/hooks/useApi.js`.

A list and a single record are **not** different operations — they are the same
GET against a different URL. So there is one `useFetch`, not a `useGetAll` plus
a `useGetById`:

```js
useFetch(endpoints.products.search())   // every product
useFetch(endpoints.products.byId(5))    // product 5
useFetch(endpoints.orders.root)         // my orders
useFetch(endpoints.orders.byId(9))      // order 9
```

URL construction already lives in `endpoints.js`, so the hook never needs to
know the difference. It also handles cancel-on-unmount and discards stale
responses, so fast typing in the search box cannot let an older result
overwrite a newer one.

`useMutation` is the write-side twin (`usePost` / `usePut` / `useDelete`).

### 5. Redux
`src/store/` with Redux Toolkit slices:

| Slice | Why it is global |
|---|---|
| `authSlice` | User + token needed by the router, navbar and every page |
| `cartSlice` | The navbar badge, cart page and checkout must all agree |
| `wishlistSlice` | Product cards need to know what is already saved |
| `uiSlice` | Toasts — any thunk anywhere can raise one |

The server is the source of truth: every cart thunk returns the whole
recalculated cart, so totals can never drift from the backend's figures.

### 6. Clean API code
No component ever calls `fetch` or types a URL.

- `api/axiosClient.js` — one axios instance. Attaches the JWT, unwraps the
  `{ success, message, data }` envelope so components see plain data, collapses
  every failure into one shape, and on a 401 clears the token and redirects.
- `api/endpoints.js` — every URL in one file. A route change on the server is a
  one-line edit here.

---

## Structure

```
src/
├── api/           axiosClient.js, endpoints.js
├── hooks/         useApi.js, useAuth.js, useDebounce.js
├── store/         index.js + slices/
├── routes/        AppRoutes.jsx, ProtectedRoute.jsx, PublicOnlyRoute.jsx
├── components/
│   ├── ui/        generic, app-agnostic pieces
│   ├── common/    domain pieces (ProductCard, PriceTag, CategoryTree…)
│   └── layout/    Navbar, Footer, MainLayout
├── pages/         home, auth, catalog, cart, wishlist, user
└── utils/         formatters.js
```

## Animations

Framer Motion for page transitions, staggered card entrances, layout animation
on cart/wishlist removal, and the toast stack. Tailwind keyframes handle
shimmer skeletons and the floating hero card. `prefers-reduced-motion` is
respected in `index.css` — everything collapses to near-instant for users who
asked the OS for less motion.

## Not built, and why

- **Admin screens** — the backend has no admin endpoints in this phase.
- **Brand filter** — `product_master` has no brand column.
- **Store pickup** — courier-only; there is no store table.
