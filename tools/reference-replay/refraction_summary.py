# refraction_summary.py <refraction.csv> [...]: the tables of review/atlas/refraction-gap-2026-10-24 from refraction.py rows.
# Uncertainty: 95% bootstrap interval of the median over FRAMES (columns of one frame are not independent).
import sys, csv
import numpy as np
from collections import defaultdict

rng = np.random.default_rng(77)
rows = []
for p in sys.argv[1:]:
    rows += list(csv.DictReader(open(p)))
RECS = ['docs1', 'docs2', 'IMG_6756', 'JGEY2190', 'LOLL8185']


def boot(per_frame, n=2000):
    v = np.array(per_frame)
    if len(v) < 3:
        return (np.nan, np.nan)
    m = [np.median(rng.choice(v, len(v))) for _ in range(n)]
    return np.percentile(m, 2.5), np.percentile(m, 97.5)


def fmt(v):
    if not v:
        return '-'
    lo, hi = boot(v)
    return f"{np.median(v):+.1f} [{lo:+.1f}, {hi:+.1f}] (n={len(v)})"


print('## RIM: bar hairline pulled toward the lens centre, central columns |x - cx| <= 60 px')
print('rec | state | iOS d (px) | ours d (px) | iOS h | ours h | gap iOS-ours')
rim = defaultdict(list)
for r in rows:
    if r['kind'] != 'rim':
        continue
    rim[(r['rec'], r['src'], r['state'])].append(r)
allg = defaultdict(lambda: defaultdict(list))
for rec in RECS:
    for st in ('held', 'moving'):
        out = {}
        for src in ('ios', 'ours'):
            fr = defaultdict(list); hh = []
            for r in rim.get((rec, src, st), []):
                if abs(float(r['x_or_u'])) <= 60:
                    fr[r['t']].append(float(r['d_or_dx'])); hh.append(float(r['h_or_scale']))
            per = [np.median(v) for v in fr.values()]
            out[src] = (per, hh)
            allg[st][src] += per
        if not out['ios'][0] and not out['ours'][0]:
            continue
        gi = np.median(out['ios'][0]) if out['ios'][0] else np.nan
        go = np.median(out['ours'][0]) if out['ours'][0] else np.nan
        print(f"{rec} | {st} | {fmt(out['ios'][0])} | {fmt(out['ours'][0])} | "
              f"{np.median(out['ios'][1]) if out['ios'][1] else float('nan'):.1f} | "
              f"{np.median(out['ours'][1]) if out['ours'][1] else float('nan'):.1f} | {gi - go:+.1f}")
for st in ('held', 'moving'):
    print(f"ALL | {st} | {fmt(allg[st]['ios'])} | {fmt(allg[st]['ours'])}")

print('\n## RIM profile: d against the true edge depth inside the rim h (all lens columns, held + moving)')
bins = [(3, 6), (6, 9), (9, 12), (12, 15), (15, 20), (20, 30)]
print('h bin | ' + ' | '.join(f'{rec} iOS / ours' for rec in RECS))
for lo, hi in bins:
    cells = []
    for rec in RECS:
        c = []
        for src in ('ios', 'ours'):
            v = [float(r['d_or_dx']) for k, rr in rim.items() if k[0] == rec and k[1] == src for r in rr
                 if lo <= float(r['h_or_scale']) < hi]
            c.append(f"{np.median(v):+.1f} ({len(v)})" if len(v) >= 10 else '-')
        cells.append(' / '.join(c))
    print(f"{lo}-{hi} | " + ' | '.join(cells))

print('\n## RIM across the lens: d by column offset x - cx (held, all recordings)')
for lo, hi in [(-200, -120), (-120, -60), (-60, 0), (0, 60), (60, 120), (120, 200)]:
    c = []
    for src in ('ios', 'ours'):
        v = [float(r['d_or_dx']) for k, rr in rim.items() if k[1] == src and k[2] == 'held' for r in rr
             if lo <= float(r['x_or_u']) < hi]
        hv = [float(r['h_or_scale']) for k, rr in rim.items() if k[1] == src and k[2] == 'held' for r in rr
              if lo <= float(r['x_or_u']) < hi]
        c.append(f"{np.median(v):+.1f} (h {np.median(hv):.0f}, n={len(v)})" if v else '-')
    print(f"{lo}..{hi} | " + ' | '.join(c))

print('\n## ICON: apparent scale of the icon under the selector (|u| <= 20) and of icons >= 200 px away')
icon = defaultdict(list)
for r in rows:
    if r['kind'] == 'icon' and float(r['ya_or_ncc']) >= .6:
        icon[(r['rec'], r['src'], r['state'])].append(r)
print('rec | state | iOS under | ours under | iOS away | ours away')
for rec in RECS:
    for st in ('rest', 'tap', 'held', 'moving'):
        c = []
        for src in ('ios', 'ours'):
            under = [float(r['h_or_scale']) for r in icon.get((rec, src, st), []) if abs(float(r['x_or_u'])) <= 20]
            away = [float(r['h_or_scale']) for r in icon.get((rec, src, st), []) if abs(float(r['x_or_u'])) >= 200]
            c.append((under, away))
        if not any(c[0]) and not any(c[1]):
            continue
        f2 = lambda v: f"{np.median(v):.2f} (n={len(v)})" if v else '-'
        print(f"{rec} | {st} | {f2(c[0][0])} | {f2(c[1][0])} | {f2(c[0][1])} | {f2(c[1][1])}")

print('\n## ICON shift along the bar as the selector passes: median dx * sign(u) by |u| (+ = pushed away from the selector centre)')
ub = [(0, 20), (20, 60), (60, 100), (100, 140), (140, 180), (180, 230), (230, 330)]
for st in ('tap', 'moving', 'held'):
    print(f'state {st}: |u| bin | ' + ' | '.join(f'{rec} iOS / ours' for rec in RECS))
    for lo, hi in ub:
        cells = []
        for rec in RECS:
            c = []
            for src in ('ios', 'ours'):
                v = [float(r['d_or_dx']) * np.sign(float(r['x_or_u'])) for r in icon.get((rec, src, st), [])
                     if lo <= abs(float(r['x_or_u'])) < hi]
                c.append(f"{np.median(v):+.1f} ({len(v)})" if len(v) >= 5 else '-')
            cells.append(' / '.join(c))
        print(f"  {lo}-{hi} | " + ' | '.join(cells))
    print(f'state {st}: apparent scale by |u|')
    for lo, hi in ub:
        cells = []
        for rec in RECS:
            c = []
            for src in ('ios', 'ours'):
                v = [float(r['h_or_scale']) for r in icon.get((rec, src, st), []) if lo <= abs(float(r['x_or_u'])) < hi]
                c.append(f"{np.median(v):.2f}" if len(v) >= 5 else '-')
            cells.append(' / '.join(c))
        print(f"  {lo}-{hi} | " + ' | '.join(cells))
