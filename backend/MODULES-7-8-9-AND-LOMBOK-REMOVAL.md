# Modules 7, 8, 9 + Lombok removal

## 1. Lombok is gone

Removed from all 30 remaining files and from `pom.xml`. The project now has
**zero** Lombok anywhere, so it compiles in Eclipse, IntelliJ, VS Code or plain
`javac` with no agent, no `.factorypath`, and no annotation processor.

What replaced what:

| Lombok | Replacement |
|---|---|
| `@Getter` / `@Setter` | explicit getters and setters |
| `@NoArgsConstructor` / `@AllArgsConstructor` | explicit constructors |
| `@Builder` | hand-written static `Builder` inner class |
| `@Builder.Default` | field initialiser inside the `Builder` |
| `@RequiredArgsConstructor` | explicit constructor over the `final` fields |
| `@Slf4j` | `private static final Logger log = LoggerFactory.getLogger(X.class);` |

The builder API is byte-for-byte the same (`X.builder().field(v).build()`), so
no calling code or test needed changing.

`SecurityConfig` had already been converted by hand earlier — it is the first
bean Spring creates, so a Lombok failure there killed the whole app with a
confusing message.

**One thing to watch:** Eclipse may still show `@Builder` red marks from a
stale build. Run `mvnw.cmd clean` once and they go away.

---

## 2. Module 7 — Wishlist

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/wishlist` | The caller's saved products |
| POST | `/api/wishlist` | Save a product |
| DELETE | `/api/wishlist/{wishlistId}` | Remove one |

Flat table, one row per `(user, product)`. The pair is UNIQUE, so a duplicate
add returns **409** rather than a raw constraint violation. Ownership is checked
before delete.

---

## 3. Module 8 — Checkout & Orders

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/orders/checkout-preview` | Totals **without** saving |
| POST | `/api/orders` | Convert cart → order |
| GET | `/api/orders` | Order history (paginated) |
| GET | `/api/orders/{orderId}` | Order / invoice detail |
| PUT | `/api/orders/{orderId}/cancel` | Cancel |
| GET | `/api/orders/{orderId}/invoice-pdf` | Download the invoice PDF |

### How the money is calculated

```
unit price   = cardholder ? cardholder_price : mrp_price
subtotal     = sum(unit price x quantity)
tax          = subtotal x 5%                     (emart.order.tax-percentage)
pointsValue  = pointsRedeemed x 1.00             (emart.points.currency-value)
total        = subtotal + tax - pointsValue      (floored at 0)
pointsEarned = cardholder ? floor(total x 10%) : 0   (emart.points.earn-percentage)
```

Preview and place-order run the **same** method, so the numbers a customer is
shown are produced by the code that later persists them.

### Decisions worth knowing

**Preview saves nothing.** It returns `"preview": true` with `orderId` and
`orderNo` null.

**Product names are snapshotted** into `order_details.prod_name_snapshot`. A
two-year-old invoice must still show what the customer actually bought, even if
the catalog was renamed since.

**The cart row is reused, not replaced.** `cart.user_id` is UNIQUE, so a user
physically cannot hold a CONVERTED cart and a fresh ACTIVE one at once. Placing
an order empties the existing cart and leaves it ACTIVE. This is the option (a)
flagged back in the Module 6 notes.

**Points balance is checked twice** — at checkout and again at payment —
because the balance can change in between.

**Cancel is a server-side state machine.** Only `PLACED` + `PENDING` can be
cancelled; a PAID order is refused with a message pointing at refunds.

**The invoice is refused until the order is PAID**, and returns raw PDF bytes
rather than the JSON envelope so a browser can render it.

### Schema gap (unchanged, flagged)

The BRD's loyalty example is "$120, **or** $80 + 60 e-Points" — a per-product
cash+points price. `product_master` has no column for that reduced cash price,
so this implements a flat conversion (1 point = ₹1, configurable). Closing it
properly needs a `points_price` column.

---

## 4. Module 9 — Payment

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/payments/{orderId}/verify` | Mock card verification |
| GET | `/api/payments/{orderId}` | Every attempt, newest first |

**Mock rule: a card number ending in `0` is DECLINED**, anything else is
approved — so the failure path is testable without a real gateway.
`4242424242424242` succeeds, `4242424242424240` fails.

The gateway call is fake, but everything around it is real:

- **Amount must equal the order total exactly.** A mismatch means the client was
  tampered with, so it is refused.
- **Ownership** is checked before any payment is accepted.
- **Double payment** is blocked; a cancelled order cannot be paid.
- **Only the last 4 digits** of the card are stored. Never the full number, never the CVV.
- **Failed attempts are kept**, not overwritten, so retries are auditable.
- **On success, in one transaction:** order → PAID, points debited and credited,
  new balance returned.

---

## 5. Postman collection

`eMART-API.postman_collection.json` — 51 requests across 10 folders, covering
**all 39 endpoints** (verified programmatically against the controllers).

**Import → run `02 Auth > Login (cardholder)` first.** A test script saves the
JWT into the `accessToken` collection variable; every other request inherits it
via Bearer auth. `orderId`, `cartItemId`, `addressId` and `wishlistId` are
captured automatically too.

Suggested end-to-end run:

```
Login  ->  Add item to cart  ->  Checkout preview  ->  Place order
       ->  Verify payment (SUCCESS)  ->  Download invoice PDF
```

One manual step: **copy `totalAmount` from the Place-order response into the
payment request's `amount`.** That is deliberate — the amount check is what
catches a tampered client, so it cannot be auto-filled without defeating it.

Folder 10 contains negative tests: no token, bad token, another user's order.

---

## 6. Testing

10 Mockito test classes, no database needed:

```bat
mvnw.cmd clean test -Dtest="*ServiceImplTest"
```

New in this round: `WishlistServiceImplTest` (6), `OrderServiceImplTest` (13 —
pricing maths, snapshotting, cart emptying, cancel state machine, invoice
gating, ownership), `PaymentServiceImplTest` (12 — points settlement, decline
path, amount mismatch, double payment, balance re-check).

`OrderServiceImplTest` builds the service by hand instead of `@InjectMocks`,
because `OrderMapper` is a real object whose arithmetic is under test and
`@InjectMocks` would pass null for it.

---

## 7. New dependency

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>2.0.5</version>
</dependency>
```

OpenPDF is the maintained LGPL fork of iText 2 — no commercial licence, unlike
iText 5+/7. In the target architecture, PDF generation belongs to the separate
Notification service; `InvoicePdfGenerator` keeps the feature working until that
exists, and `OrderService` would not change if it moved.
