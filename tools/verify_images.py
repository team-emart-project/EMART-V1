"""
Checks whether the seeded image URLs actually load. RUN THIS ON YOUR MACHINE.

WHY THIS EXISTS
---------------
The Flipkart CSV was crawled in 2016 and its image URLs point at
img5a/img6a.flixcart.com, hostnames Flipkart retired years ago. The seed
generator rewrites them to the current CDN (rukminim2.flixcart.com), but
whether that rewrite actually resolves can only be confirmed from a machine
with real internet access.

This matters more than it sounds. Earlier in this project every product image
silently fell through to an external placeholder service, and when that service
was slow nothing rendered at all. Shipping 390 products whose images 404 would
repeat that mistake, so it gets checked rather than assumed.

USAGE
-----
    python tools/verify_images.py                 # samples 40 URLs
    python tools/verify_images.py 200             # samples 200
    python tools/verify_images.py 40 original     # tests the ORIGINAL 2016 URLs

If the modern style fails and the original works (or vice versa), set
IMAGE_STYLE at the top of build_seed_from_flipkart.py accordingly and rerun it.
"""
import random
import re
import sys
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path

SEED = Path(__file__).resolve().parent.parent / "backend" / "emart_seed_data_flipkart.sql"
SAMPLE = int(sys.argv[1]) if len(sys.argv) > 1 else 40
STYLE = sys.argv[2] if len(sys.argv) > 2 else "asis"
TIMEOUT = 10

# Some CDNs refuse requests without a browser-ish User-Agent, which would look
# like a dead URL when it is really a bot block. Send one so a failure here
# means the image is genuinely unavailable.
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                  "(KHTML, like Gecko) Chrome/120.0 Safari/537.36",
    "Accept": "image/avif,image/webp,image/*,*/*;q=0.8",
}


def to_original(url):
    """rukminim2.flixcart.com/image/416/416/<path>  ->  the 2016 form."""
    m = re.match(r"https://rukminim\d?\.flixcart\.com/image/\d+/\d+/(.+)$", url)
    return f"http://img5a.flixcart.com/image/{m.group(1)}" if m else url


def check(url):
    """Returns (ok, detail). Falls back to GET because some CDNs reject HEAD."""
    for method in ("HEAD", "GET"):
        try:
            req = urllib.request.Request(url, headers=HEADERS, method=method)
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                ctype = resp.headers.get("Content-Type", "")
                if resp.status == 200 and ctype.startswith("image/"):
                    return True, ctype
                if resp.status == 200:
                    return False, f"200 but Content-Type={ctype or 'unknown'}"
                return False, f"HTTP {resp.status}"
        except urllib.error.HTTPError as e:
            if method == "GET":
                return False, f"HTTP {e.code}"
        except Exception as e:                       # DNS, TLS, timeout
            if method == "GET":
                return False, type(e).__name__
    return False, "unreachable"


def main():
    if not SEED.exists():
        print(f"missing {SEED}\nRun build_seed_from_flipkart.py first.", file=sys.stderr)
        return 2

    urls = re.findall(r"'(https?://[^']+\.(?:jpe?g|png|webp))'",
                      SEED.read_text(encoding="utf-8"))
    urls = list(dict.fromkeys(urls))
    if not urls:
        print("no image URLs found in the seed file", file=sys.stderr)
        return 2

    random.seed(1)
    sample = random.sample(urls, min(SAMPLE, len(urls)))
    if STYLE == "original":
        sample = [to_original(u) for u in sample]

    print(f"{len(urls)} distinct image URLs in the seed; testing {len(sample)} "
          f"({'original 2016' if STYLE == 'original' else 'as written'})\n")

    ok = 0
    reasons = Counter()
    for i, url in enumerate(sample, 1):
        good, detail = check(url)
        ok += good
        if not good:
            reasons[detail] += 1
        mark = "ok  " if good else "FAIL"
        print(f"  [{i:3}/{len(sample)}] {mark} {url[:96]}")

    pct = 100.0 * ok / len(sample)
    print(f"\n{ok}/{len(sample)} loaded ({pct:.0f}%)")
    if reasons:
        print("failure reasons:")
        for r, c in reasons.most_common():
            print(f"   {c:4}  {r}")

    print()
    if pct >= 90:
        print("VERDICT: these URLs work. Load the seed as-is.")
    elif pct >= 40:
        print("VERDICT: partially working. Usable — the front end already falls back\n"
              "         to an inline SVG placeholder per image via onError.")
    else:
        print("VERDICT: these URLs are dead.\n"
              "         Try the other style:  python tools/verify_images.py 40 original\n"
              "         If neither works, tell Claude and it will switch the seed to\n"
              "         locally generated images instead.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
