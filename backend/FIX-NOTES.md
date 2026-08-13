# Startup errors — what was wrong and what I changed

Three separate problems. Files already fixed in this project.

---

## 1. FATAL — `The blank final field devAuthFilterProvider may not have been initialized`

**Cause:** Lombok was listed as a `<dependency>` in `pom.xml`, which only puts it
on the **classpath**. It was never registered as an **annotation processor** for
javac, so Maven compiled every class without generating Lombok's constructors,
getters and builders. `@RequiredArgsConstructor` produced nothing, so the
`final` field had no constructor to assign it.

Eclipse had Lombok configured (`.factorypath`, APT enabled) but Maven did not,
so IDE builds and command-line builds disagreed.

**Fixed — first attempt (superseded):**

- `pom.xml` configured `maven-compiler-plugin` with `annotationProcessorPaths`
  for Lombok, so `mvn` builds ran it.
- `SecurityConfig` wrote its constructor by hand and stopped importing Lombok.

**Fixed — final: Lombok removed from the project entirely.**

The Maven fix did not help builds started from STS. Lombok only works inside
Eclipse/STS when its agent is installed into `SpringToolSuite4.ini` — a
`.factorypath` entry and APT alone are not enough. STS therefore compiled every
Lombok class into an error stub (`java.lang.Error: Unresolved compilation
problem`) and overwrote whatever Maven had just produced in `target/classes`.
It also led to a quick-fix stub `public static ResponseEntity<ErrorResponse>
builder()` being generated into `ErrorResponse`, which then broke the Maven
build too.

Every `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`,
`@AllArgsConstructor`, `@RequiredArgsConstructor` and `@Slf4j` is now written
out by hand (getters/setters, constructors, static nested `Builder` classes,
`LoggerFactory.getLogger(...)`). The public API of every class is unchanged, so
mappers and services still call `X.builder()...build()` exactly as before.

The `lombok` dependency, the `annotationProcessorPaths` block, the
spring-boot-maven-plugin `<exclude>` and the `.factorypath` entry are all gone.
Builds are now identical in STS and on the command line. **Do not add Lombok
back** unless it is installed into the STS binary itself.

---

## 2. Every `Incorrect datetime value: '0000-00-00 00:00:00'` error

**Cause:** `spring.jpa.hibernate.ddl-auto=update`.

Hibernate was trying to **rewrite your tables** to match the entities:

```
alter table cart      add column created_at datetime(6) not null   <-- failed
alter table users     add column created_at datetime(6) not null   <-- failed
alter table users     modify column role enum ('CUSTOMER') not null <-- succeeded!
alter table category_master modify column flag bit not null         <-- succeeded!
```

Adding a `NOT NULL datetime` column to a table that already has rows makes MySQL
use the zero-date `'0000-00-00'`, which MySQL 9 rejects in strict mode.

The dangerous part is the statements that **succeeded**: your `role`, `flag` and
`redeem_points` columns were silently changed away from your teacher's design.

**Fixed:** `spring.jpa.hibernate.ddl-auto=none`.
The SQL scripts own the schema; Hibernate must never alter it.
Do not set this back to `update` or `create`.

---

## 3. Smaller issues fixed

| Was | Now | Why |
|---|---|---|
| `spring.jpa.properties.hibernate.dialect=...MySQLDialect` | removed | Hibernate warned it is unnecessary and auto-detects |
| `logging.level.com.emart` | `logging.level.com.example.demo` | wrong package — none of your own logs were appearing |
| `createDatabaseIfNotExist=true` | removed | it let a wrong/empty DB be created silently |
| no active profile | `spring.profiles.active=dev` | `DevAuthFilter` is `@Profile("dev")`; without it there is no way to authenticate yet |
| `jwt.refresh-expiration.ms` | removed | this project uses access tokens only |
| — | `spring.jpa.open-in-view=false` | stops accidental queries during JSON serialisation |

---

## HOW TO RUN  (read this — do not use Eclipse's Run button)

Lombok does **not** work in Eclipse from `.factorypath` alone. In Eclipse it
needs a **javaagent** patched into `eclipse.ini`, which is not installed here.
That is why you keep getting:

```
java.lang.Error: Unresolved compilation problem:
    The blank final field <x> may not have been initialized
```

`Unresolved compilation problem` is an **Eclipse** error string. Eclipse writes
its compile errors *into* the `.class` file and lets the app start anyway, so
the failure only appears at runtime, one bean at a time.

**Maven does not have this problem** — the `annotationProcessorPaths` block now
in `pom.xml` registers Lombok correctly for javac. So run through Maven:

```bat
cd D:\EMART-V1\backend
mvnw.cmd clean spring-boot:run
```

`clean` is required: `target/classes` still holds Eclipse's poisoned `.class`
files, and they survive a source fix.

### Important while using this approach

Eclipse's **Build Automatically** will keep recompiling into the same
`target/classes` and re-break it. Either:

- turn it off — *Project → uncheck Build Automatically*, or
- just ignore the red X marks in Eclipse and always run via `mvnw.cmd`.

The editor will still show errors on every `@Getter`/`@Builder` class. Those are
Eclipse's opinion, not real — Maven compiles the same code cleanly. If the red
marks bother you later, install the agent once with
`java -jar %USERPROFILE%\.m2\repository\org\projectlombok\lombok\1.18.42\lombok-1.18.42.jar`,
point it at your Eclipse install, restart, and Eclipse will agree with Maven.

### Database (still needs the reset)

`ddl-auto=update` already altered `role`, `flag` and `redeem_points`. Restore:

```bat
mysql -u root -p emart < emart_schema.sql
mysql -u root -p emart < emart_seed_data.sql
```

Or open both files in MySQL Workbench and run them in that order.

---

## Testing the endpoints

Public (no header needed):

```
GET http://localhost:8080/api/categories
GET http://localhost:8080/api/products
GET http://localhost:8080/api/products/1
```

Cart — needs the dev auth header until Module 2 exists:

```
GET  http://localhost:8080/api/cart
Header:  X-User-Id: 1

POST http://localhost:8080/api/cart/items
Header:  X-User-Id: 1
Body:    { "prodId": 1, "quantity": 2 }
```

User 1 is the seeded cardholder, so prices come back at `cardholder_price`.
User 2 is a normal member and gets `mrp_price`.

---

## Note on `mvn test`

`BackendApplicationTests.contextLoads()` is a `@SpringBootTest` — it boots the
whole app and needs MySQL running. The three module tests
(`CartServiceImplTest`, `CategoryServiceImplTest`, `ProductServiceImplTest`) are
pure Mockito and need no database.

Run only those with:

```bash
mvnw.cmd test -Dtest="*ServiceImplTest"
```
