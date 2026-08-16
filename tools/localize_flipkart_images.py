"""
Downloads every image the Flipkart catalogue seed points at and rewrites the
seed to serve them from this repo instead of Flipkart's CDN.

WHY
---
emart_seed_data_flipkart.sql stores absolute rukminim2.flixcart.com URLs. Those
work today, but they make the whole catalogue depend on someone else's CDN
staying up, staying un-blocked, and not changing its URL scheme. CATALOGUE-RESET.md
already settled this argument for the generated placeholders:

    "A fallback that depends on the network is not a fallback."

The same applies to the images themselves. After this script runs, the app
serves every photo from frontend/public/images/catalogue/ and needs no internet
at all - which is also what makes it work inside Docker with no egress.

IN   backend/emart_seed_data_flipkart.sql   (untouched)
OUT  backend/emart_seed_data_catalogue.sql  (same seed, local paths)
     frontend/public/images/catalogue/*.jpeg

Re-runnable: files already on disk are not downloaded again, so an interrupted
run just carries on where it stopped.

    python tools/localize_flipkart_images.py
"""
import re
import sys
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed
from hashlib import sha1
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "backend" / "emart_seed_data_flipkart.sql"
OUT_SQL = ROOT / "backend" / "emart_seed_data_catalogue.sql"
IMG_DIR = ROOT / "frontend" / "public" / "images" / "catalogue"

# The path the BROWSER asks for. nginx (and Vite in dev) serve
# frontend/public/ at the web root, so this is just the folder above.
WEB_PREFIX = "/images/catalogue"

URL_RE = re.compile(r"https://rukminim\d+\.flixcart\.com/[^'\"\s]+")

WORKERS = 16
ATTEMPTS = 3
TIMEOUT = 30

# Flipkart's CDN answers a bare urllib request with 403; it wants a browser-ish
# User-Agent. Nothing else about the request matters.
HEADERS = {"User-Agent": "Mozilla/5.0 (compatible; emart-seed-localizer/1.0)"}


def local_name(url: str, taken: dict) -> str:
    """
    Filename for a URL. The CDN basename is already unique in practice, and
    keeping it means a file on disk can be traced straight back to its source.
    Collisions are still handled rather than assumed away: a second URL wanting
    a name that is taken gets a short hash of its full path appended.
    """
    name = url.rsplit("/", 1)[-1] or "image.jpeg"
    name = re.sub(r"[^A-Za-z0-9._-]", "-", name)
    if not name.lower().endswith((".jpeg", ".jpg", ".png", ".webp")):
        name += ".jpeg"

    if taken.get(name, url) != url:
        stem, dot, ext = name.rpartition(".")
        name = f"{stem}-{sha1(url.encode()).hexdigest()[:8]}{dot}{ext}"
    taken[name] = url
    return name


def download(url: str, dest: Path) -> tuple[str, str | None]:
    if dest.exists() and dest.stat().st_size > 0:
        return url, None

    last = None
    for attempt in range(1, ATTEMPTS + 1):
        try:
            req = urllib.request.Request(url, headers=HEADERS)
            with urllib.request.urlopen(req, timeout=TIMEOUT) as r:
                body = r.read()
            if not body:
                raise ValueError("empty response")
            # Write to a temp name first: an interrupted run must never leave a
            # half-written file that the next run then skips as "already there".
            tmp = dest.with_suffix(dest.suffix + ".part")
            tmp.write_bytes(body)
            tmp.replace(dest)
            return url, None
        except Exception as e:  # noqa: BLE001 - any failure is just a retry
            last = f"{type(e).__name__}: {e}"
    return url, last


# dead URL -> the local filename to use in its place. Filled by resolve_dead().
DEAD_FIXES: dict[str, str] = {}

# Same visual language as the generated placeholders already in
# frontend/public/images/products/: soft gradient, white disc, the product's own
# name. Inline SVG, so it costs one file and zero network.
PLACEHOLDER = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 800" \
width="800" height="800" role="img" aria-label="{alt}">
<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
<stop offset="0%" stop-color="#f1f5f9"/><stop offset="100%" stop-color="#cbd5e1"/></linearGradient></defs>
<rect width="800" height="800" fill="url(#g)"/>
<circle cx="400" cy="330" r="150" fill="#ffffff" opacity="0.7"/>
<g transform="translate(400,330)" fill="none" stroke="#64748b" stroke-width="6"
   stroke-linecap="round" stroke-linejoin="round">
<rect x="-70" y="-55" width="140" height="110" rx="10"/>
<circle cx="-28" cy="-18" r="14"/><path d="M-70 34l46-40 34 30 26-22 34 32"/>
</g>
<text x="400" y="600" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif"
      font-size="34" font-weight="600" fill="#334155">{line1}</text>
<text x="400" y="646" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif"
      font-size="34" font-weight="600" fill="#334155">{line2}</text>
<text x="400" y="706" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif"
      font-size="26" fill="#94a3b8">Image unavailable</text>
</svg>
"""


def _esc(s: str) -> str:
    return (s.replace("&", "&amp;").replace("<", "&lt;")
             .replace(">", "&gt;").replace('"', "&quot;"))


def _wrap(name: str) -> tuple[str, str]:
    """Split a product name over two lines without cutting a word in half."""
    words, line1 = name.split(), ""
    for i, w in enumerate(words):
        if len(line1) + len(w) + 1 > 26:
            rest = " ".join(words[i:])
            return line1.strip(), (rest[:26] + "…") if len(rest) > 26 else rest
        line1 += w + " "
    return line1.strip(), ""


def resolve_dead(sql: str, mapping: dict, dead: set) -> None:
    """
    Give every dead URL a local file to point at.

    Preference order, per product:
      1. another image of the SAME product that downloaded fine - a real photo
         of the right thing always beats a placeholder;
      2. a generated placeholder carrying the product's name, for the products
         whose every image is gone.
    """
    def rows(table, pattern):
        i = sql.find("INSERT INTO " + table)
        return re.findall(pattern, sql[i:sql.find(";\n", i)]) if i >= 0 else []

    # (prod_image_id, prod_id, url)
    images = rows("product_image", r"\((\d+),\s*(\d+),\s*'([^']+)'")
    # (prod_id, catmaster_id, prod_name)
    products = rows("product_master", r"\(\s*(\d+),\s*(\d+),\s*'((?:[^']|'')*)'")
    names = {int(p): n.replace("''", "'") for p, _c, n in products}

    live_by_prod: dict[int, str] = {}
    owner: dict[str, int] = {}
    for _iid, pid, url in images:
        pid = int(pid)
        owner.setdefault(url, pid)
        if url not in dead and pid not in live_by_prod:
            live_by_prod[pid] = mapping[url]

    # prod_image_path on product_master is the card thumbnail. It can be dead
    # while the gallery is fine, and it does not always appear in product_image,
    # so the owning product has to be read off the product_master row itself.
    i = sql.find("INSERT INTO product_master")
    pm_block = sql[i:sql.find(";\n", i)] if i >= 0 else ""
    for row in re.split(r"\n\s*(?=\(\s*\d+,)", pm_block):
        pid_m = re.match(r"\s*\(\s*(\d+),", row)
        if not pid_m:
            continue
        for url in URL_RE.findall(row):
            owner.setdefault(url, int(pid_m.group(1)))

    for url in dead:
        pid = owner.get(url)

        if pid is not None and pid in live_by_prod:
            DEAD_FIXES[url] = live_by_prod[pid]
            continue

        name = names.get(pid, "Product") if pid is not None else "Product"
        fname = f"placeholder-{pid or 'x'}.svg"
        l1, l2 = _wrap(name)
        (IMG_DIR / fname).write_text(
            PLACEHOLDER.format(alt=_esc(name), line1=_esc(l1), line2=_esc(l2)),
            encoding="utf-8",
        )
        DEAD_FIXES[url] = fname

    made = len({v for v in DEAD_FIXES.values() if v.startswith("placeholder-")})
    reused = len(DEAD_FIXES) - sum(
        1 for v in DEAD_FIXES.values() if v.startswith("placeholder-"))
    print(f"\ndead URLs resolved: {reused} repointed to a sibling photo, "
          f"{len(DEAD_FIXES) - reused} to {made} generated placeholder(s)")


def main() -> int:
    if not SRC.exists():
        print(f"missing {SRC}", file=sys.stderr)
        return 1

    sql = SRC.read_text(encoding="utf-8")
    urls = sorted(set(URL_RE.findall(sql)))
    print(f"{len(urls)} distinct image URLs in {SRC.name}")

    IMG_DIR.mkdir(parents=True, exist_ok=True)

    taken: dict[str, str] = {}
    mapping = {u: local_name(u, taken) for u in urls}

    have = sum(1 for u, n in mapping.items() if (IMG_DIR / n).exists())
    print(f"{have} already on disk, {len(urls) - have} to fetch")

    failures: dict[str, str] = {}
    done = 0
    with ThreadPoolExecutor(max_workers=WORKERS) as pool:
        futures = {pool.submit(download, u, IMG_DIR / n): u for u, n in mapping.items()}
        for f in as_completed(futures):
            url, err = f.result()
            done += 1
            if err:
                failures[url] = err
            if done % 200 == 0 or done == len(urls):
                print(f"  {done}/{len(urls)}  failed={len(failures)}")

    # Some of the CDN URLs are simply dead - the products were delisted years
    # after the CSV was captured, and Flipkart answers 404. Leaving those as
    # remote URLs would put the one thing this script exists to remove back into
    # the catalogue: a network request that fails. Resolve them locally instead.
    if failures:
        resolve_dead(sql, mapping, set(failures))

    # Rewrite the seed. Longest URLs first so no URL that is a prefix of another
    # can be replaced out from under it.
    out = sql
    for url in sorted(mapping, key=len, reverse=True):
        if url in failures and url not in DEAD_FIXES:
            continue
        local = DEAD_FIXES.get(url) or mapping[url]
        out = out.replace(url, f"{WEB_PREFIX}/{local}")

    banner = (
        "-- GENERATED by tools/localize_flipkart_images.py - do not edit by hand.\n"
        "-- Same catalogue as emart_seed_data_flipkart.sql, except every image\n"
        f"-- points at {WEB_PREFIX}/... in frontend/public/ instead of Flipkart's\n"
        "-- CDN, so the app needs no internet to render the catalogue.\n"
    )
    OUT_SQL.write_text(banner + out, encoding="utf-8")

    left = len(URL_RE.findall(out))
    total = sum(f.stat().st_size for f in IMG_DIR.glob("*") if f.is_file())
    print(f"\nwrote {OUT_SQL.relative_to(ROOT)}")
    print(f"remote URLs remaining: {left}")
    print(f"images on disk: {len(list(IMG_DIR.glob('*')))}  ({total / 1e6:.1f} MB)")

    unresolved = {u: e for u, e in failures.items() if u not in DEAD_FIXES}
    if failures:
        print(f"\n{len(failures)} URLs did not download "
              f"({len(failures) - len(unresolved)} resolved locally, "
              f"{len(unresolved)} still remote):")
        for u, e in list(failures.items())[:5]:
            print(f"  {e}  {u.rsplit('/', 1)[-1]}")
    return 1 if left else 0


if __name__ == "__main__":
    raise SystemExit(main())
