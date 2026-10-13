import csv,sys
r=[(float(x["t"]),float(x["cx"]),int(x["n"])) for x in csv.DictReader(open(sys.argv[1]))]
tabs=[215,478,742]
def tab(cx):
    for i,c in enumerate(tabs):
        if abs(cx-c)<14: return i
    return None
state=None; last_t=None; out=[]
for t,cx,n in r:
    k=tab(cx) if cx>0 else None
    if state is None and k is not None: state=k; stable=t; continue
    if k is not None and k==state: last_t=t
    elif k is None and state is not None and last_t is not None and t-last_t<0.2:
        pass
    if k is not None and k!=state:
        out.append((round(last_t,3),state,k,round(t,3))); state=k
    if k==state: last_t=t
for o in out: print("leave %.3f  %d->%d  arrive %.3f"%o)
