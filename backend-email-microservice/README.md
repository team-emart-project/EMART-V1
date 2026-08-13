# backend-email-microservice

Order confirmation + invoice emails for e-MART.

One job: turn a finished order into an email. It owns no data, has no database
connection, and knows nothing about carts, pricing or payments. Whichever
backend is running — `backend` (Java) or `dotnet-backend` (.NET) — posts the
completed order to one endpoint and this service does the rest.

Runs on **port 8082**, beside whichever backend holds 8080.

---

## Contents

- [Quick start](#quick-start)
- [The API](#the-api)
- [Calling it from `backend` (Java)](#calling-it-from-backend-java)
- [Calling it from `dotnet-backend` (.NET)](#calling-it-from-dotnet-backend-net)
- [Configuration](#configuration)
- [How it is decoupled](#how-it-is-decoupled)
- [Troubleshooting](#troubleshooting)

---

## Quick start

```bash
cd backend-email-microservice
mvnw spring-boot:run
```

It ships with `emart.email.dry-run=true`, so the first run needs **no
credentials and no mailbox**: the full message is printed to the console
instead of being sent. Check it is alive:

```bash
curl http://localhost:8082/actuator/health
```

Then send a test order:

```bash
curl -X POST http://localhost:8082/api/send-order-email \
  -H "Content-Type: application/json" \
  -H "X-API-Key: emart-local-dev-key" \
  -d '{
    "sourceSystem": "CURL",
    "eventType": "ORDER_PLACED",
    "customer": { "name": "Rishiraj Chhalotre", "email": "you@example.com",
                  "membershipNo": "EM-000123", "cardholder": true },
    "order": {
      "orderId": 41, "orderNo": "ORD-2026-048372",
      "orderDate": "2026-08-10T14:22:31",
      "orderStatus": "PLACED", "paymentStatus": "PENDING",
      "subtotalMrp": 4998.00, "subtotalAmount": 3998.00,
      "totalSavings": 1000.00, "totalAmount": 3998.00,
      "pointsRedeemed": 0, "pointsEarned": 119,
      "items": [ { "prodId": 7, "prodName": "Wireless Headphones", "quantity": 2,
                   "mrpPrice": 2499.00, "cardholderPrice": 1999.00,
                   "priceOption": "CARDHOLDER", "priceCharged": 1999.00,
                   "lineTotal": 3998.00, "lineSavings": 1000.00,
                   "pointsRedeemed": 0 } ],
      "shippingAddress": { "addressLine1": "12 MG Road", "city": "Indore",
                           "state": "Madhya Pradesh", "zipCode": "452001",
                           "country": "India" }
    }
  }'
```

The rendered invoice appears in the console. To send it for real, set
`emart.email.dry-run=false` and put a working Gmail **app password** in
`spring.mail.password`.

---

## The API

### `POST /api/send-order-email`

Header `X-API-Key: <shared secret>` is required.

**202 Accepted**

```json
{
  "success": true,
  "message": "Order email accepted for delivery",
  "data": {
    "requestId": "86a0e207",
    "orderNo": "ORD-2026-048372",
    "recipient": "r******j@example.com",
    "status": "ACCEPTED",
    "acceptedAt": "2026-08-10T14:08:12.87"
  },
  "timestamp": "2026-08-10T14:08:12.87"
}
```

**202, not 200, and `ACCEPTED`, not `SENT`.** The response is written before
SMTP is contacted — the HTTP thread validates the payload and hands the send to
a background pool. That is what stops a slow mail server from stalling a
customer's checkout. Whether delivery succeeded is reported in this service's
log, keyed by `requestId`:

```
[86a0e207] Sent 'ORDER_PLACED' email for orderNo=ORD-... to r***j@example.com on attempt 1/3
```

| Status | When |
|---|---|
| `202` | Accepted and queued (or `SKIPPED` when `emart.email.enabled=false`) |
| `400` | Payload failed validation — the `fieldErrors` map names the field |
| `401` | `X-API-Key` missing or wrong |
| `500` | Bug in this service |

Errors use the same envelope as both backends:

```json
{ "success": false, "status": 400, "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "path": "/api/send-order-email",
  "fieldErrors": { "order.items": "An order must have at least one item" },
  "timestamp": "..." }
```

### `GET /actuator/health`

Outside the API-key filter, so a health check does not need the secret.

---

## Calling it from `backend` (Java)

Already wired. Three pieces, none of which live in the order logic:

| File | Role |
|---|---|
| `client/dto/OrderEmailPayload.java` | The request body, built from `OrderResponse` |
| `client/EmailServiceClient.java` | `RestClient` POST — **never throws** |
| `listener/OrderPlacedEmailListener.java` | `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` |
| `config/EmailIntegrationConfig.java` | `RestClient` bean, timeouts, `@EnableAsync`, notification pool |

`OrderServiceImpl.placeOrder()` adds exactly one statement:

```java
eventPublisher.publishEvent(
        new OrderPlacedEvent(OrderEmailPayload.from(response, user.getEmail())));
```

That is a *publish*, not a call. The payload is built inside the transaction
(while the `User` is still attached — `spring.jpa.open-in-view=false`), but
`OrderPlacedEmailListener` does not run until the commit succeeds, and then on
its own thread. Checkout neither waits for the email nor fails because of it.

`application.properties`:

```properties
emart.email-service.enabled=true
emart.email-service.base-url=http://localhost:8082
emart.email-service.api-key=emart-local-dev-key
emart.email-service.connect-timeout-ms=3000
emart.email-service.read-timeout-ms=5000
```

---

## Calling it from `dotnet-backend` (.NET)

Already wired, and behaves identically from this service's side:

| File | Role |
|---|---|
| `DTOs/OrderEmailDto.cs` | The request body, built from `OrderDto` |
| `Services/EmailServiceClient.cs` | `HttpClient` POST — **never throws** |
| `Program.cs` | `AddHttpClient<IEmailServiceClient, EmailServiceClient>` |

`OrderService.PlaceOrderAsync()` adds one statement, after `CommitAsync()`:

```csharp
await _emailServiceClient.SendOrderPlacedAsync(
    OrderEmailPayload.From(dto, user.Email));
```

Awaited rather than fired and forgotten: a `Task.Run` here would outlive the
request scope and reach for an `HttpClient` already disposed with it. The cost
is bounded — the endpoint returns 202 without sending anything, and the client
caps the call at `TimeoutSeconds` and swallows every failure.

`appsettings.json`:

```jsonc
"EmailService": {
  "Enabled": true,
  "BaseUrl": "http://localhost:8082",
  "ApiKey": "emart-local-dev-key",
  "TimeoutSeconds": 5
}
```

Two details that are easy to get wrong and that `Emart.Tests/Services/OrderEmailPayloadTests.cs`
now guards:

- **camelCase.** The JSON options in `Program.cs` apply to MVC *responses*
  only. `EmailServiceClient` sets its own `JsonNamingPolicy.CamelCase`;
  without it every key arrives PascalCase and Jackson reads a payload of nulls.
- **`orderDate` is formatted by hand** as `yyyy-MM-ddTHH:mm:ss`. A `DateTime`
  with `Kind=Local` serialises with a `+05:30` offset, which
  `java.time.LocalDateTime` refuses to parse.

---

## Configuration

Everything under `emart.email.*` in `src/main/resources/application.properties`:

| Property | Default | Meaning |
|---|---|---|
| `emart.email.enabled` | `true` | `false` → endpoint still returns 2xx, nothing is sent (`SKIPPED`) |
| `emart.email.dry-run` | `true` | Render and **log** the message instead of sending it |
| `emart.email.api-key` | `emart-local-dev-key` | Shared secret; blank disables the check |
| `emart.email.from` / `.from-name` | Gmail account / `e-MART` | The `From:` header |
| `emart.email.subject-prefix` | `[e-MART]` | Prefixed to every subject |
| `emart.email.support-email` | Gmail account | `Reply-To`, and printed in the footer |
| `emart.email.store-name` | `e-MART` | Invoice header |
| `emart.email.currency-symbol` | `₹` (set in Java) | Prefixed to every amount |
| `emart.email.max-attempts` | `3` | SMTP attempts before giving up |
| `emart.email.retry-delay-ms` | `2000` | Pause between attempts |

`spring.mail.*` (host, port, username, password) all read environment
variables first: `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`.
The API key reads `EMAIL_SERVICE_API_KEY`.

> **Gmail:** `spring.mail.password` must be an **app password**
> (<https://myaccount.google.com/apppasswords>, 2-Step Verification required).
> A normal account password is rejected.

**The API key must match in all three places** or every notification comes back
401:

| Where | Property |
|---|---|
| this service | `emart.email.api-key` |
| `backend` | `emart.email-service.api-key` |
| `dotnet-backend` | `EmailService:ApiKey` |

---

## How it is decoupled

The brief asked for a service that only sends email and never touches order
logic. That is enforced by what is *absent*, not by convention:

- **No database.** The Initializr scaffold's `spring-boot-starter-data-jpa` and
  `mysql-connector-j` were removed. This service cannot look an order up even
  if someone wanted it to, so it can never drift into re-deriving a total.
- **No order maths.** Every figure on the invoice arrives pre-computed. The
  same numbers the shopper saw at checkout are the numbers in the inbox,
  because they are literally the same numbers.
- **No enums shared with the backends.** `priceOption` travels as a plain
  string, so `PriceOption` can gain a value in either backend without a
  coordinated release here.
- **One endpoint, one payload.** Deleting `EmailServiceClient` and the listener
  from a backend turns the feature off without touching a line of order logic.

The email is a *notification about* an order, never part of placing one. Both
clients swallow every failure: by the time they run the order is committed, and
a checkout that appears to have failed is far worse than a missing email.

---

## Troubleshooting

| Symptom | Cause |
|---|---|
| Backend logs `Email service did not accept orderNo=...` | Service not running, or wrong `base-url` |
| `401 Unauthorized` in the response body | The three API keys do not match |
| `400` with `fieldErrors` | The payload is genuinely wrong — the map names the field |
| Nothing in the inbox, log says `Sent` | Check spam; Gmail also silently drops mail whose `From:` is not the authenticated account |
| `Attempt 1/3 failed ... Authentication failed` | `spring.mail.password` is an account password, not an app password |
| Console shows the whole email | `emart.email.dry-run=true` — that is the default |

To watch the raw SMTP conversation, uncomment the last two lines of
`application.properties`.
