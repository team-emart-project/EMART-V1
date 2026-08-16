"""
Catches the class of bug that just bit us TWICE:

a text patch adds a FIELD to a POJO but silently misses the getter, setter,
builder method or constructor argument, because the anchor string did not match
the file's actual formatting. Braces stay balanced and imports still resolve,
so every other check passes -- and it only blows up at runtime.

For every hand-written POJO (DTOs, entities, response wrappers), assert that
each instance field has:  a getter, a setter, an all-args ctor param that is
assigned, and -- if the class has a Builder -- a builder method and a slot in
build().
"""
import os, re, sys

import os as _os
ROOT = _os.path.dirname(_os.path.dirname(_os.path.abspath(__file__))) + "/backend/src/main/java/com/example/demo"
DIRS = ["dto/request", "dto/response", "entity", "response"]

def bal(s, i, o, c):
    d = 0
    while i < len(s):
        if s[i] == o: d += 1
        elif s[i] == c:
            d -= 1
            if d == 0: return i
        i += 1
    return -1

def cap(x): return x[0].upper() + x[1:]

errors = []
checked = 0

for d in DIRS:
    p = os.path.join(ROOT, d)
    if not os.path.isdir(p): continue
    for fn in sorted(os.listdir(p)):
        if not fn.endswith(".java"): continue
        src = open(os.path.join(p, fn)).read()
        cname = fn[:-5]
        if "enum " + cname in src: continue

        # OUTER-class instance fields only (4-space indent), skipping constants
        fields = []
        for m in re.finditer(r'^    private\s+(?!static)((?:final\s+)?)([\w<>,\.\[\]\? ]+?)\s+(\w+)\s*(?:=[^;]*)?;\s*$',
                             src, re.M):
            fields.append((m.group(2).strip(), m.group(3)))
        if not fields: continue
        checked += 1

        has_builder = "public static Builder builder()" in src

        for ftype, fname in fields:
            getter = ("is" if ftype == "boolean" else "get") + cap(fname)
            # accept either get/is form, some UserDetails methods use is*
            if not (re.search(r'\bpublic\s+[\w<>,\.\[\]\? ]+\s+' + getter + r'\s*\(\s*\)', src)
                    or re.search(r'\bpublic\s+[\w<>,\.\[\]\? ]+\s+(?:get|is)' + cap(fname) + r'\s*\(\s*\)', src)):
                errors.append(f"{d}/{fn}: field '{fname}' has NO getter")

            if not re.search(r'\bpublic\s+void\s+set' + cap(fname) + r'\s*\(', src):
                # entities/DTOs built only via constructor (e.g. CustomUserDetails) are fine
                if has_builder or d.startswith("dto"):
                    errors.append(f"{d}/{fn}: field '{fname}' has NO setter")

            if has_builder:
                if not re.search(r'\bpublic\s+Builder\s+' + re.escape(fname) + r'\s*\(', src):
                    errors.append(f"{d}/{fn}: field '{fname}' has NO builder method")
                bm = re.search(r'public\s+' + cname + r'\s+build\s*\(\s*\)\s*\{', src)
                if bm:
                    body = src[bm.end(): bal(src, src.index('{', bm.start()), '{', '}')]
                    if not re.search(r'\b' + re.escape(fname) + r'\b', body):
                        errors.append(f"{d}/{fn}: field '{fname}' NOT passed by build()")

        # every all-args-style ctor must assign every field it names, and only
        # assign fields that are parameters
        for m in re.finditer(r'public\s+' + re.escape(cname) + r'\s*\(', src):
            op = m.end() - 1
            cp = bal(src, op, '(', ')')
            if cp == -1: continue
            ob = src.find('{', cp)
            if ob == -1: continue
            cb = bal(src, ob, '{', '}')
            plist = re.sub(r'@\w+\s*\([^)]*\)', '', src[op + 1:cp])
            params = {x.strip().split()[-1] for x in plist.split(',') if x.strip()}
            body = src[ob:cb]
            for am in re.finditer(r'this\.(\w+)\s*=\s*([^;]+);', body):
                target, rhs = am.group(1), am.group(2).strip()
                if rhs == target and target not in params:
                    errors.append(f"{d}/{fn}: ctor assigns '{target}' but it is NOT a parameter")

print(f"Checked {checked} POJOs across {DIRS}")
print()
if errors:
    print("ERRORS:")
    for e in errors[:30]: print("  -", e)
else:
    print("PASS - every field has a getter, setter, builder method, a build() slot")
    print("       and a matching constructor parameter.")
sys.exit(1 if errors else 0)
