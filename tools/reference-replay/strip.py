# strip.py <ref.mp4> <frames-dir> <t0> <t1> <step> <out.png>: paired bar crops, reference above ours.
import sys,subprocess,io
from PIL import Image, ImageDraw
ref,fr,t0,t1,step,out=sys.argv[1],sys.argv[2],float(sys.argv[3]),float(sys.argv[4]),float(sys.argv[5]),sys.argv[6]
offset=float(sys.argv[7]) if len(sys.argv)>7 else 0.0
Y0,Y1=400,690; S=0.5
ts=[]; t=t0
while t<=t1+1e-9: ts.append(round(t,4)); t+=step
cols=4; w=int(1170*S); h=int((Y1-Y0)*S)
rows=(len(ts)+cols-1)//cols
sheet=Image.new('RGB',(cols*w,rows*(2*h+18)),(40,40,40)); d=ImageDraw.Draw(sheet)
for i,t in enumerate(ts):
    png=subprocess.run(["ffmpeg","-v","error","-ss",str(t),"-i",ref,"-frames:v","1","-f","image2pipe","-vcodec","png","-"],capture_output=True).stdout
    a=Image.open(io.BytesIO(png)).convert('RGB').crop((0,Y0,1170,Y1)).resize((w,h))
    b=Image.open(f"{fr}/f{round((t-offset)*60):05d}.png").convert('RGB').crop((0,Y0,1170,Y1)).resize((w,h))
    x=(i%cols)*w; y=(i//cols)*(2*h+18)
    d.text((x+4,y+2),f"t={t:.3f}s",fill=(255,255,0)); sheet.paste(a,(x,y+18)); sheet.paste(b,(x,y+18+h))
sheet.save(out)
