"""
Validates emart_seed_data_flipkart.sql against emart_schema.sql.

Written because a first, sloppier check produced a false alarm: its regex for
"product rows" also matched product_image rows, so it reported 2,259 products
and 176 duplicate names that were actually image URLs. This version parses each
INSERT block properly - from the column list to the terminating semicolon - so
a row can only be counted as the table it belongs to.

Checks:
  1. every INSERT's value count matches its column count
  2. every price is a clean round rupee value
  3. FK integrity: product -> category, image -> product, variant -> product
  4. the schema's CHECK constraints hold (hybrid pair)
  5. no duplicate product names
  6. 4-6 images per product, exactly one primary
  7. points offers are affordable by a seeded account
"""
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SEED = ROOT / "backend" / "emart_seed_data_flipkart.sql"

MAX_SEEDED_POINTS = 15_000
err = []


def split_top_level(text):
    """Splits a VALUES row on commas that are not inside quotes."""
    out, buf, in_str, i = [], [], False, 0
    while i < len(text):
        ch = text[i]
        if ch == "'":
            if in_str and i + 1 < len(text) and text[i + 1] == "'":
                buf.append("''")
                i += 2
                continue
            in_str = not in_str
            buf.append(ch)
        elif ch == "," and not in_str:
            out.append("".join(buf).strip())
            buf = []
        else:
            buf.append(ch)
        i += 1
    if buf:
        out.append("".join(buf).strip())
    return out


def parse_inserts(sql):
    """table -> (columns, [row-value-lists])"""
    tables = {}
    for m in re.finditer(r"INSERT INTO (\w+)\s*\(([^)]*)\)\s*VALUES\s*", sql):
        table = m.group(1)
        cols = [c.strip() for c in m.group(2).split(",")]
        body = sql[m.end():]
        # +1 so the terminating ";" stays INSIDE the slice. Without it the
        # final "...);" line falls outside and every table silently loses its
        # last row - which is exactly the false alarm this checker first
        # reported (product 390 "missing", category 64 "does not exist").
        end = body.find(";\n")
        end = len(body) if end == -1 else end + 1
        rows = []
        for rm in re.finditer(r"^\s*\((.*)\)[,;]\s*$", body[:end], re.M):
            rows.append(split_top_level(rm.group(1)))
        tables[table] = (cols, rows)
    return tables


def num(v):
    if v is None or v.upper() == "NULL":
        return None
    return float(v)


def main():
    if not SEED.exists():
        print(f"missing {SEED}", file=sys.stderr)
        return 1

    sql = SEED.read_text(encoding="utf-8")
    tables = parse_inserts(sql)

    print("rows per table:")
    for t, (cols, rows) in tables.items():
        print(f"   {t:18} {len(rows):6}  ({len(cols)} columns)")
        for i, r in enumerate(rows):
            if len(r) != len(cols):
                err.append(f"{t} row {i + 1}: {len(r)} values for {len(cols)} columns")
                break
    print()

    def col(table, name):
        cols = tables[table][0]
        return cols.index(name)

    # ---------------------------------------------------------- products
    pcols, prows = tables["product_master"]
    i_id, i_cat = col("product_master", "prod_id"), col("product_master", "catmaster_id")
    i_name = col("product_master", "prod_name")
    i_mrp = col("product_master", "mrp_price")
    i_ch = col("product_master", "cardholder_price")
    i_pp = col("product_master", "points_price")
    i_hc = col("product_master", "hybrid_cash_price")
    i_hp = col("product_master", "hybrid_points")

    cat_ids = {int(r[col("category_master", "catmaster_id")]) for r in tables["category_master"][1]}
    prod_ids = set()
    names = Counter()
    charm = []
    offer_counts = Counter()

    for r in prows:
        pid = int(r[i_id])
        prod_ids.add(pid)
        names[r[i_name]] += 1

        if int(r[i_cat]) not in cat_ids:
            err.append(f"product {pid}: catmaster_id {r[i_cat]} does not exist")

        mrp, ch, pp, hc, hp = (num(r[i_mrp]), num(r[i_ch]), num(r[i_pp]),
                               num(r[i_hc]), num(r[i_hp]))

        for label, v in (("mrp", mrp), ("cardholder", ch), ("hybrid_cash", hc)):
            if v is not None and (v != int(v) or int(v) % 100 != 0):
                charm.append(f"product {pid} {label}={v}")

        if ch is not None and mrp is not None and ch >= mrp:
            err.append(f"product {pid}: cardholder_price {ch} >= mrp {mrp}")

        if (hc is None) != (hp is None):
            err.append(f"product {pid}: half-filled hybrid offer violates chk_product_hybrid_pair")

        if pp is not None and pp > MAX_SEEDED_POINTS:
            err.append(f"product {pid}: points_price {pp} exceeds the richest seeded balance")

        offer_counts[(ch is not None, pp is not None, hc is not None)] += 1

    dups = [n for n, c in names.items() if c > 1]
    if dups:
        err.append(f"{len(dups)} duplicate product names, e.g. {dups[0][:60]!r}")
    if charm:
        err.append(f"{len(charm)} non-round prices, e.g. {charm[0]}")

    # ---------------------------------------------------------- images
    icols, irows = tables["product_image"]
    i_ipid = col("product_image", "prod_id")
    i_prim = col("product_image", "is_primary")
    i_url = col("product_image", "image_url")

    per_product = defaultdict(list)
    for r in irows:
        pid = int(r[i_ipid])
        if pid not in prod_ids:
            err.append(f"product_image references missing prod_id {pid}")
        per_product[pid].append(r)

    missing = prod_ids - set(per_product)
    if missing:
        err.append(f"{len(missing)} products have no images")
    bad_count = [p for p, v in per_product.items() if not 4 <= len(v) <= 6]
    if bad_count:
        err.append(f"{len(bad_count)} products outside the 4-6 image range")
    bad_prim = [p for p, v in per_product.items()
                if sum(1 for x in v if x[i_prim] == "1") != 1]
    if bad_prim:
        err.append(f"{len(bad_prim)} products lack exactly one primary image")

    hosts = Counter(re.sub(r"^'https?://([^/]+)/.*$", r"\1", r[i_url]) for r in irows)

    # ---------------------------------------------------------- variants
    if "prod_dtl_master" in tables:
        i_vpid = col("prod_dtl_master", "prod_id")
        cfg_ids = {int(r[0]) for r in tables["config_master"][1]}
        i_cfg = col("prod_dtl_master", "config_id")
        for r in tables["prod_dtl_master"][1]:
            if int(r[i_vpid]) not in prod_ids:
                err.append(f"prod_dtl_master references missing prod_id {r[i_vpid]}")
                break
            if int(r[i_cfg]) not in cfg_ids:
                err.append(f"prod_dtl_master references missing config_id {r[i_cfg]}")
                break

    # ---------------------------------------------------------- wishlist
    if "wishlist" in tables:
        i_wpid = col("wishlist", "prod_id")
        for r in tables["wishlist"][1]:
            if int(r[i_wpid]) not in prod_ids:
                err.append(f"wishlist references missing prod_id {r[i_wpid]}")
                break

    # ---------------------------------------------------------- report
    print("offer mix (member / points / hybrid):")
    for k, v in sorted(offer_counts.items()):
        print(f"   member={str(k[0]):5} points={str(k[1]):5} hybrid={str(k[2]):5} -> {v}")
    none_offer = offer_counts.get((False, False, False), 0)
    if none_offer == 0:
        err.append("no product lacks all offers - the hide-the-checkboxes path is untested")
    print()
    print("image hosts:")
    for h, c in hosts.most_common(5):
        print(f"   {c:6} {h}")
    print()

    if err:
        print("ERRORS:")
        for e in dict.fromkeys(err):
            print("  -", e)
    else:
        print("PASS - column counts, round prices, FK integrity, hybrid pairs,\n"
              "       image coverage and offer mix all check out.")
    return 1 if err else 0


if __name__ == "__main__":
    raise SystemExit(main())
