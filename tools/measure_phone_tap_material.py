"""Measure clean source patches and the complete Contacts icon during a short tap.

Uses the existing native decode, validates every image hash, and retains all RGB
samples. This records visible output, not the hidden backdrop beneath the bar.
NumPy/Pillow required. Overlays must be inspected before accepting measurements.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw


def main(directory, geometry, output):
    if output.exists():
        raise ValueError('Use a new output directory; retain earlier measurements')
    output.mkdir(parents=True)
    index={r['file']:r for r in json.loads((directory/'index.json').read_text())}
    source=json.loads(geometry.read_text())
    result=dict(sourceSha256=source['sourceSha256'],geometrySha256=hashlib.sha256(geometry.read_bytes()).hexdigest(),
        method='Fixed clear bar ROI and inspected clear selector ROIs; all channel values retained. '
               'Contacts icon bounding box at three thresholds on unchanged native pixels.',
        limits='Visible output patches, not a recovered underlying page or source texture. '
               'Selector ROI changes only to avoid ink as the body moves. '
               'Complete-icon masks are used only on frames001,013,014; intermediate refracted fragments '
               'cannot define magnification. No original pointer timestamps or optical model fit.',samples=[])
    for row in source['samples']:
        name=row['file'];n=int(name[6:9]);path=directory/name
        digest=hashlib.sha256(path.read_bytes()).hexdigest()
        if digest!=index[name]['sha256']:raise ValueError('Decode hash mismatch: '+name)
        image=Image.open(path).convert('RGB');pixels=np.asarray(image)
        if image.size!=(1170,260):raise ValueError('Expected native1170x260 crop')
        h=row['summary']['horizontal']['edgeMedians'];cx=(h['left']+h['right'])/2
        patches={'bar':[300,100,340,120],
                 'selector':[650,100,670,120] if n<=8 else [int(cx+16),136,int(cx+32),147],
                 'outside':[600,236,640,250]}
        overlay=image.copy();draw=ImageDraw.Draw(overlay)
        measurements={}
        for role,rect in patches.items():
            x0,y0,x1,y1=rect;a=pixels[y0:y1,x0:x1]
            spread=int(a.max())-int(a.min())
            measurements[role]=dict(rect=rect,medianRgb=np.median(a,axis=(0,1)).tolist(),
                minRgb=np.min(a,axis=(0,1)).tolist(),maxRgb=np.max(a,axis=(0,1)).tolist(),
                spatialChannelRange=spread,usable=spread<=3,
                flags=[] if spread<=3 else ['Patch range exceeds authored3level uniformity gate'],
                pixels=a.tolist())
            draw.rectangle((x0,y0,x1-1,y1-1),outline={'bar':'#ffcf60','selector':'#4cffff','outside':'#ff6ab6'}[role],width=1)
        icons=[]
        if n in (1,13,14):
            roi=pixels[55:150,400:540].astype(int)
            for threshold in (160,180,200):
                mask=roi.min(axis=2)>threshold if n==1 else (
                    (roi[:,:,2]-roi[:,:,0]>40)&(roi[:,:,2]-roi[:,:,1]>15)&(roi[:,:,2]>threshold))
                yy,xx=np.nonzero(mask)
                if not len(xx):raise ValueError('Empty complete icon mask')
                box=[int(xx.min()+400),int(yy.min()+55),int(xx.max()+401),int(yy.max()+56)]
                icons.append(dict(threshold=threshold,colour='white' if n==1 else 'blue',bbox=box,
                                  width=box[2]-box[0],height=box[3]-box[1],pixels=int(len(xx))))
                draw.rectangle((box[0],box[1],box[2]-1,box[3]-1),outline='#ff80ef',width=1)
        result['samples'].append(dict(index[name],patches=measurements,iconProbes=icons))
        overlay.save(output/name)
    (output/'measurements.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    sheet=Image.new('RGB',(1170,7*155),'#15232d');draw=ImageDraw.Draw(sheet)
    for i,row in enumerate(result['samples']):
        x=i%2*585;y=i//2*155
        draw.text((x+5,y+3),f"{row['file']} {row['seconds']:.6f}s",fill='white')
        sheet.paste(Image.open(output/row['file']).resize((585,130)),(x,y+25))
        print(row['file'],{k:v['medianRgb'] for k,v in row['patches'].items()},row['iconProbes'])
    sheet.save(output/'overlays.png')


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('directory',type=Path);p.add_argument('geometry',type=Path)
    p.add_argument('--output',type=Path,required=True)
    a=p.parse_args();main(a.directory,a.geometry,a.output)
