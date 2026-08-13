# e-Points fix + seed data

## 1. The e-Points bug — root cause

**Symptom:** `emart_card.points_balance` never changed after an order, and
redeeming points always failed with "you only have 0".

**Root cause:** `CardStatus.APPROVED` was only ever **read**, never **written**,
and `setIsCardholder(true)` was never called anywhere in the codebase.

```
EmartCardServiceImpl.apply()   ->  status = PENDING,  is_cardholder = false
PaymentServiceImpl.settlePoints()  ->  `if (status != APPROVED) return null;`   <-- always taken
OrderServiceImpl.currentPointsBalance() -> `.filter(status == APPROVED)` -> 0   <-- always 0
CartServiceImpl pricing        ->  `user.getIsCardholder()`  ->  false
```

Because there is no admin module in this phase, nothing ever approved a card.
Every application sat at `PENDING` forever, so **every** downstream points check
silently no-opped. The only account that worked was the one I hand-wrote as
`'APPROVED'` in the seed SQL — which is exactly why it looked like it worked
sometimes.

There was a second, quieter defect underneath it: the schema stores
"is this person a cardholder" in **two** places —
`users.is_cardholder` and `emart_card.status` — and nothing kept them in sync,
so even setting one would have left the other stale.

### The fix

New class **`service/CardholderService.java`** is now the single source of truth.
Every read and write of cardholder status and points goes through it:

| Method | Purpose |
|---|---|
| `isActiveCardholder(userId)` | true only for an **APPROVED** card |
| `isCurrentUserCardholder()` | same, but safe on public endpoints (signed-out ⇒ false) |
| `getPointsBalance(userId)` | 0 for non-cardholders — never null, never throws |
| `approve(card, date)` | sets `status=APPROVED` **and** `users.is_cardholder=true` in one transaction |
| `adjustPoints(card, redeemed, earned)` | the debit + credit, persisted |

`EmartCardServiceImpl.apply()` now calls `approve()` immediately
(`emart.card.auto-approve=true`). Turn that off only once a real admin approval
workflow exists — otherwise you reintroduce the bug.

`CartServiceImpl`, `OrderServiceImpl` and `PaymentServiceImpl` all now derive
cardholder status and balances from `CardholderService` instead of reading
`users.is_cardholder` directly, so the two columns can no longer drift.

---

## 2. e-Points earning — tiered 2%–5%

Only **active cardholders** earn. Points are always rounded **down**.

| Order total | Rate |
|---|---|
| under ₹5,000 | **2%** |
| ₹5,000 – ₹24,999 | **3%** |
| ₹25,000 – ₹74,999 | **4%** |
| ₹75,000 and above | **5%** |

Implemented in `OrderServiceImpl.calculatePointsEarned(BigDecimal)`, configurable:

```properties
emart.points.earn.tier1-limit=5000
emart.points.earn.tier2-limit=25000
emart.points.earn.tier3-limit=75000
emart.points.earn.tier1-percentage=2
emart.points.earn.tier2-percentage=3
emart.points.earn.tier3-percentage=4
emart.points.earn.tier4-percentage=5
```

Every boundary is covered by `OrderServiceImplTest.earnTiersAcrossBoundaries()`.

---

## 3. Price visibility

> Always show `mrp_price`. Show `cardholder_price` **only** to cardholders, and
> charge `cardholder_price` when a cardholder adds to the cart.

Enforced in **two layers**:

**Backend** — `ProductMapper.toSummary(product, isCardholder)` nulls out both
`cardholderPrice` and `cardholderSaving` for non-cardholders. `ProductResponse`
is annotated `@JsonInclude(NON_NULL)`, so those fields are **absent from the
JSON entirely** — a non-cardholder cannot see the member price even in devtools.
Applied to product search, product detail, home new-arrivals and wishlist.

The catalog endpoints are public, so this needed
`SecurityUtils.getCurrentUserIdOrNull()` — returns null instead of throwing for
a signed-out visitor.

**Frontend** — `components/common/PriceTag.jsx`:

- non-cardholder → MRP only, nothing else rendered
- cardholder → member price as the headline, MRP struck through, "% member price" badge

The old "₹X with e-MART card" teaser shown to non-cardholders has been removed —
that violated the requirement.

**Which price is charged** is decided server-side in `CartServiceImpl`, never by
the client.

---

## 4. "Redeem e-Points on this item" checkbox

`components/common/RedeemPointsBox.jsx`, backed by a new endpoint:

```
GET /api/emart-card/balance   ->  { cardholder, pointsBalance, cardStatus }
```

Unlike `/api/emart-card/me` this **never 404s** — a non-cardholder gets
`cardholder:false, pointsBalance:0`, so the UI does not have to treat "no card"
as an error.

Behaviour when the box is ticked:

1. Re-reads the **live** balance (not a value cached at page load — the user may
   have spent points in another tab).
2. Enough points → redemption is applied, with a slider capped at
   `min(points_to_redeem × quantity, balance)`.
3. Not enough → modal: **"You don't have enough e-Points to redeem."** showing
   what is needed vs what they have, and the checkbox **stays unchecked**.

The box hides itself entirely for non-cardholders and for products with
`points_to_redeem = 0`.

**The UI is convenience, not the security boundary.** The server re-validates the
balance three times: on add-to-cart (`CartServiceImpl.validatePoints`), at
checkout (`OrderServiceImpl.buildOrder`) and again at payment
(`PaymentServiceImpl.settlePoints`) — because the balance can change between
each step.

---

## 5. Seed data

`emart_seed_data.sql` — regenerated.

| | |
|---|---|
| Categories | **42** (10 roots → sub-categories, up to 3 levels) |
| Products | **145** |
| Users | **10** |

Categories: Electronics, Home Appliances, Groceries, Apparel, Home Essentials,
Books & Media, Sports & Fitness, Beauty & Health, Toys & Baby, Furniture.

### Test accounts — password is `Password@123` for all

| Email | Card | e-Points | Notes |
|---|---|---|---|
| rishi.chhalotre@example.com | APPROVED | 3,500 | member pricing |
| karan.mehta@example.com | APPROVED | 1,200 | member pricing |
| vikram.singh@example.com | APPROVED | 8,750 | plenty to redeem |
| arjun.reddy@example.com | APPROVED | **250** | good for testing the "not enough points" alert |
| rahul.verma@example.com | APPROVED | 15,000 | |
| neha.kulkarni@example.com | PENDING | 0 | applied, not approved → **no** member pricing |
| divya.menon@example.com | REJECTED | 0 | → no member pricing |
| ananya.sharma@example.com | none | — | plain member, MRP only |
| sneha.iyer@example.com | none | — | plain member |
| priya.nair@example.com | none | — | `is_active = 0`, login refused |

`arjun.reddy@example.com` (250 points) is the useful one for demoing the
insufficient-points alert against an expensive item.

---

## Apply it

```bat
cd D:\EMART-V1\backend
mysql -u root -p emart < emart_schema.sql
mysql -u root -p emart < emart_seed_data.sql
mvnw.cmd clean spring-boot:run
```

### Verify the fix end-to-end

1. Log in as `ananya.sharma@example.com` → products show **MRP only**.
2. Apply for a card at *Account → e-MART card* → it is **APPROVED immediately**.
3. Reload a product → the **member price** now appears.
4. Add to cart → checkout → pay → the order shows points earned, and
   *Account → e-MART card* shows the **new balance**.
5. Log in as `arjun.reddy@example.com` (250 pts), open an expensive item, tick
   "Redeem e-Points" → the **not-enough-points** modal appears and the box
   stays unchecked.
