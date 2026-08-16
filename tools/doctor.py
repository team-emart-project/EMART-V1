"""
e-MART doctor — run this ON YOUR MACHINE to find what is actually broken.

    python tools\\doctor.py

Checks your toolchain, your database and your installed dependencies, then
prints an ordered fix list. Read-only: it changes nothing.
"""
import os, re, subprocess, sys, shutil, json

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BE, FE = os.path.join(ROOT, "backend"), os.path.join(ROOT, "frontend")
ok, warn, bad = [], [], []
def OK(m):   ok.append(m);   print(f"  [ OK ]  {m}")
def WARN(m): warn.append(m); print(f"  [WARN]  {m}")
def BAD(m):  bad.append(m);  print(f"  [FAIL]  {m}")

def run(cmd):
    try:
        p = subprocess.run(cmd, shell=True, capture_output=True, text=True, timeout=60)
        return p.returncode, (p.stdout or "") + (p.stderr or "")
    except Exception as e:
        return 1, str(e)

print("=" * 66); print(" 1. TOOLCHAIN"); print("=" * 66)

rc, out = run("java -version")
m = re.search(r'version "(\d+)', out)
if rc or not m:
    BAD("Java not found on PATH. Install JDK 21 and reopen the terminal.")
else:
    major = int(m.group(1))
    if major >= 17: OK(f"Java {major} (Spring Boot 4 needs 17+)")
    else: BAD(f"Java {major} is too old. Spring Boot 4 needs 17+. Install JDK 21.")

mvnw = os.path.join(BE, "mvnw.cmd" if os.name == "nt" else "mvnw")
OK("Maven wrapper present") if os.path.isfile(mvnw) else BAD("mvnw missing from backend/")

rc, out = run("node -v")
OK(f"Node {out.strip()}") if rc == 0 else BAD("Node.js not found. Install Node 18+.")

mysql_cli = shutil.which("mysql")
OK(f"mysql client: {mysql_cli}") if mysql_cli else \
    WARN("mysql client not on PATH — use MySQL Workbench to run the .sql files")

print(); print("=" * 66); print(" 2. CONFIG"); print("=" * 66)

props = os.path.join(BE, "src/main/resources/application.properties")
url = user = pwd = None
if not os.path.isfile(props):
    BAD("application.properties missing")
else:
    txt = open(props).read()
    g = lambda k: (re.search(rf'^{re.escape(k)}=(.*)$', txt, re.M) or [None, None])[1]
    url, user, pwd = g("spring.datasource.url"), g("spring.datasource.username"), g("spring.datasource.password")
    OK(f"datasource url: {url}")
    if url and "allowPublicKeyRetrieval=true" in url:
        OK("allowPublicKeyRetrieval=true present (needed by MySQL 8/9 auth)")
    else:
        BAD("JDBC url is missing allowPublicKeyRetrieval=true -> 'Public Key Retrieval is not allowed'")
    ddl = g("spring.jpa.hibernate.ddl-auto")
    OK("ddl-auto=none (SQL scripts own the schema)") if ddl == "none" else \
        BAD(f"ddl-auto={ddl} — set it to none, or Hibernate will rewrite your tables")

print(); print("=" * 66); print(" 3. DATABASE"); print("=" * 66)

TABLES = ["users","address","emart_card","category_master","product_master","product_image",
          "config_master","prod_dtl_master","cart","cart_items","wishlist","orders",
          "order_details","payment"]
COLS = {"product_master": ["brand","stock_quantity","rating","rating_count","discount_percentage",
                           # the three e-MART card offers
                           "cardholder_price","points_price","hybrid_cash_price","hybrid_points"],
        "users": ["created_at","updated_at","reset_password_token"],
        "cart": ["created_at"],
        "cart_items": ["added_at","price_option"],
        "order_details": ["price_option"]}

# Columns that must NOT exist any more. A leftover here means the database is
# still on the old schema even though every table "looks" present, which is
# exactly the drift that produced the Unknown-column crashes before.
GONE = {"orders": ["tax_amount"],
        "product_master": ["points_to_redeem"],
        "cart_items": ["redeem_points"]}

def q(sql):
    if not mysql_cli or not url: return None
    db = url.split("?")[0].rsplit("/", 1)[-1]
    pw = f'-p"{pwd}"' if pwd else ""
    rc, out = run(f'mysql -u {user} {pw} -N -B -e "{sql}" {db}')
    return None if rc else out.strip()

if not mysql_cli:
    WARN("Skipped — no mysql client. Run the SQL in Workbench, then re-run this.")
else:
    res = q("SELECT 1")
    if res is None:
        BAD("Cannot connect to MySQL with the credentials in application.properties")
    else:
        OK("Connected to MySQL")
        have = (q("SHOW TABLES") or "").split()
        missing = [t for t in TABLES if t not in have]
        if missing: BAD(f"Missing tables: {missing}  -> run emart_schema.sql")
        else: OK(f"All {len(TABLES)} tables present")

        for tbl, cols in COLS.items():
            if tbl in have:
                cur = (q(f"SHOW COLUMNS FROM {tbl}") or "").split("\n")
                names = [c.split("\t")[0] for c in cur if c]
                miss = [c for c in cols if c not in names]
                if miss: BAD(f"{tbl} is missing columns {miss}  -> re-run emart_schema.sql")
                else: OK(f"{tbl}: all expected columns present")

        for tbl, cols in GONE.items():
            if tbl in have:
                cur = (q(f"SHOW COLUMNS FROM {tbl}") or "").split("\n")
                names = [c.split("\t")[0] for c in cur if c]
                stale = [c for c in cols if c in names]
                if stale:
                    BAD(f"{tbl} still has dropped columns {stale} -> your database is on the "
                        f"OLD schema. Re-run emart_schema.sql then emart_seed_data.sql.")

        if "product_master" in have:
            n = q("SELECT COUNT(*) FROM product_master")
            i = q("SELECT COUNT(*) FROM product_image") if "product_image" in have else "0"
            c = q("SELECT COUNT(*) FROM category_master")
            if n and int(n) >= 200: OK(f"Seed loaded: {c} categories, {n} products, {i} images")
            elif n and int(n) > 0:  BAD(f"Only {n} products — OLD seed. Re-run emart_seed_data.sql")
            else: BAD("No products. Run emart_seed_data.sql")

print(); print("=" * 66); print(" 4. FRONTEND"); print("=" * 66)

nm = os.path.join(FE, "node_modules")
if not os.path.isdir(nm):
    BAD("node_modules missing — run: cd frontend && npm install")
else:
    pkg = json.load(open(os.path.join(FE, "package.json")))
    miss = [d for d in pkg["dependencies"] if not os.path.isdir(os.path.join(nm, *d.split("/")))]
    miss += [d for d in pkg["devDependencies"] if not os.path.isdir(os.path.join(nm, *d.split("/")))]
    if miss: BAD(f"Dependencies not installed: {miss}  -> cd frontend && npm install")
    else: OK(f"All {len(pkg['dependencies'])+len(pkg['devDependencies'])} npm packages installed")

for sub, label in [("products", "product"), ("categories", "category"), ("banners", "banner")]:
    d = os.path.join(FE, "public/images", sub)
    n = len(os.listdir(d)) if os.path.isdir(d) else 0
    OK(f"{n} {label} images on disk") if n else BAD(f"No {label} images in public/images/{sub}")

print(); print("=" * 66)
print(f" RESULT: {len(ok)} ok · {len(warn)} warnings · {len(bad)} failures")
print("=" * 66)
if bad:
    print("\nFIX IN THIS ORDER:\n")
    for i, b in enumerate(bad, 1): print(f"  {i}. {b}")
    print("""
Usual sequence:
    cd backend
    mysql -u root -p emart < emart_schema.sql
    mysql -u root -p emart < emart_seed_data.sql
    mvnw.cmd clean spring-boot:run

    cd frontend
    npm install
    npm run dev""")
else:
    print("\nEverything checks out. Start with:")
    print("    backend :  cd backend  && mvnw.cmd clean spring-boot:run")
    print("    frontend:  cd frontend && npm run dev        ->  http://localhost:5173")
sys.exit(1 if bad else 0)
