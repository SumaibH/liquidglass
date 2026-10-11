"""Score two native partial trips against frozen old/new production traces.

One best time translation per model/episode, no time scaling or framewise matching.
Keeps every source sample, including unresolved axes. NumPy/Matplotlib required.
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

from compare_phone_tap_sequence import cuts, observed


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main(args):
    if args.output.exists():
        raise ValueError('Use a new output directory')
    args.output.mkdir(parents=True)
    traces = {}
    for name, path in [('previous', args.baseline), ('candidate', args.candidate)]:
        with path.open() as stream:
            rows = [{k:float(v) for k,v in r.items()} for r in csv.DictReader(stream)]
        traces[name] = [r for r in rows if r['restWidth']==278 and r['pressSeconds']==.04]
        if len(traces[name])!=481:
            raise ValueError('Expected481 nominal production samples')
    result = dict(method='Nominal278px/40ms production input. For each model/episode, fit one '
                  'time offset to all nine moving centre samples on a0.1ms grid. '
                  'No rate/geometry fit, time scaling or per-frame rematching. '
                  'Model polygon intersections use the same source rows.',
        limits='Partial optical edge tracks, not recovered pointer timing or rendered optical '
               'agreement. Repeated-episode heights unresolved. Model was calibrated using '
               'the first episode; second episode informed candidate validation. '
               'Whole-bar transform and source resting-geometry uncertainty remain.',
        baselineSha256=digest(args.baseline), candidateSha256=digest(args.candidate), episodes={})
    fig, axes = plt.subplots(2,2,figsize=(12,7),layout='constrained')
    for column,(name,path) in enumerate([('first',args.first),('repeat',args.repeat)]):
        source=json.loads(path.read_text(encoding='utf-8'))
        rows=[r for r in source['samples'] if int(r['file'][6:9])>=6]
        seconds=np.array([r['seconds'] for r in rows])
        x=(seconds-seconds[0])*1000
        actual=[observed(r) for r in rows]
        episode=dict(sourceSha256=digest(path), models={})
        for model_name,trace in traces.items():
            t=np.array([r['seconds'] for r in trace])
            model_cuts=[cuts(trace,r) for r in rows]
            shifts=np.arange(seconds[0]-.25,seconds[0]+.10001,.0001)
            residual=np.array([np.interp(r['seconds']-shifts,t,cut['centre'])-obs['centre']
                for r,cut,obs in zip(rows,model_cuts,actual) if r['summary']['horizontal']['usable']])
            shift=float(shifts[np.argmin(np.sum(residual*residual,axis=0))])
            predicted=[{k:float(np.interp(r['seconds']-shift,t,v)) for k,v in cut.items()}
                       for r,cut in zip(rows,model_cuts)]
            metrics={}
            for key,axis in [('centre','horizontal'),('width','horizontal'),('height','vertical')]:
                errors=[p[key]-a[key] for r,p,a in zip(rows,predicted,actual) if r['summary'][axis]['usable']]
                if errors:
                    metrics[key]=dict(samples=len(errors),rms=float(np.sqrt(np.mean(np.square(errors)))),
                                      maxAbsolute=float(max(abs(e) for e in errors)))
            episode['models'][model_name]=dict(timeOrigin=shift,metrics=metrics,
                samples=[dict(file=r['file'],pts=r['pts'],seconds=r['seconds'],observed=a,predicted=p,
                              horizontalUsable=r['summary']['horizontal']['usable'],
                              verticalUsable=r['summary']['vertical']['usable'])
                         for r,a,p in zip(rows,actual,predicted)])
            for i,key in enumerate(('centre','width')):
                axes[i,column].plot(x,[p[key] for p in predicted],'.-',
                    color='#0a7589' if model_name=='candidate' else '#969ba4',label=model_name.title())
        for i,key in enumerate(('centre','width')):
            ax=axes[i,column];ax.plot(x,[r[key] for r in actual],'o',color='#bd3372',label='Source edge probes')
            ax.set_ylabel(key+' (source px)');ax.grid(alpha=.2);ax.legend(fontsize=8)
        axes[0,column].set_title(name.title()+' episode: one time offset per model')
        axes[1,column].set_xlabel('Native milliseconds from first moving sample')
        result['episodes'][name]=episode
    fig.suptitle('Slower adjacent travel improves timing; repeated-episode width remains imperfect')
    fig.savefig(args.output/'timing-comparison.png',dpi=150);plt.close(fig)
    (args.output/'timing-comparison.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    for ep,row in result['episodes'].items():
        print(ep,{name:report['metrics'] for name,report in row['models'].items()})


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('first',type=Path);parser.add_argument('repeat',type=Path)
    parser.add_argument('baseline',type=Path);parser.add_argument('candidate',type=Path)
    parser.add_argument('--output',type=Path,required=True)
    main(parser.parse_args())
