"""
Cross-object method-call check.

Catches the failure mode that produced `setImages(...) is undefined`: code
calls a method on a collaborator (mapper, repository, service, util) that does
not exist on that type. Braces balance and imports resolve, so nothing else
notices until Spring dispatches a request.

For every class, we resolve each `field.method(` call to the field's declared
type and assert that type really declares `method`. Types we cannot resolve
(JDK, Spring, third-party) are skipped rather than guessed at.
"""
import os, re, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + "/backend"
SRC  = ROOT + "/src/main/java/com/example/demo"

def strip(src):
    out=[];i=0;n=len(src)
    while i<n:
        c=src[i]
        if src.startswith('"""',i):
            j=src.find('"""',i+3); i=(j+3) if j!=-1 else n; out.append(' "" '); continue
        if c=='"':
            i+=1
            while i<n and src[i]!='"': i+=2 if src[i]=='\\' else 1
            i+=1; out.append(' "" '); continue
        if src.startswith('//',i):
            j=src.find('\n',i); i=n if j==-1 else j; continue
        if src.startswith('/*',i):
            j=src.find('*/',i+2); i=n if j==-1 else j+2; out.append(' '); continue
        out.append(c); i+=1
    return ''.join(out)

# ---- index every project class: simple name -> {methods}
methods, files = {}, {}
for dp,_,fns in os.walk(SRC):
    for fn in fns:
        if not fn.endswith(".java"): continue
        p=os.path.join(dp,fn); s=strip(open(p).read()); cname=fn[:-5]
        files[cname]=os.path.relpath(p,SRC)
        m=set(re.findall(r'\b(?:public|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>,\.\[\]\?\s]+?\s(\w+)\s*\(', s))
        # Interface / abstract methods have no body. Parameter lists can span
        # lines and contain nested parens (@Param("x")), so match by brace-free
        # balance rather than [^)]*.
        for mm in re.finditer(r'\b(\w+)\s*\(', s):
            name, i, d = mm.group(1), mm.end()-1, 0
            while i < len(s):
                if s[i] == '(': d += 1
                elif s[i] == ')':
                    d -= 1
                    if d == 0: break
                i += 1
            tail = s[i+1:i+3]
            if tail.lstrip().startswith(';'):
                m.add(name)
        methods[cname]=m

# JpaRepository / CrudRepository built-ins + Spring Data derived queries
JPA = {"save","saveAll","findById","findAll","findAllById","deleteById","delete",
       "deleteAll","deleteAllById","count","existsById","flush","saveAndFlush","getReferenceById"}

errors=[]; checked=0
for dp,_,fns in os.walk(SRC):
    for fn in fns:
        if not fn.endswith(".java"): continue
        p=os.path.join(dp,fn); s=strip(open(p).read())

        # field name -> declared simple type  (private final Foo bar;)
        ftypes={}
        for m in re.finditer(r'\bprivate\s+(?:final\s+)?([A-Z][\w]*)(?:<[^>]*>)?\s+(\w+)\s*[;=]', s):
            ftypes[m.group(2)]=m.group(1)

        for m in re.finditer(r'\b(\w+)\.(\w+)\s*\(', s):
            obj, meth = m.group(1), m.group(2)
            t = ftypes.get(obj)
            if not t or t not in methods:      # unknown / external type -> skip
                continue
            checked+=1
            known = methods[t]
            # repositories: JPA built-ins + anything Spring Data derives
            if t.endswith("Repository"):
                if meth in JPA or meth in known: continue
                if re.match(r'^(find|count|exists|delete|remove|read|get|stream|search)[A-Z]', meth): continue
                errors.append(f"{files[fn[:-5]]}: {obj}.{meth}() not on {t}")
            elif meth not in known:
                errors.append(f"{files[fn[:-5]]}: {obj}.{meth}() not declared on {t}")

print(f"Resolved and checked {checked} method calls across {len(methods)} classes")
print()
if errors:
    print("ERRORS:")
    for e in sorted(set(errors))[:30]: print("  -", e)
else:
    print("PASS - every resolvable method call exists on its target type.")
sys.exit(1 if errors else 0)
