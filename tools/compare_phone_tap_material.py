"""Compare measured tap interiors with the current style-transfer diagnostic.

This is the declared interior tint/lift algebra over the OBSERVED bar, not a full
shader render. It isolates material formation from unknown source-page pixels.
The zoom is the central map factor, not a rendered glyph bound. Keep those limits.
"""
import argparse
import csv
import hashlib
import json
from pathlib import Path

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import numpy as np


def main(args):
    if args.output.exists():raise ValueError('Use a new output directory')
    args.output.mkdir(parents=True)
    source=json.loads(args.measurements.read_text())
    alignment=json.loads(args.alignment.read_text())
    origin=alignment['episodes']['first']['models']['candidate']['timeOrigin']
    with args.trace.open() as f:trace=[{k:float(v) for k,v in row.items()} for row in csv.DictReader(f)]
    result=dict(sourceSha256=source['sourceSha256'],
        measurementsSha256=hashlib.sha256(args.measurements.read_bytes()).hexdigest(),
        traceSha256=hashlib.sha256(args.trace.read_bytes()).hexdigest(),
        alignmentSha256=hashlib.sha256(args.alignment.read_bytes()).hexdigest(),
        timeOrigin=origin,
        model='Current Calm/V3 interior tint alpha=.5*(1-f), lift=-10*(1-f) RGB levels. '
              'Use OBSERVED bar B: S=B*(1-.5*(1-f))-10*(1-f). Central ink factor '
              '1/(1-.12*f*f): style heldMagnification and heldLens both interpolate with f.',
        limits='Diagnostic interior algebra only, not a full rendered optical comparison. '
               'Raw share, blur, edge transport and highlights are not reproduced. '
               'Selected glyph size may include native emphasis as well as lensing. '
               'Unknown hidden backdrop prevents fitting bar lift from its observed value. '
               'Use the previous nominal trajectory time origin for all authored alternatives; '
               'input durations and resting dimensions are not recovered original input.',samples=[])
    for row in source['samples']:
        bar=float(np.mean(row['patches']['bar']['medianRgb']))
        selected=float(np.mean(row['patches']['selector']['medianRgb']))
        alternatives=[]
        for width in (271,278,285):
            for duration in (.04,.08,.116,.16,.24):
                a=[r for r in trace if r['restWidth']==width and r['pressSeconds']==duration]
                form=float(np.interp(row['seconds']-origin,[r['seconds'] for r in a],[r['formation'] for r in a]))
                transfer=bar*(1-.5*(1-form))-10*(1-form)
                alternatives.append(dict(restWidth=width,pressSeconds=duration,formation=form,
                    selectorInterior=transfer,selectorToBar=transfer/bar,
                    centralInkFactor=1/(1-.12*form*form)))
        icon_scale=[dict(threshold=p['threshold'],width=p['width']/69,height=p['height']/69)
                    for p in row['iconProbes']]
        result['samples'].append(dict(file=row['file'],pts=row['pts'],seconds=row['seconds'],
            observedBar=bar,observedSelector=selected,observedRatio=selected/bar,
            iconScale=icon_scale,alternatives=alternatives))
    (args.output/'comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    x=np.array([r['seconds'] for r in result['samples']])
    fig,axes=plt.subplots(1,2,figsize=(12,4.5),layout='constrained')
    for axis,key in [(axes[0],'selectorToBar'),(axes[1],'centralInkFactor')]:
        a=np.array([[v[key] for v in r['alternatives']] for r in result['samples']])
        axis.fill_between(x,a.min(axis=1),a.max(axis=1),alpha=.3,color='#1f8093',label='15 authored model alternatives')
        nominal=[next(v[key] for v in r['alternatives'] if v['restWidth']==278 and v['pressSeconds']==.04)
                 for r in result['samples']]
        axis.plot(x,nominal,'-',color='#106c81',label='278px/40ms nominal diagnostic')
        axis.set_xlabel('Native source seconds');axis.grid(alpha=.2)
    axes[0].plot(x,[r['observedRatio'] for r in result['samples']],'o-',color='#c13d79',label='Visible source patches')
    axes[0].set_ylabel('Selector / observed bar RGB ratio')
    for row in result['samples']:
        if row['iconScale']:
            values=[p['width'] for p in row['iconScale']]
            axes[1].plot([row['seconds']]*len(values),values,'o',color='#c13d79',markersize=5)
    axes[1].plot([],[],'o',color='#c13d79',label='Complete source icon bounds, 3 thresholds')
    axes[1].set_ylabel('Visible icon scale / central map factor')
    for ax in axes:ax.legend(fontsize=8)
    fig.suptitle('Tap optics remain too close to the dark inset after timing improves')
    fig.supxlabel('Interior transfer and central-map diagnostics; not matched shader pixels or recovered Apple mechanics.',fontsize=9)
    fig.savefig(args.output/'comparison.png',dpi=150);plt.close(fig)
    for r in result['samples'][-2:]:
        print(r['file'],'source',r['observedSelector'],'bar',r['observedBar'],
              'predicted',[min(v['selectorInterior'] for v in r['alternatives']),max(v['selectorInterior'] for v in r['alternatives'])],
              'icon',r['iconScale'])


if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('measurements',type=Path);p.add_argument('trace',type=Path);p.add_argument('alignment',type=Path)
    p.add_argument('--output',type=Path,required=True)
    main(p.parse_args())
