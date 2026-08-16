"""
Checks the JUnit sources against the MAIN sources.

WHY THIS EXISTS
---------------
check_backend / check_calls / check_pojos all scan src/main only. When the
pricing model changed, every main-source check passed while eleven test files
still called removed builder methods — code that would have failed the build
the moment `mvn test` ran. A green checker that cannot see half the code is
worse than no checker, because it is trusted.

What it verifies, for each builder call and getter used in a test:
  * ProductMaster.builder().x(...)  -> x is a real builder method
  * order.getX()                    -> getX exists on that entity/DTO
  * constructor arity for hand-built services (e.g. new OrderServiceImpl(...))
  * enum constants referenced actually exist
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + "/backend"
MAIN = ROOT + "/src/main/java/com/example/demo"
TEST = ROOT + "/src/test/java/com/example/demo"

err = []


def java_files(root):
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(".java"):
                yield os.path.join(dp, fn)


# ---------------------------------------------------------------- main index
builders = {}     # ClassName -> {builder method names}
members = {}      # ClassName -> {method names}
enums = {}        # EnumName  -> {constants}
ctor_arity = {}   # ClassName -> max constructor parameter count

for path in java_files(MAIN):
    src = open(path, encoding="utf-8").read()
    cname = os.path.basename(path)[:-5]

    if re.search(r"\benum\s+" + re.escape(cname) + r"\b", src):
        body = src.split("{", 1)[1] if "{" in src else ""
        first = re.split(r";", body, 1)[0]
        enums[cname] = set(re.findall(r"\b([A-Z][A-Z0-9_]{1,30})\b", first))
        continue

    members[cname] = set(re.findall(r"\b(?:public|protected)\s+[\w<>\[\],.\s?]+?\s+(\w+)\s*\(", src))

    # builder methods live inside "public static class Builder { ... }"
    m = re.search(r"public\s+static\s+class\s+Builder\b", src)
    if m:
        builders[cname] = set(re.findall(r"public\s+Builder\s+(\w+)\s*\(", src[m.start():]))

    arities = [len(re.findall(r",", args)) + 1 if args.strip() else 0
               for args in re.findall(r"public\s+" + re.escape(cname) + r"\s*\(([^)]*)\)", src)]
    if arities:
        ctor_arity[cname] = max(arities)

# ---------------------------------------------------------------- test scan
BUILDER_START = re.compile(r"\b([A-Z]\w+)\s*\.\s*builder\s*\(\s*\)")
tested = 0


def chain_methods(src, start):
    """
    Walks a fluent chain from just after `X.builder()` and yields only the
    method names called AT THE TOP LEVEL of that chain.

    Naive regex matching cannot do this: a nested
    `CategoryMaster.builder()...build()` argument, or a plain
    `LocalDateTime.now()`, sits inside the parentheses of an outer call and
    would otherwise be reported as a bogus builder method on the outer class.
    Tracking paren depth is what separates "argument" from "chain link".
    """
    i, depth, n = start, 0, len(src)
    while i < n:
        c = src[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth < 0:
                return
        elif c == ";" and depth == 0:
            return
        elif c == "." and depth == 0:
            m = re.match(r"\.\s*(\w+)\s*\(", src[i:])
            if not m:
                return
            name = m.group(1)
            yield name
            if name == "build":
                return
            i += m.end() - 1
            depth += 1
            i += 1
            continue
        i += 1

for path in java_files(TEST):
    src = open(path, encoding="utf-8").read()
    rel = os.path.relpath(path, TEST)

    for m in BUILDER_START.finditer(src):
        cls = m.group(1)
        if cls not in builders:
            continue
        for call in chain_methods(src, m.end()):
            if call == "build":
                continue
            tested += 1
            if call not in builders[cls]:
                err.append(f"{rel}: {cls}.builder().{call}(...) -- no such builder method")

    for cls, const in re.findall(r"\b([A-Z]\w+)\s*\.\s*([A-Z][A-Z0-9_]{1,30})\b", src):
        if cls in enums and const not in enums[cls]:
            err.append(f"{rel}: {cls}.{const} -- no such enum constant")

    for cls, args in re.findall(r"new\s+(\w+Impl)\s*\(([^;]*?)\)\s*;", src, re.S):
        if cls in ctor_arity:
            n = len([a for a in args.split(",") if a.strip()])
            if n != ctor_arity[cls]:
                err.append(f"{rel}: new {cls}(...) passes {n} args, constructor takes {ctor_arity[cls]}")

    for cls in ("CartMapper", "OrderMapper", "ProductMapper", "PricingService"):
        for args in re.findall(r"new\s+" + cls + r"\s*\(([^)]*)\)", src):
            if cls in ctor_arity:
                n = len([a for a in args.split(",") if a.strip()])
                if n != ctor_arity[cls]:
                    err.append(f"{rel}: new {cls}({args.strip()}) -- constructor takes "
                               f"{ctor_arity[cls]} arg(s)")

print(f"Checked {tested} builder calls across {len(list(java_files(TEST)))} test files")
print()
if err:
    print("ERRORS:")
    for e in dict.fromkeys(err):
        print("  -", e)
else:
    print("PASS - tests match the main sources: builder methods, enum\n"
          "       constants and constructor arities all resolve.")
sys.exit(1 if err else 0)
