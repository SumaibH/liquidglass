"""Compare a consecutive native short tap with a frozen production export.

Use one time shift at the pre-existing frame013 calibration centre. No time
scaling or framewise rematching in the trajectory score. A separate phase-only
diagnostic distinguishes geometry errors from trajectory errors. NumPy and
Matplotlib required. Original images and rejected probes remain separate.
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


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def cuts(trace, row):
    ys = row['annotation']['ys']
    left = np.median([[r[f'left{y}'] for y in ys] for r in trace], axis=1)
    right = np.median([[r[f'right{y}'] for y in ys] for r in trace], axis=1)
    return dict(centre=(left+right)/2, width=right-left,
                height=np.array([r['height'] for r in trace]))


def observed(row):
    h, v = row['summary']['horizontal'], row['summary']['vertical']
    return dict(centre=sum(h['edgeMedians'].values())/2,
                width=h['median'], height=v['median'])


def first_crossing(times, centres, target):
    indexes = np.where((centres[:-1] >= target) & (centres[1:] <= target))[0]
    if not len(indexes):
        raise ValueError('Observed centre outside the leftward model travel')
    i = indexes[0]
    return float(times[i]+(target-centres[i])/(centres[i+1]-centres[i])*(times[i+1]-times[i]))


def metrics(rows, mode):
    output = {}
    for key in ('centre', 'width', 'height'):
        selected = [r for r in rows if r['file'] != 'frame-013.png' and
                    int(r['file'][6:9]) >= 6 and r['usable'][key]]
        e = np.array([r[mode][key]-r['observed'][key] for r in selected])
        output[key] = dict(samples=len(e), rms=float(np.sqrt(np.mean(e*e))),
                           maxAbsolute=float(max(abs(e))))
    return output


def main(measurements, trace_path, output):
    if output.exists():
        raise ValueError('Use a new output directory to preserve earlier attempts')
    output.mkdir(parents=True)
    source = json.loads(measurements.read_text(encoding='utf-8'))
    rows = source['samples']
    anchor = next(r for r in rows if r['file'] == 'frame-013.png')
    with trace_path.open() as stream:
        trace = [{k: float(v) for k, v in r.items()} for r in csv.DictReader(stream)]
    alternatives = []
    for width in (285, 278, 271):
        for duration in (.04, .08, .116, .16, .24):
            values = [r for r in trace if r['restWidth'] == width and
                      abs(r['pressSeconds']-duration) < 1e-6]
            if len(values) != 481:
                raise ValueError('Expected all481 unchanged production samples per alternative')
            t = np.array([r['seconds'] for r in values])
            anchor_time = first_crossing(t, cuts(values, anchor)['centre'], observed(anchor)['centre'])
            shift = anchor['seconds'] - anchor_time
            aligned = []
            for row in rows:
                actual = observed(row)
                model = cuts(values, row)
                relative = row['seconds']-shift
                # Before authored pointer-down the model stays at its exact initial rest.
                time_values = {k: float(np.interp(relative, t, v)) for k, v in model.items()}
                phase_values = None
                try:
                    phase_time = first_crossing(t, model['centre'], actual['centre'])
                    phase_values = {k: float(np.interp(phase_time, t, v)) for k, v in model.items()}
                except ValueError:
                    # Initial observed rest is slightly outside this authored nominal origin.
                    phase_values = {k: float(v[0]) for k, v in model.items()}
                h = row['summary']['horizontal']['usable']
                v = row['summary']['vertical']['usable']
                aligned.append(dict(file=row['file'], pts=row['pts'], seconds=row['seconds'],
                    modelSeconds=relative, usable=dict(centre=h, width=h, height=v),
                    observed=actual, aligned=time_values, phaseOnly=phase_values))
            alternatives.append(dict(restWidth=width, pressSeconds=duration,
                anchorModelSeconds=anchor_time, authoredPointerDownOnSourceClock=shift,
                trajectoryMetrics=metrics(aligned, 'aligned'),
                phaseOnlyMetrics=metrics(aligned, 'phaseOnly'), rows=aligned))
    nominal = next(r for r in alternatives if r['restWidth'] == 278 and r['pressSeconds'] == .04)
    result = dict(measurementsSha256=sha(measurements), traceSha256=sha(trace_path),
        nativeSourceSha256=source['sourceSha256'], sourceSamples=len(rows), modelSamples=len(trace),
        method='One translation of time at frame013 centre for each authored alternative. No time '
               'scaling. Horizontal polygon intersections use the original scanline rows, y origin125.5. '
               'Middle target x479 is inherited provisional geometry. Vertical central-spine cuts use '
               'five offsets -16..16px about model centre. No production parameters fitted.',
        score='Frames006..014, excluding previously calibrated anchor013 and unresolved axes. '
              'Phase-only metrics independently match each centre and therefore are NOT temporal evidence.',
        limits='Observed optical gradients versus production geometric contour, not rendered colour/refraction. '
               'A14-frame partial trip is not a complete gesture or iOS parity proof. '
               'Three resting geometries and five contact durations are authored alternatives; '
               'one alignment cannot recover original pointer-down/up timestamps. '
               'Ambiguous source heights remain unresolved and are omitted from scores.',
        nominal=dict(restWidth=278, pressSeconds=.04), alternatives=alternatives)
    (output/'comparison.json').write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    fig, axes = plt.subplots(3, 2, figsize=(12, 9), sharex='col', layout='constrained')
    x = np.array([r['seconds']-anchor['seconds'] for r in nominal['rows']])*1000
    for i, key in enumerate(('centre', 'width', 'height')):
        for j, mode in enumerate(('aligned', 'phaseOnly')):
            ax = axes[i, j]
            all_values = np.array([[r[mode][key] for r in a['rows']] for a in alternatives])
            ax.fill_between(x, all_values.min(axis=0), all_values.max(axis=0), color='#7ea4b6',
                            alpha=.28, label='15 authored input/body alternatives')
            ax.plot(x, [r[mode][key] for r in nominal['rows']], '.-', color='#14778e',
                    label='278px /40ms nominal model')
            valid = np.array([r['usable'][key] for r in nominal['rows']])
            y = np.array([r['observed'][key] for r in nominal['rows']])
            ax.plot(x[valid], y[valid], 'o', color='#c14679', label='Inspected original edge probes')
            ax.axvline(0, color='#666666', linestyle=':', linewidth=1)
            ax.set_ylabel(key+' (source px)'); ax.grid(alpha=.2)
        axes[i, 0].text(.02, .04, f"Nominal non-anchor frame RMS: {nominal['trajectoryMetrics'][key]['rms']:.2f}px",
                        transform=axes[i, 0].transAxes, fontsize=9)
    axes[0, 0].set_title('Continuous partial trip: one time alignment')
    axes[0, 1].set_title('Shape-only diagnostic: centre matched each frame')
    axes[0, 0].legend(fontsize=7)
    for ax in axes[-1]: ax.set_xlabel('Native milliseconds relative to frame013 calibration anchor')
    fig.suptitle('Short tap: matching one shape does not establish the trajectory')
    fig.savefig(output/'comparison.png', dpi=150); plt.close(fig)
    print(json.dumps(dict(nominalTrajectory=nominal['trajectoryMetrics'],
                          nominalShapeOnly=nominal['phaseOnlyMetrics'],
                          alternatives=[{k:v for k,v in a.items() if k!='rows'} for a in alternatives]), indent=2))


if __name__ == '__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('measurements', type=Path)
    parser.add_argument('trace', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args=parser.parse_args()
    main(args.measurements, args.trace, args.output)
