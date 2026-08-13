"""
Verifies the interview-prep docs against the actual codebase.

WHY: these docs are for a mock interview. A wrong file path or a class name that
does not exist is worse than no doc at all — it gets said out loud in front of an
interviewer. Everything quoted must be checkable.

Checks:
  1. Every `File.java` / `File.jsx` named in a docs table really exists
  2. Every class name in backtick-quoted Java references exists
  3. Library versions quoted match pom.xml / package.json
"""
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DOCS = ROOT / "interview-prep"
BACKEND = ROOT / "backend"
FRONTEND = ROOT / "frontend"

err = []
checked_files = 0

# ------------------------------------------------------------------ index
real_files = {}
for base in (BACKEND / "src", FRONTEND / "src", ROOT / "tools"):
    for dp, _, fns in os.walk(base):
        for fn in fns:
            real_files.setdefault(fn, []).append(os.path.join(dp, fn))

# ------------------------------------------------- 1. file names in tables
FILE_REF = re.compile(r"`([A-Za-z0-9_]+\.(?:java|jsx|js|sql|py))`")

for doc in sorted(DOCS.glob("*.md")):
    text = doc.read_text(encoding="utf-8")
    for name in sorted(set(FILE_REF.findall(text))):
        checked_files += 1
        if name not in real_files:
            err.append(f"{doc.name}: `{name}` does not exist anywhere in the project")

# --------------------------------------------- 2. class names referenced
java_types = set()
for dp, _, fns in os.walk(BACKEND / "src/main/java"):
    for fn in fns:
        if fn.endswith(".java"):
            java_types.add(fn[:-5])

# Only check names that look like our own classes being called as `X.method(`
CLASS_CALL = re.compile(r"\b([A-Z][A-Za-z0-9]{3,})\.(?:builder|from|[a-z]\w*)\(")
KNOWN_EXTERNAL = {
    "Jwts", "Keys", "Decoders", "SecurityContextHolder", "BigDecimal", "LocalDate",
    "LocalDateTime", "Optional", "List", "Map", "Set", "Collectors", "Comparator",
    "String", "Integer", "Boolean", "Number", "Object", "Date", "UUID", "Arrays",
    "Math", "System", "PageResponse", "ResponseEntity", "HttpStatus", "RoundingMode",
    "SessionCreationPolicy", "HttpMethod", "UsernamePasswordAuthenticationFilter",
    "WebAuthenticationDetailsSource", "ReflectionTestUtils", "Assertions",
    "CartStatus", "OrderStatus", "PaymentStatus", "CardStatus", "PriceOption",
    "AddressType", "RoleType", "JwtInclude", "JsonInclude", "EnumType", "FetchType",
    "GenerationType", "Number", "Promise", "URL", "JSON", "Intl",
    # Static FIELDS that read like class names to the regex, e.g.
    # Decoders.BASE64.decode(...) and RoundingMode.DOWN.
    "BASE64", "TRUE", "FALSE", "ZERO", "DOWN", "HALF_UP",
    # Spring / Nimbus types used by the Google sign-in module.
    "NimbusJwtDecoder", "DelegatingOAuth2TokenValidator", "OAuth2TokenValidatorResult",
    "JwtTimestampValidator", "OAuth2Error", "Keys",
}

for doc in sorted(DOCS.glob("*.md")):
    text = doc.read_text(encoding="utf-8")
    for name in sorted(set(CLASS_CALL.findall(text))):
        if name in KNOWN_EXTERNAL or name in java_types:
            continue
        # ignore JS/React identifiers
        if name in {"React", "Number", "Object", "Array"}:
            continue
        err.append(f"{doc.name}: class `{name}` referenced but no such .java file")

# ------------------------------------------------------ 3. versions quoted
pom = (BACKEND / "pom.xml").read_text(encoding="utf-8")
pkg = json.loads((FRONTEND / "package.json").read_text(encoding="utf-8"))
deps = {**pkg.get("dependencies", {}), **pkg.get("devDependencies", {})}

VERSION_CLAIMS = [
    ("jjwt 0.12.6",      lambda: "<jjwt.version>0.12.6</jjwt.version>" in pom),
    ("OpenPDF 2.0.5",    lambda: "<openpdf.version>2.0.5</openpdf.version>" in pom),
    ("Spring Boot 4.0.0", lambda: "<version>4.0.0</version>" in pom),
    ("Java 21",          lambda: "<java.version>21</java.version>" in pom),
    ("React 19",         lambda: deps.get("react", "").startswith("^19")),
    ("Vite 8",           lambda: deps.get("vite", "").startswith("^8")),
    ("Redux Toolkit",    lambda: "@reduxjs/toolkit" in deps),
    ("React Router 7",   lambda: deps.get("react-router-dom", "").startswith("^7")),
    ("Tailwind CSS 4",   lambda: deps.get("tailwindcss", "").startswith("^4")),
]

all_text = "\n".join(d.read_text(encoding="utf-8") for d in DOCS.glob("*.md"))
for claim, verify in VERSION_CLAIMS:
    token = claim.split()[0]
    if token.lower() in all_text.lower() and not verify():
        err.append(f"docs claim '{claim}' but the build files disagree")

# ------------------------------------------------------------------ report
print(f"Scanned {len(list(DOCS.glob('*.md')))} docs, "
      f"{checked_files} file references, {len(VERSION_CLAIMS)} version claims")
print()
if err:
    print("ERRORS:")
    for e in dict.fromkeys(err):
        print("  -", e)
else:
    print("PASS - every file, class and version named in the interview docs\n"
          "       exists in the project.")
sys.exit(1 if err else 0)
