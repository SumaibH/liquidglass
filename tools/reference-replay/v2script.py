# Inferred finger script for video2 from the reference's selected-ink centroid. Local only.
# Everything about the finger is INFERRED. A held press begins HOLD before the ink starts its held
# shift: finger-down is invisible in the recording, so the visible onset is aligned with ours by
# construction (our 120 ms hold threshold plus a frame); an un-held departure begins 50 ms
# early. The finger follows the ink centroid while it travels. A throw releases when the ink has
# settled within 22 px of a tab for 3 frames: into the middle tab the finger stops first; into
# an end tab it lifts still moving, because the ink stops at the tab when the lens meets the
# wall and cannot show the finger beyond it (2026-10-14). A stationary hold lifts where the
# bar starts to return (the bar is pressed only while touched: its far edge, on a row above the
# search-button neck, from v2-row495.gray / v2-pts.txt when present), less our own bar's
# release latency of 43 ms; without those files, 50 ms before the held shift disappears.
# The finger leads the ink by LEAD (33 ms) through every throw: the selected-ink centroid trails
# the lens's leading edge (the lens reaches the end tab about three frames before the ink does);
# 33 ms is the lead the ten right-wall landings fit best (2026-10-14).
# 0.35 s after each release is ignored (landing recovery is not a new press). The
# held-reversal episode is spliced in from reversal-script.txt.
import csv, os
BAR = None
if os.path.exists('v2-row495.gray') and os.path.exists('v2-pts.txt'):
    import numpy as np
    BAR = (np.fromfile('v2-row495.gray', np.uint8).reshape(-1, 6, 1170),
           np.array([float(l.strip().strip(',')) for l in open('v2-pts.txt') if l.strip()]))
BAR_LATENCY = 0.043
def bar_release(t_end, finger_x):
    """The first frame the bar's far edge returns toward rest by 2 px and keeps going, within
    0.3 s before the held shift disappears; None if it cannot be seen."""
    if BAR is None: return None
    rows, pts = BAR
    ks = [k for k in range(len(pts)) if t_end - .3 <= pts[k] <= t_end + .05]
    def edge(k):
        r = rows[k, 0:5].astype(float).mean(0)
        if finger_x < 480:
            x = 925
            while x > 600 and r[x] <= 20: x -= 1
            return -x
        return next((x for x in range(0, 300) if r[x] > 20), -1)
    e = [edge(k) for k in ks]
    for i in range(1, len(e) - 2):
        if e[i] - e[i-1] >= 2 and e[i+1] - e[i] >= 1 and e[i+2] - e[i+1] >= 1:
            return pts[ks[i-1]]
    return None
r=[(float(x['t']),float(x['cx'])) for x in csv.DictReader(open('v2.csv'))]
REST=[217,479,742]; Y=546; N=len(r)
cxs=[c for _,c in r]
med=[sorted(cxs[max(0,i-1):i+2])[len(cxs[max(0,i-1):i+2])//2] for i in range(N)]
def tab(cx,tol):
    k=min(range(3),key=lambda k:abs(cx-REST[k])); return k if abs(cx-REST[k])<=tol else None
def settled(i,tol=22):
    ks={tab(med[j],tol) for j in range(i,min(N,i+3))}
    k=ks.pop() if len(ks)==1 and None not in ks else None
    if k==1 and any(tab(med[j],tol)!=1 for j in range(i,min(N,i+8))): return None
    return k
REV=(18.7,22.3)
HOLD=0.137
LEAD=0.033
WALL=[63,897]
ev=[]; cat=[]; at=0; i=0; quiet=-1
while i<N:
    t,cx=r[i][0],med[i]
    if t<quiet or tab(cx,3.5)==at:
        if t>=quiet and tab(cx,3.5) is not None: at=tab(cx,3.5)
        i+=1; continue
    # press begins
    held = abs(cx-REST[at])<20
    start=t; origin=at
    ev.append((t-(HOLD if held else 0.05),'down',REST[origin]))
    moved=False; j=i
    while j<N:
        tj,cj=r[j][0],med[j]
        rev = REV[0]<start<REV[1]
        if abs(cj-REST[origin])>40: moved=True
        if moved:
            ev.append((tj,'move',min(max(cj,REST[0]),REST[2])))
            d=settled(j)
            if d is not None and (d!=origin or rev) and not (rev and tj<REV[1]) and abs(cj-REST[origin])>40 or (d is not None and rev and tj>=REV[1]):
                xs=min(max(cj,REST[0]),REST[2])
                if d in (0,2) and not rev:
                    # Into a wall: lift one frame after the ink first reaches the end tab, at the
                    # speed it arrived with; the confirming frames are the lens against the wall.
                    k=j
                    while k>i+1 and (med[k-1]-REST[d])*(1 if d==2 else -1)>=-22: k-=1
                    tk=r[k][0]
                    while ev and ev[-1][1]=='move' and ev[-1][0]>tk: ev.pop()
                    v=(med[k]-med[k-1])/max(tk-r[k-1][0],1e-3)
                    xw=min(max(med[k]+v*0.017,WALL[0]),WALL[1])
                    ev.append((tk,'move',min(max(med[k],REST[0]),REST[2])))
                    ev.append((tk+0.016,'move',xw)); ev.append((tk+0.017,'up',xw))
                else:
                    # The lens stops dead at the middle tab, so the finger stops before lifting.
                    ev.append((tj+0.017,'move',xs)); ev.append((tj+0.033,'move',xs)); ev.append((tj+0.034,'up',xs))
                kind='held reversals' if rev else ('formed throw' if tj-start>0.3 else 'early throw')
                cat.append(f"{start:6.2f}-{tj:6.2f}  {kind:15s} {origin}->{d}" + ("  wall" if d in (0,2) and not rev else "  intermediate" if not rev else ""))
                at=d; quiet=tj+0.35; break
        elif all(tab(med[q],3.5)==origin for q in range(j,min(N,j+4))):
            onset = bar_release(tj, REST[origin])
            ev.append(((onset - BAR_LATENCY) if onset is not None else tj-0.05,'up',REST[origin],'stationary'))
            cat.append(f"{start:6.2f}-{tj:6.2f}  stationary hold {origin}")
            quiet=tj+0.1; break
        j+=1
    i=j+1
# Throw moves and lifts lead the ink; a stationary hold's bar-timed lift is already the finger's.
ev=[(t-LEAD,k,x) if k=='move' or (k=='up' and not stationary) else (t,k,x) for t,k,x,stationary in
    [(e[0],e[1],e[2],len(e)>3) for e in ev]]
for c in cat: print(c)
# Splice the rebuilt held-reversal episode (its times are relative to 18.5 s).
ev=[e for e in ev if not 18.5<=e[0]<22.6]
for line in open('reversal-script.txt'):
    w=line.split()
    if len(w)==4: ev.append(((int(w[0])-(HOLD-0.017)*1000*(w[1]=='down'))/1000+18.5,w[1],float(w[2])))
ev.sort()
with open('v2-script.txt','w') as f:
    f.write("select 0\n")
    for t,k,x in ev: f.write(f"{t*1000:.0f} {k} {x:.1f} {Y}\n")
    f.write(f"end {r[-1][0]*1000:.0f}\n")
open('v2-catalogue.txt','w').write("\n".join(cat)+"\n")
