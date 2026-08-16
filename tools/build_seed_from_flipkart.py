"""
Builds emart_seed_data_flipkart.sql from the Flipkart sample CSV.

WHAT IT PRODUCES
----------------
A drop-in replacement for the CATALOGUE half of the seed:
    category_master, product_master, product_image,
    config_master, prod_dtl_master, and a fresh wishlist

It deliberately does NOT touch users, address, emart_card or cart, so the
accounts already in the database survive. cart_items and wishlist ARE cleared,
because both hold foreign keys to prod_id and would dangle otherwise.

PRICE RULE
----------
Every price is a clean round rupee value - no 299, no 234.23. Rounded to the
nearest 100 below 10,000 and the nearest 500 above, so a 32,157 sofa becomes
32,000 rather than 32,200.

IMAGE URLS
----------
The CSV carries 2016-era hostnames (img5a/img6a.flixcart.com) which are long
retired. IMAGE_STYLE rewrites them to Flipkart's current CDN. Run
verify_images.py on YOUR machine to confirm which style actually loads, since
this sandbox cannot fetch binary content.
"""
import csv
import json
import random
import re
import sys
from collections import defaultdict, Counter
from pathlib import Path

CSV_PATH = Path(sys.argv[1]) if len(sys.argv) > 1 else None
OUT = Path(__file__).resolve().parent.parent / "backend" / "emart_seed_data_flipkart.sql"

# 'modern'  -> https://rukminim2.flixcart.com/image/416/416/<path>   (current CDN)
# 'original'-> the 2016 URL exactly as the CSV has it                (likely dead)
IMAGE_STYLE = "modern"

MAX_ROOTS = 20            # top-level categories
MAX_SUBS_PER_ROOT = 5     # leaf categories under each root
MAX_PRODUCTS_PER_SUB = 9
MIN_ROOT_PRODUCTS = 25
MIN_SUB_PRODUCTS = 6
IMAGES_PER_PRODUCT = (4, 6)

# Points offers are only worth seeding where a seeded user can afford them.
# The richest test account holds 15,000 e-Points.
POINTS_ONLY_MAX = 3_000
HYBRID_MAX = 10_000

random.seed(20260803)      # deterministic: re-running gives the same catalogue


# ----------------------------------------------------------------- helpers

def sql_str(value):
    """Quotes a value for SQL, or emits NULL. Escapes by doubling quotes."""
    if value is None:
        return "NULL"
    text = str(value).replace("\\", "").replace("'", "''")
    text = re.sub(r"\s+", " ", text).strip()
    return f"'{text}'" if text else "NULL"


def round_price(value):
    """Clean rupee values only: nearest 100, or nearest 500 above 10k."""
    value = float(value)
    if value >= 10_000:
        rounded = round(value / 500) * 500
    else:
        rounded = round(value / 100) * 100
    return max(100, int(rounded))


def code_of(name, taken):
    """A unique CHAR(3) code, because the schema stores the tree that way."""
    letters = re.sub(r"[^A-Za-z]", "", name).upper()
    for base in (letters[:3],
                 (letters[:1] + letters[1:].translate(str.maketrans("", "", "AEIOU")))[:3],
                 letters[::2][:3]):
        if len(base) == 3 and base not in taken:
            taken.add(base)
            return base
    stem = (letters + "XXX")[:2]
    for i in list("0123456789") + list("ABCDEFGHIJKLMNOPQRSTUVWXYZ"):
        candidate = (stem + i)[:3]
        if candidate not in taken:
            taken.add(candidate)
            return candidate
    raise RuntimeError("ran out of 3-character category codes")


def rewrite_image(url):
    if IMAGE_STYLE == "original":
        return url
    # http://img5a.flixcart.com/image/<path>  ->  current CDN, 416px
    m = re.match(r"https?://img\d+[a-z]?\.flixcart\.com/image/(.+)$", url)
    if m:
        return f"https://rukminim2.flixcart.com/image/416/416/{m.group(1)}"
    return url.replace("http://", "https://")


def clean_text(text, limit):
    if not text:
        return None
    text = re.sub(r"\s+", " ", str(text)).strip()
    text = re.sub(r"\b(Price: Rs\.?\s*[\d,]+)", "", text)      # stale prices
    text = re.sub(r"^(Key Features of|Specifications of)\s*", "", text)
    return text[:limit].strip() or None


def clean_description(text, product_name):
    """
    Turns the CSV's run-on blob into something readable.

    The raw field concatenates marketing copy with a full spec dump
    ("...Specifications of X Shorts Details Number of Contents..."). Cutting at
    that boundary leaves the human-written half, which is what a product page
    actually wants to show.
    """
    text = clean_text(text, 2000)
    if not text:
        return None
    text = re.split(r"\bSpecifications of\b", text)[0].strip()
    if product_name and text.startswith(product_name):
        text = text[len(product_name):].strip(" ,-")
    text = re.sub(r"^(Key Features of\s*)+", "", text).strip()
    if len(text) < 25:
        return None
    return text[:800].strip()


def dedupe_key(name):
    """
    Collapses the dataset's near-identical variants.

    It ships four rows literally called "Alisha Solid Women's Cycling Shorts",
    differing only in pack size. Showing all four makes the catalogue look
    broken, so only the first survives.
    """
    return re.sub(r"[^a-z0-9]", "", name.lower())


# ----------------------------------------------------------------- read CSV

def load_rows(path):
    csv.field_size_limit(10 ** 9)
    with open(path, encoding="utf-8", errors="replace") as handle:
        for row in csv.DictReader(handle):
            try:
                tree = json.loads(row["product_category_tree"])[0]
            except Exception:
                continue
            parts = [p.strip() for p in tree.split(">>") if p.strip()]
            if len(parts) < 2:
                continue

            try:
                images = [u for u in json.loads(row["image"]) if u.startswith("http")]
            except Exception:
                images = []
            if len(images) < IMAGES_PER_PRODUCT[0]:
                continue

            name = clean_text(row["product_name"], 250)
            if not name:
                continue

            try:
                retail = float(row["retail_price"] or 0)
                disc = float(row["discounted_price"] or 0)
            except ValueError:
                continue
            if retail <= 0:
                continue

            yield {
                "root": parts[0],
                "sub": parts[1],
                "name": name,
                "brand": clean_text(row["brand"], 90),
                "retail": retail,
                "disc": disc if 0 < disc <= retail else retail,
                "images": images,
                "desc": clean_description(row["description"], name),
                "rating": row["product_rating"],
            }


def main():
    if CSV_PATH is None or not CSV_PATH.exists():
        print("usage: build_seed_from_flipkart.py <path-to-flipkart.csv>", file=sys.stderr)
        return 1

    by_sub = defaultdict(list)
    root_counts = Counter()
    seen_names = set()
    duplicates = 0
    for rec in load_rows(CSV_PATH):
        key = dedupe_key(rec["name"])
        if key in seen_names:
            duplicates += 1
            continue
        seen_names.add(key)
        by_sub[(rec["root"], rec["sub"])].append(rec)
        root_counts[rec["root"]] += 1

    # Pick roots by volume, then the busiest subcategories inside each. The long
    # tail of this dataset is product names leaking into the category tree, so a
    # volume floor is what separates real categories from noise.
    roots = [r for r, n in root_counts.most_common() if n >= MIN_ROOT_PRODUCTS][:MAX_ROOTS]

    chosen = {}
    for root in roots:
        subs = sorted(((s, recs) for (r, s), recs in by_sub.items()
                       if r == root and len(recs) >= MIN_SUB_PRODUCTS),
                      key=lambda kv: -len(kv[1]))[:MAX_SUBS_PER_ROOT]
        if subs:
            chosen[root] = subs

    # ------------------------------------------------------- assign ids
    taken_codes = set()
    categories = []          # (catmaster_id, cat_id, subcat_id, name, image, flag)
    products = []
    cat_id = 0
    prod_id = 0

    for root, subs in chosen.items():
        cat_id += 1
        root_code = code_of(root, taken_codes)
        root_row = [cat_id, root_code, "^", root, None, 0]
        categories.append(root_row)

        for sub_name, recs in subs:
            cat_id += 1
            sub_code = code_of(sub_name, taken_codes)
            # Spread the picks across the whole subcategory instead of taking
            # the first N. The CSV is grouped by brand, so the first N would be
            # seven products from one brand.
            step = max(1, len(recs) // MAX_PRODUCTS_PER_SUB)
            picked = recs[::step][:MAX_PRODUCTS_PER_SUB]
            categories.append([cat_id, sub_code, root_code, sub_name, None, 0])

            for rec in picked:
                prod_id += 1
                rec["prod_id"] = prod_id
                rec["catmaster_id"] = cat_id
                products.append(rec)

            if root_row[4] is None and picked:
                root_row[4] = rewrite_image(picked[0]["images"][0])

    # Feature a handful of roots on the home page.
    featured = [c for c in categories if c[2] == "^"][:8]
    for c in featured:
        c[5] = 1

    # ------------------------------------------------------- price + offers
    for rec in products:
        pid = rec["prod_id"]
        mrp = round_price(rec["retail"])
        member = round_price(rec["disc"]) if rec["disc"] < rec["retail"] else None
        if member is not None and member >= mrp:
            member = round_price(mrp * 0.85)
        if member is not None and member >= mrp:
            member = None

        profile = pid % 6                      # same convention as the old seed
        wants_member = profile in (1, 2, 3, 4)
        wants_points = profile in (2, 4, 5)
        wants_hybrid = profile in (3, 4)

        rec["mrp"] = mrp
        rec["member"] = member if (wants_member and member) else None

        basis = rec["member"] or mrp
        rec["points"] = (round(basis / 10) * 10) if (wants_points and basis <= POINTS_ONLY_MAX) else None

        if wants_hybrid and basis <= HYBRID_MAX:
            cash = round_price(basis * 0.6)
            rec["hcash"] = cash
            rec["hpts"] = max(10, round((basis - cash) / 10) * 10)
        else:
            rec["hcash"] = rec["hpts"] = None

        rec["discount_pct"] = round((1 - (rec["member"] or mrp) / mrp) * 100, 2) if mrp else 0.0
        rec["stock"] = random.randint(0, 300)
        try:
            rec["rating_val"] = round(float(rec["rating"]), 1)
        except (TypeError, ValueError):
            rec["rating_val"] = round(random.uniform(3.2, 4.8), 1)
        rec["rating_count"] = random.randint(40, 9000)

    # ------------------------------------------------------- emit SQL
    lines = []
    add = lines.append

    add("-- =====================================================================")
    add("-- e-MART catalogue seed, built from the Flipkart sample dataset.")
    add("--")
    add("-- Replaces ONLY the catalogue. users / address / emart_card / cart are")
    add("-- left untouched, so existing accounts survive. cart_items and wishlist")
    add("-- are cleared because both carry a foreign key to prod_id.")
    add("--")
    add("-- Every price is a clean round rupee value. No 299, no 234.23.")
    add("-- =====================================================================")
    add("")
    add("USE emart;")
    add("")
    add("SET FOREIGN_KEY_CHECKS = 0;")
    add("TRUNCATE TABLE wishlist;")
    add("TRUNCATE TABLE cart_items;")
    add("TRUNCATE TABLE prod_dtl_master;")
    add("TRUNCATE TABLE config_master;")
    add("TRUNCATE TABLE product_image;")
    add("TRUNCATE TABLE product_master;")
    add("TRUNCATE TABLE category_master;")
    add("SET FOREIGN_KEY_CHECKS = 1;")
    add("")

    # --- categories
    add("-- ------------------------------------------------ category_master")
    add("INSERT INTO category_master (catmaster_id, cat_id, subcat_id, cat_name,")
    add("                             cat_image_path, flag) VALUES")
    rows = [f"  ({c[0]}, {sql_str(c[1])}, {sql_str(c[2])}, {sql_str(c[3])}, "
            f"{sql_str(c[4])}, {c[5]})" for c in categories]
    add(",\n".join(rows) + ";")
    add("")

    # --- products
    add("-- ------------------------------------------------- product_master")
    add("INSERT INTO product_master (prod_id, catmaster_id, prod_name, prod_short_desc,")
    add("                            prod_long_desc, mrp_price, cardholder_price,")
    add("                            points_price, hybrid_cash_price, hybrid_points,")
    add("                            brand, stock_quantity, rating, rating_count,")
    add("                            discount_percentage, prod_image_path) VALUES")
    rows = []
    for r in products:
        short = clean_text(f"{r['brand'] or ''} {r['sub']}".strip(), 190)
        rows.append(
            f"  ({r['prod_id']}, {r['catmaster_id']}, {sql_str(r['name'])}, {sql_str(short)}, "
            f"{sql_str(r['desc'])}, {r['mrp']}.00, "
            f"{str(r['member']) + '.00' if r['member'] else 'NULL'}, "
            f"{r['points'] if r['points'] else 'NULL'}, "
            f"{str(r['hcash']) + '.00' if r['hcash'] else 'NULL'}, "
            f"{r['hpts'] if r['hpts'] else 'NULL'}, "
            f"{sql_str(r['brand'])}, {r['stock']}, {r['rating_val']}, {r['rating_count']}, "
            f"{r['discount_pct']}, {sql_str(rewrite_image(r['images'][0]))})")
    add(",\n".join(rows) + ";")
    add("")

    # --- images
    add("-- -------------------------------------------------- product_image")
    add("INSERT INTO product_image (prod_image_id, prod_id, image_url, alt_text,")
    add("                           display_order, is_primary) VALUES")
    rows = []
    img_id = 0
    for r in products:
        urls = r["images"][:IMAGES_PER_PRODUCT[1]]
        if len(urls) < IMAGES_PER_PRODUCT[0]:
            urls = (urls * IMAGES_PER_PRODUCT[0])[:IMAGES_PER_PRODUCT[0]]
        for order, url in enumerate(urls):
            img_id += 1
            alt = f"{r['name']} - view {order + 1} of {len(urls)}"
            rows.append(f"  ({img_id}, {r['prod_id']}, {sql_str(rewrite_image(url))}, "
                        f"{sql_str(alt)}, {order}, {1 if order == 0 else 0})")
    add(",\n".join(rows) + ";")
    add("")

    # --- variants
    add("-- ------------------------------- config_master / prod_dtl_master")
    add("INSERT INTO config_master (config_id, config_name) VALUES")
    add("  (1, 'Color'),\n  (2, 'Size'),\n  (3, 'Storage');")
    add("")
    variant_rows = []
    vid = 0
    palettes = {1: ["Black", "White", "Blue", "Red"],
                2: ["S", "M", "L", "XL"],
                3: ["32GB", "64GB", "128GB"]}
    for r in products:
        if r["prod_id"] % 4:
            continue
        config = 2 if "Clothing" in r["root"] or "Footwear" in r["root"] else 1
        for value in palettes[config][:3]:
            vid += 1
            variant_rows.append(f"  ({vid}, {r['prod_id']}, {config}, {sql_str(value)})")
    add("INSERT INTO prod_dtl_master (prod_dtl_id, prod_id, config_id, config_dtls) VALUES")
    add(",\n".join(variant_rows) + ";")
    add("")

    # --- wishlist for the seeded users
    add("-- -------------------------------------------------------- wishlist")
    add("-- Re-seeded against the NEW prod_ids; the old rows pointed at products")
    add("-- that no longer exist.")
    picks = random.sample(range(1, len(products) + 1), min(8, len(products)))
    wl = [f"  ({i + 1}, {(i % 3) + 1}, {p})" for i, p in enumerate(picks)]
    add("INSERT INTO wishlist (wishlist_id, user_id, prod_id) VALUES")
    add(",\n".join(wl) + ";")
    add("")

    # --- counters
    add("-- --------------------------------------------- AUTO_INCREMENT reset")
    add(f"ALTER TABLE category_master AUTO_INCREMENT = {len(categories) + 100};")
    add(f"ALTER TABLE product_master  AUTO_INCREMENT = {len(products) + 1000};")
    add(f"ALTER TABLE product_image   AUTO_INCREMENT = {img_id + 5000};")
    add("ALTER TABLE config_master   AUTO_INCREMENT = 100;")
    add(f"ALTER TABLE prod_dtl_master AUTO_INCREMENT = {vid + 500};")
    add("ALTER TABLE cart_items      AUTO_INCREMENT = 100;")
    add("ALTER TABLE wishlist        AUTO_INCREMENT = 100;")
    add("")
    add("-- --------------------------------------------------------- summary")
    add("SELECT (SELECT COUNT(*) FROM category_master) AS categories,")
    add("       (SELECT COUNT(*) FROM product_master)  AS products,")
    add("       (SELECT COUNT(*) FROM product_image)   AS images,")
    add("       (SELECT COUNT(*) FROM product_master WHERE cardholder_price IS NOT NULL) AS with_member_price;")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")

    roots_n = sum(1 for c in categories if c[2] == "^")
    print(f"wrote {OUT.name}")
    print(f"  categories : {len(categories)} ({roots_n} top-level)")
    print(f"  products   : {len(products)}")
    print(f"  images     : {img_id}")
    print(f"  variants   : {vid}")
    print(f"  duplicates dropped: {duplicates}")
    print(f"  image style: {IMAGE_STYLE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
