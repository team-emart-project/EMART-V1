"""Frontend: imports resolve, named exports exist, no unused imports."""
import os, re, sys, json
BASE=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))+"/frontend"
SRC=BASE+"/src"
errors=[];files=[]
for dp,_,fns in os.walk(SRC):
    for fn in fns:
        if fn.endswith(('.js','.jsx')): files.append(os.path.join(dp,fn))
def resolve(spec,frm):
    base=os.path.join(SRC,spec[2:]) if spec.startswith('@/') else os.path.normpath(os.path.join(os.path.dirname(frm),spec))
    for c in (base,base+'.js',base+'.jsx',os.path.join(base,'index.js'),os.path.join(base,'index.jsx')):
        if os.path.isfile(c): return os.path.normpath(c)
    return None
pkg=json.load(open(BASE+"/package.json"))
deps=set(pkg['dependencies'])|set(pkg['devDependencies']);used=set()
exports={}
for f in files:
    s=open(f).read();names=set()
    if re.search(r'export\s+default\b',s): names.add('default')
    names|=set(re.findall(r'export\s+(?:const|let|function|class)\s+(\w+)',s))
    for blk in re.findall(r'export\s+const\s*\{([^}]*)\}\s*=',s):
        for p in blk.split(','):
            p=p.strip()
            if p: names.add(p.split(':')[-1].strip())
    for blk in re.findall(r'export\s*\{([^}]*)\}',s):
        for p in blk.split(','):
            p=p.strip()
            if p: names.add(p.split(' as ')[-1].strip())
    exports[os.path.normpath(f)]=names
for f in files:
    s=open(f).read();rel=os.path.relpath(f,SRC)
    for m in re.finditer(r'import\s+([^;]*?)\s+from\s+[\'"]([^\'"]+)[\'"]',s,re.S):
        clause,spec=m.group(1),m.group(2)
        if not spec.startswith(('@/','.')):
            used.add(spec.split('/')[0] if not spec.startswith('@') else '/'.join(spec.split('/')[:2]));continue
        real=resolve(spec,f)
        if not real: errors.append(f"{rel}: cannot resolve '{spec}'");continue
        want=set()
        if not clause.strip().startswith('{'): want.add('default')
        for blk in re.findall(r'\{([^}]*)\}',clause):
            for p in blk.split(','):
                p=p.strip()
                if p: want.add(p.split(' as ')[0].strip())
        for w in want:
            if w not in exports.get(real,set()):
                errors.append(f"{rel}: '{w}' not exported by {os.path.relpath(real,SRC)}")
for p in sorted(used-deps): errors.append(f"package '{p}' missing from package.json")
STMT=re.compile(r'^\s*import\s+(?:([\w$]+)\s*,\s*)?(?:\{([^}]*)\}|([\w$]+)|\*\s+as\s+([\w$]+))\s+from\s+[\'"][^\'"]+[\'"]\s*;?',re.M)
for f in files:
    s=open(f).read();rel=os.path.relpath(f,SRC);names=[]
    for m in STMT.finditer(s):
        if m.group(1): names.append(m.group(1))
        if m.group(2): names+=[x.strip().split(' as ')[-1] for x in m.group(2).split(',') if x.strip()]
        if m.group(3): names.append(m.group(3))
        if m.group(4): names.append(m.group(4))
    body=STMT.sub('',s)
    for n in names:
        if not re.search(r'\b'+re.escape(n)+r'\b',body):
            errors.append(f"{rel}: unused import '{n}'")
print(f"Scanned {len(files)} files")
print()
if errors:
    print("ERRORS:");[print("  -",e) for e in errors[:20]]
else: print("PASS - imports resolve, exports exist, no unused imports.")
sys.exit(1 if errors else 0)
