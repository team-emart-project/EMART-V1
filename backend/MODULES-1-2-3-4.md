# Modules 1, 2, 3, 4

| Module | Name | Auth | Tables |
|---|---|---|---|
| 1 | Home Page | public | category_master, product_master (read) |
| 2 | Authentication & Security | public | users |
| 3 | User Profile & Address | JWT | users, address |
| 4 | e-MART Card & Loyalty | JWT | emart_card |

---

## Endpoints

### Module 2 — Authentication (public)
```
POST   /api/auth/register          create an account, emails the membership number
POST   /api/auth/login             returns a JWT access token
POST   /api/auth/logout            clears the context (client must discard the token)
POST   /api/auth/forgot-password   issues a 30-minute reset token
POST   /api/auth/reset-password    consumes the token, sets a new password
```

### Module 1 — Home (public)
```
GET    /api/home/featured-categories
GET    /api/home/new-arrivals?limit=8
```

### Module 3 — Profile & Addresses (JWT required)
```
GET    /api/users/me
PUT    /api/users/me
GET    /api/users/me/addresses
POST   /api/users/me/addresses
PUT    /api/users/me/addresses/{addressId}
DELETE /api/users/me/addresses/{addressId}
PUT    /api/users/me/addresses/{addressId}/default
```

### Module 4 — e-MART Card (JWT required)
```
POST   /api/emart-card/apply
GET    /api/emart-card/me
```

---

## IMPORTANT: DevAuthFilter has been deleted

Module 2 replaced it. The `X-User-Id` header no longer does anything, and
`spring.profiles.active=dev` has been removed from `application.properties`.

Authentication is now a real JWT:

```
1. POST /api/auth/login   { "email": "...", "password": "..." }
2. copy accessToken from the response
3. send it on every protected call:  Authorization: Bearer <token>
```

### Try it with the seeded users

Every seeded user's password is `Password@123` — I verified the BCrypt hashes in
`emart_seed_data.sql` actually validate against it, so these work immediately:

| Email | Notes |
|---|---|
| `rishi.chhalotre@example.com` | approved cardholder, 350 points → gets cardholder pricing |
| `ananya.sharma@example.com`   | normal member → gets MRP pricing |
| `priya.nair@example.com`      | `is_active = 0` → login is refused |

---

## Design decisions worth knowing

**Access token only.** There is no refresh token, no `/refresh-token` endpoint,
no token table. When the token expires (2 hours) the user logs in again.

**Logout is client-side.** A stateless JWT stays valid until it expires — the
server has nothing to revoke. Real revocation needs a blacklist table, which
this project deliberately does not have. The endpoint exists so the frontend
has something to call.

**Login failures are indistinguishable.** "No such email" and "wrong password"
return the *same* message. A different message would let someone discover which
emails are registered. There is a test asserting the two messages are equal.

**Forgot-password always returns 200**, even for an unknown email — same reason.
The token is logged to the console (see below), not emailed.

**Emails are logged, not sent.** `EmailUtil` prints to the console instead of
sending. The Notification/PDF microservice does not exist yet, and real SMTP in
a college project means committing credentials. To test a password reset, copy
the token straight out of the console log.

**Applying for a card does NOT make you a cardholder.** `apply` writes a row
with `status = PENDING` and leaves `users.is_cardholder = false`. Cardholder
pricing in the cart keys off that flag, so applying cannot give anyone a
discount. Approval is an admin action, and admin is out of scope for this phase
— so a card stays PENDING for now. There is a test asserting this.

**PAN is never returned and the bank account is masked.** `EmartCardResponse`
has no `panNumber` field at all, and shows `********9012`. This is exactly the
reason we never return entities directly.

**Profile update uses an allow-list.** `UpdateProfileRequest` has no email,
password, role, isCardholder or membershipNo field. A field that does not exist
on the DTO cannot be mass-assigned by a crafted request body.

**Address rules.** The first address saved becomes the default automatically
(otherwise checkout has nothing pre-selected). Promoting a new default demotes
the old one in the same transaction. Deleting the default promotes the next
address rather than leaving the user with addresses but no default.

**Home page "featured" = `category_master.flag = 1`.** That column is the
teacher's own marker for a row that jumps straight to a product, which is what a
home-page tile does. `limit` on new-arrivals is clamped to 1..50 rather than
rejected, so `?limit=999999` cannot pull the whole catalog.

---

## Not implemented, and why

- **Promotional banners / sponsor adverts.** The BRD describes these on the home
  page. There is no table for them in the schema, so Module 1 only serves
  featured categories and new arrivals. Adding them needs a `banner` table.
- **Card approve/reject.** Admin functionality, out of scope for this phase.
- **Real email delivery.** Waiting on the Notification service.

---

## Testing

Five Mockito test classes, no database required:

| Class | Covers |
|---|---|
| `AuthServiceImplTest` | password hashing, email normalisation, duplicate email, identical failure messages, inactive user, reset token issue/expiry/replay |
| `AddressServiceImplTest` | first-address-is-default, demotion, ownership on update/delete, promotion after deleting the default |
| `EmartCardServiceImplTest` | PENDING start, `is_cardholder` untouched, duplicate application, PAN uppercasing, masking |
| `HomeServiceImplTest` | flag filtering, limit clamping both directions, pricing |
| `CartServiceImplTest`, `CategoryServiceImplTest`, `ProductServiceImplTest` | Modules 5 and 6 |

```bat
mvnw.cmd clean test -Dtest="*ServiceImplTest"
```

`BackendApplicationTests.contextLoads()` is excluded by that filter — it is a
`@SpringBootTest` and needs MySQL running.

---

## Run

```bat
cd D:\EMART-V1\backend
mvnw.cmd clean spring-boot:run
```

Still run through Maven, not Eclipse's Run button — see `FIX-NOTES.md`.

---

## Interview questions

**Q: Why no refresh token?**
It was an explicit project requirement. The trade-off: a short-lived access
token with no refresh means the user re-logs in when it expires. A refresh token
buys longer sessions but needs server-side storage and revocation logic.

**Q: Where is the user identity stored between requests?**
Nowhere on the server. The JWT carries the user id in its `sub` claim and the
client sends it on every request. That is what "stateless" means, and it is why
`SessionCreationPolicy.STATELESS` and disabled CSRF go together.

**Q: Is a JWT encrypted?**
No — it is *signed*. Anyone can base64-decode and read the payload. The
signature only proves it was not tampered with. Never put a secret in a JWT.

**Q: Why does JwtAuthFilter never reject a request?**
Because public endpoints must still work. The filter authenticates when it can
and stays silent otherwise; `SecurityConfig`'s rules decide whether being
unauthenticated is actually a problem.

**Q: Why extend OncePerRequestFilter?**
It guarantees the filter runs exactly once per request, even across forwards
and includes.

**Q: Why is BCrypt preferred over SHA-256 for passwords?**
SHA-256 is fast, which is the wrong property for password hashing — it makes
brute force cheap. BCrypt is deliberately slow and has a tunable work factor,
and it salts every hash so identical passwords store differently.

**Q: What is mass assignment and how does this code prevent it?**
Binding a request body straight onto an entity, letting a caller set fields you
never intended (like `role`). Prevented by using a request DTO that simply has
no such fields, and copying across field by field.

**Q: Why 409 for a duplicate email rather than 400?**
400 means the request was malformed. This request is perfectly well-formed; it
just conflicts with existing state. 409 Conflict says that precisely.
