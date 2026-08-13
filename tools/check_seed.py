"""Seed data vs schema: columns, NOT NULL coverage, PKs, FKs, image counts."""
import os, re, sys
B=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))+"/backend"
schema=re.sub(r'--.*','',open(B+"/emart_schema.sql").read()); seed=open(B+"/emart_seed_data.sql").read()
tables={}
for m in re.finditer(r'CREATE TABLE\s+(\w+)\s*\((.*?)\n\)\s*ENGINE',schema,re.S|re.I):
    t=m.group(1);cols={};fks=[];pk=None;d=0;cur="";parts=[]
    for ch in m.group(2):
        if ch=='(': d+=1
        elif ch==')': d-=1
        if ch==',' and d==0: parts.append(cur);cur=""
        else: cur+=ch
    if cur.strip(): parts.append(cur)
    for p in parts:
        p=p.strip()
        if not p: continue
        if p.upper().startswith("CONSTRAINT"):
            f=re.search(r'FOREIGN KEY\s*\((\w+)\)\s*REFERENCES\s+(\w+)\s*\((\w+)\)',p,re.I)
            if f: fks.append((f.group(1),f.group(2),f.group(3)))
            continue
        if p.upper().startswith(("INDEX","KEY","PRIMARY","UNIQUE")): continue
        cm=re.match(r'(\w+)\s+[A-Z]',p,re.I)
        if not cm: continue
        n=cm.group(1);rest=p[cm.end():]
        cols[n]={"nn":bool(re.search(r'NOT NULL',rest,re.I)) or 'PRIMARY KEY' in rest.upper(),
                 "def":bool(re.search(r'DEFAULT',rest,re.I)),
                 "auto":bool(re.search(r'AUTO_INCREMENT',rest,re.I))}
        if 'PRIMARY KEY' in rest.upper(): pk=n
    tables[t]={"cols":cols,"fks":fks,"pk":pk}
def tup(b):
    o=[];d=0;c="";ins=False
    for ch in b:
        if ins:
            c+=ch
            if ch=="'": ins=False
            continue
        if ch=="'": ins=True;c+=ch;continue
        if ch=='(':
            d+=1
            if d==1: c="";continue
        if ch==')':
            d-=1
            if d==0: o.append(c);continue
        if d>=1: c+=ch
    return o
def vals(t):
    o=[];c="";ins=False;d=0
    for ch in t:
        if ins:
            c+=ch
            if ch=="'": ins=False
            continue
        if ch=="'": ins=True;c+=ch;continue
        if ch=='(': d+=1;c+=ch;continue
        if ch==')': d-=1;c+=ch;continue
        if ch==',' and d==0: o.append(c.strip());c="";continue
        c+=ch
    if c.strip(): o.append(c.strip())
    return o
err=[];pks={t:set() for t in tables};rows={}
for m in re.finditer(r'INSERT INTO\s+(\w+)\s*\(([^)]*)\)\s*VALUES(.*?);',seed,re.S|re.I):
    t=m.group(1);cl=[c.strip() for c in m.group(2).split(',')]
    if t not in tables: err.append(f"unknown table {t}");continue
    sc=tables[t]["cols"]
    for c in cl:
        if c not in sc: err.append(f"[{t}] column '{c}' not in schema")
    for c,i in sc.items():
        if i["nn"] and not i["def"] and not i["auto"] and c not in cl:
            err.append(f"[{t}] NOT NULL '{c}' missing from INSERT")
    rows.setdefault(t,[])
    for tt in tup(m.group(3)):
        v=vals(tt)
        if len(v)!=len(cl): err.append(f"[{t}] col/value count mismatch");continue
        r=dict(zip(cl,v));rows[t].append(r)
        pk=tables[t]["pk"]
        if pk and pk in r:
            if r[pk] in pks[t]: err.append(f"[{t}] duplicate PK {r[pk]}")
            pks[t].add(r[pk])
for t,rr in rows.items():
    for r in rr:
        for (fc,rt,rc) in tables[t]["fks"]:
            if fc in r and r[fc].upper()!='NULL' and r[fc] not in pks.get(rt,set()):
                err.append(f"[{t}] FK {fc}={r[fc]} -> no such {rt}.{rc}")
print("rows per table:")
for t in tables:
    if t in rows: print(f"   {t:18s} {len(rows[t])}")
from collections import defaultdict
imgs=defaultdict(list)
for r in rows.get("product_image",[]): imgs[r["prod_id"]].append(r)
prods={r["prod_id"] for r in rows.get("product_master",[])}
noimg=[p for p in prods if p not in imgs]
bad=[p for p,v in imgs.items() if not (4<=len(v)<=6)]
prim=[p for p,v in imgs.items() if sum(1 for x in v if x["is_primary"]=="1")!=1]
print(f"\nproducts {len(prods)} | with images {len(imgs)} | none {len(noimg)} | outside 4-6 {len(bad)} | not exactly 1 primary {len(prim)}")
if noimg: err.append(f"{len(noimg)} products have no images")
if bad: err.append(f"{len(bad)} products outside the 4-6 range")
if prim: err.append(f"{len(prim)} products lack exactly one primary image")
# ---------------------------------------------------------------- pricing
# NULL is legal on every member-offer column: it means "this product does not
# carry that offer", which is what makes the product card hide the checkbox.
def num(v):
    return None if v is None or v.upper() == "NULL" else float(v)

prods_rows = rows.get("product_master", [])
bp = [r for r in prods_rows
      if num(r["cardholder_price"]) is not None
      and num(r["cardholder_price"]) > num(r["mrp_price"])]
if bp: err.append(f"{len(bp)} products have cardholder_price > mrp_price")

# A half-filled hybrid offer is meaningless - the schema CHECK forbids it, so
# catch it here too rather than at INSERT time on the user's machine.
halfhy = [r for r in prods_rows
          if (num(r["hybrid_cash_price"]) is None) != (num(r["hybrid_points"]) is None)]
if halfhy: err.append(f"{len(halfhy)} products set only one half of the hybrid offer")

# Charm pricing must be gone: every cash price a clean round number.
def charmy(v):
    return v is not None and (v != int(v) or int(v) % 10 != 0)

charm = [r for r in prods_rows
         if charmy(num(r["mrp_price"]))
         or charmy(num(r["cardholder_price"]))
         or charmy(num(r["hybrid_cash_price"]))]
if charm: err.append(f"{len(charm)} products still have charm prices (e.g. {charm[0]['mrp_price']})")

# Points offers only make sense if a seeded user could actually afford one.
MAX_SEEDED_BALANCE = 15000
steep = [r for r in prods_rows
         if num(r["points_price"]) is not None and num(r["points_price"]) > MAX_SEEDED_BALANCE]
if steep: err.append(f"{len(steep)} products cost more points than any seeded user holds")

with_offer = sum(1 for r in prods_rows if any(
    num(r[c]) is not None for c in ("cardholder_price", "points_price", "hybrid_cash_price")))
print(f"pricing  | member {sum(1 for r in prods_rows if num(r['cardholder_price']) is not None)}"
      f" | points {sum(1 for r in prods_rows if num(r['points_price']) is not None)}"
      f" | hybrid {sum(1 for r in prods_rows if num(r['hybrid_cash_price']) is not None)}"
      f" | no offer {len(prods_rows)-with_offer}")
if with_offer == 0: err.append("no product carries any member offer")
if with_offer == len(prods_rows):
    err.append("every product carries an offer - nothing exercises the hide-the-checkboxes path")
print()
if err:
    print("ERRORS:");[print("  -",e) for e in err[:20]]
else: print("PASS - seed data consistent with emart_schema.sql")
sys.exit(1 if err else 0)
