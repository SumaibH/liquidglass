# Track the selected (blue) ink centroid along the bar in a reference video. Local only.
import sys, subprocess, numpy as np, csv
src, out = sys.argv[1], sys.argv[2]
W, H = 1170, 702
p = subprocess.Popen(["ffmpeg","-v","error","-i",src,"-fps_mode","passthrough","-f","rawvideo","-pix_fmt","rgb24","-"],stdout=subprocess.PIPE)
pts = subprocess.run(["ffprobe","-v","error","-select_streams","v","-show_entries","frame=pts_time","-of","csv=p=0",src],capture_output=True,text=True).stdout.replace(",","").split()
rows=[]; i=0
while True:
    b = p.stdout.read(W*H*3)
    if len(b) < W*H*3: break
    f = np.frombuffer(b, np.uint8).reshape(H,W,3)[380:702, 0:930].astype(int)
    r,g,bl = f[...,0],f[...,1],f[...,2]
    m = (bl > 120) & (bl - r > 35) & (bl - g > 15)
    n = int(m.sum())
    if n > 30:
        ys,xs = np.nonzero(m); cx=float(xs.mean()); cy=float(ys.mean())+380; x0=int(xs.min()); x1=int(xs.max())
    else: cx=cy=x0=x1=-1
    rows.append((float(pts[i]) if i < len(pts) else i/60, n, cx, cy, x0, x1)); i+=1
with open(out,"w",newline="") as fh:
    w=csv.writer(fh); w.writerow(["t","n","cx","cy","x0","x1"]); w.writerows(rows)
print(len(rows))
