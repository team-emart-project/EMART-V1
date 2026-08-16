"""
One-shot rewrite of the product_master INSERT block in emart_seed_data.sql.

What it does
------------
1. Rounds every mrp_price to a clean number (no charm pricing: no 2999, no 24.99).
2. Drops the old points_to_redeem column.
3. Adds the four new e-MART card offer columns and fills them so that the
   catalogue contains a realistic mix of offer combinations, including
   products with no member offer at all.

Points economy used here: 1 e-Point = 1 rupee of value.
POINTS-only and HYBRID offers are therefore restricted to cheaper products,
because nobody can pay 75,000 points for a phone. That restriction is not
cosmetic: it is what makes the seeded point balances (250 - 15,000) actually
usable in testing.
"""
import re
import sys
from pathlib import Path

SEED = Path(__file__).resolve().parent.parent / "backend" / "emart_seed_data.sql"

# Price bands -> rounding step. Bigger numbers get coarser rounding, which is
# how real round pricing works (you see 74,999 -> 75,000, but 49 -> 50).
BANDS = [(100, 10), (1_000, 50), (10_000, 100), (100_000, 500), (float("inf"), 1_000)]

# Points-only is only offered at or below this cash price.
POINTS_ONLY_MAX = 3_000
# Hybrid (part cash, part points) is only offered at or below this cash price.
HYBRID_MAX = 10_000


def round_clean(value: float) -> int:
    """Round a price up/down to the nearest step for its band, never to 0."""
    for ceiling, step in BANDS:
        if value < ceiling:
            return max(step, int(round(value / step) * step))
    return int(value)


def round_points(value: float) -> int:
    """Points are always shown in tens - 1,250 reads better than 1,247."""
    return max(10, int(round(value / 10) * 10))


# Each product gets one of six offer profiles, chosen by prod_id % 6.
# Deterministic on purpose: re-running this script produces the same catalogue,
# so screenshots and test scripts stay valid.
PROFILES = {
    0: (),                            # no member offer at all -> UI hides all 3 checkboxes
    1: ("member",),
    2: ("member", "points"),
    3: ("member", "hybrid"),
    4: ("member", "points", "hybrid"),
    5: ("points",),                   # points-only, no member cash price
}

ROW = re.compile(
    r"^\s*\((?P<prod_id>\d+), (?P<cat>\d+), (?P<name>'(?:[^']|'')*'), "
    r"(?P<short>'(?:[^']|'')*'), (?P<long>'(?:[^']|'')*'), "
    r"(?P<mrp>[\d.]+), (?P<chp>[\d.]+), (?P<ptr>\d+), "
    r"(?P<brand>'(?:[^']|'')*'), (?P<stock>\d+), (?P<rating>[\d.]+), "
    r"(?P<rcount>\d+), (?P<disc>[\d.]+), (?P<img>'(?:[^']|'')*')\)(?P<tail>[,;])\s*$"
)


def rewrite_row(m: re.Match) -> str:
    pid = int(m.group("prod_id"))
    mrp = round_clean(float(m.group("mrp")))
    disc = float(m.group("disc"))

    wanted = PROFILES[pid % 6]

    # Option 1 - member cash price.
    if "member" in wanted:
        member = round_clean(mrp * (1 - disc / 100))
        if member >= mrp:                       # rounding collapsed the saving
            member = round_clean(mrp * 0.9)
        member_sql = f"{member}.00"
    else:
        member = None
        member_sql = "NULL"

    # Option 2 - points only. Cash falls to 0, so the point cost is the
    # member price if there is one, otherwise the full price.
    basis = member if member is not None else mrp
    if "points" in wanted and basis <= POINTS_ONLY_MAX:
        points_sql = str(round_points(basis))
    else:
        points_sql = "NULL"

    # Option 3 - hybrid. Roughly 60% cash, the rest converted to points.
    if "hybrid" in wanted and basis <= HYBRID_MAX:
        h_cash = round_clean(basis * 0.6)
        h_pts = round_points(max(10, basis - h_cash))
        hybrid_cash_sql, hybrid_pts_sql = f"{h_cash}.00", str(h_pts)
    else:
        hybrid_cash_sql, hybrid_pts_sql = "NULL", "NULL"

    # The long description quoted a percentage; the spec says never show a
    # percentage, so strip that sentence rather than leave it contradicting
    # what the card renders.
    long_desc = re.sub(r"\s*e-MART members save \d+% off MRP\.", "", m.group("long"))

    return (
        f"  ({pid}, {m.group('cat')}, {m.group('name')}, {m.group('short')}, {long_desc}, "
        f"{mrp}.00, {member_sql}, {points_sql}, {hybrid_cash_sql}, {hybrid_pts_sql}, "
        f"{m.group('brand')}, {m.group('stock')}, {m.group('rating')}, {m.group('rcount')}, "
        f"{m.group('disc')}, {m.group('img')}){m.group('tail')}\n"
    )


def main() -> int:
    text = SEED.read_text(encoding="utf-8").splitlines(keepends=True)
    out, changed, skipped = [], 0, []
    in_block = False

    for line in text:
        if line.startswith("INSERT INTO product_master"):
            in_block = True
            out.append(
                "INSERT INTO product_master (prod_id, catmaster_id, prod_name, prod_short_desc,\n"
                "                            prod_long_desc, mrp_price, cardholder_price,\n"
                "                            points_price, hybrid_cash_price, hybrid_points,\n"
                "                            brand, stock_quantity, rating, rating_count,\n"
                "                            discount_percentage, prod_image_path) VALUES\n"
            )
            continue

        if in_block:
            if "prod_image_path) VALUES" in line or "points_to_redeem" in line:
                continue                       # leftover column-list lines
            m = ROW.match(line)
            if m:
                out.append(rewrite_row(m))
                changed += 1
                if line.rstrip().endswith(";"):
                    in_block = False
                continue
            if line.strip() and not line.lstrip().startswith("--"):
                skipped.append(line.rstrip()[:90])
            if line.rstrip().endswith(";"):
                in_block = False

        out.append(line)

    if skipped:
        print("UNPARSED LINES INSIDE product_master BLOCK:", file=sys.stderr)
        for s in skipped:
            print("   ", s, file=sys.stderr)
        return 1

    SEED.write_text("".join(out), encoding="utf-8")
    print(f"rewrote {changed} product rows")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
