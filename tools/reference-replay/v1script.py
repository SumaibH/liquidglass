# Inferred tap script for video1: a 60 ms tap on the destination tab, pressing when the
# reference's selected ink starts to leave (motion onset). Finger timing is INFERRED.
import subprocess,sys
lines=subprocess.run([sys.executable,"events.py","v1.csv"],capture_output=True,text=True).stdout.split("\n")
ev=[]
for l in lines:
    p=l.split()
    if not p: continue
    ev.append((float(p[1]),int(p[2].split("->")[0]),int(p[2].split("->")[1]),float(p[4])))
# merge pass-through of the middle tab
taps=[]
for t,a,b,arr in ev:
    if taps and taps[-1][2]==1 and a==1 and t-taps[-1][3]<0.12:
        taps[-1]=(taps[-1][0],taps[-1][1],b,arr); continue
    taps.append((t,a,b,arr))
t0=float(sys.argv[1]); t1=float(sys.argv[2])
cent=[(21+278*(0.5+i)/3) for i in range(3)]  # rough slot centres in pt; refined below
# slot centres in px from the reference ink: 215, 478, 742
cx=[215,478,742]
out=["select 0"]
for t,a,b,arr in taps:
    if t<t0 or t>t1: continue
    ms=(t-t0)*1000
    out+= [f"{ms:.0f} down {cx[b]} 546", f"{ms+60:.0f} up {cx[b]} 546"]
    print(f"# tap {a}->{b} at {t:.3f}s", file=sys.stderr)
out.append(f"end {(t1-t0)*1000:.0f}")
open(sys.argv[3],"w").write("\n".join(out)+"\n")
