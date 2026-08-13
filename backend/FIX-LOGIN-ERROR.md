# Login fails: `Unknown column 'u1_0.created_at' in 'field list'`

## What is actually wrong

Your **database does not match `emart_schema.sql`**. The Java code is fine.

Your `users` table has no `created_at` column, so the moment login runs
`findByEmail(...)`, MySQL rejects the SELECT.

### How it got that way

Your `emart` database was never built from `emart_schema.sql`. It was created
some other way, and the old `spring.jpa.hibernate.ddl-auto=update` setting then
tried to patch it to match the entities. Look at your earlier startup log —
three of those ALTERs **failed**:

```
alter table cart       add column created_at datetime(6) not null   <-- FAILED
alter table cart_items add column added_at   datetime(6) not null   <-- FAILED
alter table users      add column created_at datetime(6) not null   <-- FAILED
```

They failed because MySQL 9 refuses to add a `NOT NULL` datetime to a table
that already has rows — it would have to invent the illegal value
`'0000-00-00 00:00:00'` for them. Hibernate emitted no DEFAULT, so it lost.

Everything else got patched, which is why the app starts and the catalog works
— the failure only surfaces on the first query that touches `users`.

---

## Fix A — clean reset (recommended)

Your schema is in an unknown, half-patched state, and the Module 7/8/9 tables
(`wishlist`, `orders`, `order_details`, `payment`) plus `address` and
`emart_card` may not exist at all — they were added after that `update` run,
and `ddl-auto` is now correctly `none`, so nothing will create them.

```bat
cd D:\EMART-V1\backend
mysql -u root -p emart < emart_schema.sql
mysql -u root -p emart < emart_seed_data.sql
```

Both scripts are in this folder. `emart_schema.sql` drops and recreates all 13
tables, so you get a guaranteed-correct database. You lose any test data you
created — the seed script puts back the demo users, catalog and a sample order.

If `mysql` is not on your PATH, open both files in MySQL Workbench and run them
in that order.

---

## Fix B — keep your data

If you have registered users you do not want to lose, run
**`fix_schema_drift.sql`** instead. It:

1. Reports which tables and columns are missing (run this part first).
2. Adds the three columns **with `DEFAULT CURRENT_TIMESTAMP`**, which is the bit
   Hibernate got wrong — existing rows get a real timestamp instead of the
   illegal zero date.
3. Reverts `users.role` from the `enum` Hibernate forced on it back to the
   designed `VARCHAR(20)`.
4. Verifies the result.

**If step 1 reports any missing TABLE, use Fix A instead** — Fix B only repairs
columns, it does not create the Module 7/8/9 tables.

---

## After either fix

```bat
mvnw.cmd clean spring-boot:run
```

Then log in with `rishi.chhalotre@example.com` / `Password@123`.

---

## Why this will not happen again

`application.properties` already has:

```properties
spring.jpa.hibernate.ddl-auto=none
```

Keep it there. The SQL scripts own the schema; Hibernate must never alter it.
Setting this back to `update` is what caused the drift in the first place.

If you want Hibernate to *check* the schema without changing it, use
`validate` — it fails fast at startup with a clear message listing any
mismatch, instead of a confusing SQL error on the first login.
