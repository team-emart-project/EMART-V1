"""Structure, imports, interface contracts, constructor integrity, entity<->schema."""
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
        if c=="'":
            i+=1
            while i<n and src[i]!="'": i+=2 if src[i]=='\\' else 1
            i+=1; out.append(" '' "); continue
        if src.startswith('//',i):
            j=src.find('\n',i); i=n if j==-1 else j; continue
        if src.startswith('/*',i):
            j=src.find('*/',i+2); i=n if j==-1 else j+2; out.append(' '); continue
        out.append(c); i+=1
    return ''.join(out)

def bal(s,i,o,c):
    d=0
    while i<len(s):
        if s[i]==o: d+=1
        elif s[i]==c:
            d-=1
            if d==0: return i
        i+=1
    return -1

errors=[]; declared={}; files=[]
for base in ["src/main/java","src/test/java"]:
    for dp,_,fns in os.walk(os.path.join(ROOT,base)):
        for fn in fns:
            if fn.endswith(".java"): files.append((os.path.join(dp,fn),base))

for path,base in files:
    code=strip(open(path).read()); rel=os.path.relpath(path,os.path.join(ROOT,base))
    exp=os.path.dirname(rel).replace(os.sep,".").replace("/",".")
    m=re.search(r'^\s*package\s+([\w.]+)\s*;',code,re.M)
    if not m: errors.append(f"{rel}: no package"); continue
    if m.group(1)!=exp: errors.append(f"{rel}: package != directory")
    declared[f"{m.group(1)}.{os.path.basename(path)[:-5]}"]=rel
    for o,c,l in [('{','}','braces'),('(',')','parens')]:
        if code.count(o)!=code.count(c): errors.append(f"{rel}: unbalanced {l}")

for path,base in files:
    code=strip(open(path).read()); rel=os.path.relpath(path,os.path.join(ROOT,base))
    for imp in re.findall(r'^\s*import\s+(?:static\s+)?(com\.example\.demo\.[\w.]+)\s*;',code,re.M):
        if imp not in declared and '.'.join(imp.split('.')[:-1]) not in declared:
            errors.append(f"{rel}: imports missing '{imp}'")

for i,mi in [("CartService","CartServiceImpl"),("CategoryService","CategoryServiceImpl"),
             ("ProductService","ProductServiceImpl"),("AuthService","AuthServiceImpl"),
             ("UserService","UserServiceImpl"),("AddressService","AddressServiceImpl"),
             ("EmartCardService","EmartCardServiceImpl"),("HomeService","HomeServiceImpl"),
             ("WishlistService","WishlistServiceImpl"),("OrderService","OrderServiceImpl"),
             ("PaymentService","PaymentServiceImpl")]:
    ifc=strip(open(f"{SRC}/service/interfaces/{i}.java").read())
    imp=strip(open(f"{SRC}/service/implementation/{mi}.java").read())
    names=set(re.findall(r'\b(\w+)\s*\([^)]*\)\s*;',ifc))
    ovr=[o[1] for o in re.findall(r'@Override\b((?:\s*@\w+(?:\([^)]*\))?)*)\s+[\w.<>,\s\[\]]+?\s(\w+)\s*\(',imp)]
    miss=names-set(ovr)
    if miss: errors.append(f"{mi}: not implemented {sorted(miss)}")

for dp,_,fns in os.walk(SRC):
    for fn in fns:
        if not fn.endswith(".java"): continue
        p=os.path.join(dp,fn); s=open(p).read(); cname=fn[:-5]
        fields=[m.group(1) for m in re.finditer(r'^\s*private\s+final\s+[\w<>,\.\[\]\?\s]+?\s(\w+)\s*;\s*$',s,re.M)]
        if not fields: continue
        # Constructors of NESTED classes count too. Scanning only "public
        # <FileName>(" missed them, so a nested class with its own final field
        # was reported as never assigned - a false positive. A checker that
        # cries wolf gets ignored, and an ignored checker is worse than none.
        nested=set(re.findall(r'\b(?:class|record)\s+(\w+)',s))
        nested.add(cname)
        ctor_pat=r'(?:public|private|protected|\s)\s*(?:'+'|'.join(re.escape(c) for c in sorted(nested))+r')\s*\('

        params=set(); assigned=set(); derived=set(); n=0
        for m in re.finditer(ctor_pat,s):
            op=m.end()-1; cp=bal(s,op,'(',')')
            if cp==-1: continue
            ob=s.find('{',cp)
            if ob==-1: continue
            cb=bal(s,ob,'{','}'); n+=1
            pl=re.sub(r'@\w+\s*\([^)]*\)','',s[op+1:cp]); pl=re.sub(r'@\w+','',pl)
            for part in pl.split(','):
                part=part.strip()
                if part: params.add(part.split()[-1])
            for am in re.finditer(r'this\.(\w+)\s*=\s*([^;]+);', s[ob:cb]):
                assigned.add(am.group(1))
                if am.group(2).strip()!=am.group(1): derived.add(am.group(1))
        if n==0: continue
        un=[f for f in fields if f not in assigned]
        nd=[a for a in assigned if a in fields and a not in params and a not in derived]
        if un: errors.append(f"{fn}: final field never assigned {un}")
        if nd: errors.append(f"{fn}: assigned but not a ctor param {nd}")

schema=re.sub(r'--.*','',open(f"{ROOT}/emart_schema.sql").read())
tabs={}
for m in re.finditer(r'CREATE TABLE\s+(\w+)\s*\((.*?)\n\)\s*ENGINE',schema,re.S|re.I):
    cols=set()
    for line in m.group(2).split('\n'):
        line=line.strip().rstrip(',')
        if not line or line.upper().startswith(('CONSTRAINT','FOREIGN','PRIMARY','UNIQUE','KEY','INDEX')): continue
        cm=re.match(r'(\w+)\s+[A-Z]',line,re.I)
        if cm: cols.add(cm.group(1).lower())
    tabs[m.group(1)]=cols
ents=0
for fn in os.listdir(f"{SRC}/entity"):
    s=open(f"{SRC}/entity/{fn}").read()
    tm=re.search(r'@Table\(\s*name\s*=\s*"(\w+)"',s)
    if not tm: continue
    t=tm.group(1); ents+=1
    if t not in tabs: errors.append(f"{fn}: table {t} not in schema"); continue
    for c in set(re.findall(r'@(?:Column|JoinColumn)\(\s*name\s*=\s*"(\w+)"',s)):
        if c.lower() not in tabs[t]: errors.append(f"{fn}: column '{c}' not in {t}")

print(f"java files {len(files)}  types {len(declared)}  entities {ents}  tables {len(tabs)}")
print()
if errors:
    print("ERRORS:"); [print("  -",e) for e in errors[:25]]
else:
    print("PASS - packages, balance, imports, 11 interface contracts,")
    print("       constructor integrity, entity columns vs schema.")
sys.exit(1 if errors else 0)
