"""Exploratory critical-response fit to inspected optical cross-section centres.

This fits output only. It does not identify pointer timing or Apple's mechanics,
does not change production, and is not independent validation. SciPy required.
"""
import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
from scipy.optimize import least_squares


def main(args):
    if args.output.exists():
        raise ValueError('Use a new output path')
    source = json.loads(args.measurements.read_text(encoding='utf-8'))
    rows = [r for r in source['samples'] if int(r['file'][6:9]) >= args.first_frame
            and r['summary']['horizontal']['usable']]
    if len(rows) < 5:
        raise ValueError('At least five inspected centre samples required')
    t = np.array([r['seconds'] for r in rows])
    x = np.array([sum(r['summary']['horizontal']['edgeMedians'].values())/2 for r in rows])

    def model(omega, start):
        q = omega * np.maximum(t-start, 0)
        return args.target_x+(args.rest_x-args.target_x)*(1+q)*np.exp(-q)

    bounds = ([5, t[0]-.3], [60, t[0]+.05])
    fit = least_squares(lambda p: model(*p)-x, [18, t[0]-.02], bounds=bounds)
    fixed = least_squares(lambda p: model(24, p[0])-x, [t[0]],
                          bounds=([bounds[0][1]], [bounds[1][1]]))
    predictions = dict(fitted=model(*fit.x), fixed24=model(24, fixed.x[0]))
    candidate = None
    if args.candidate_omega is not None:
        candidate = least_squares(lambda p: model(args.candidate_omega, p[0])-x, [t[0]],
                                  bounds=([bounds[0][1]], [bounds[1][1]]))
        predictions['frozenCandidate'] = model(args.candidate_omega, candidate.x[0])
    result = dict(measurementsSha256=hashlib.sha256(args.measurements.read_bytes()).hexdigest(),
        method='Fit omega and one time origin for an analytical critically damped response, '
               'with fixed resting/target centres. Compare24/s with its own best time origin.',
        limits='Exploratory output fit only; all selected samples train the fit. No recovered '
               'pointer-down, Apple spring, production change, full-gesture or independent validation claim. '
               'Observed centres are optical cross-section midpoints, not tracked mass centres.',
        fixedRestX=args.rest_x, fixedTargetX=args.target_x,
        fittedOmega=float(fit.x[0]), fittedTimeOrigin=float(fit.x[1]),
        fixed24TimeOrigin=float(fixed.x[0]),
        frozenCandidateOmega=args.candidate_omega,
        frozenCandidateTimeOrigin=float(candidate.x[0]) if candidate is not None else None,
        metrics={name:dict(rms=float(np.sqrt(np.mean((pred-x)**2))),
                           maxAbsolute=float(max(abs(pred-x)))) for name,pred in predictions.items()},
        samples=[dict(file=r['file'], pts=r['pts'], seconds=r['seconds'], observed=float(x[i]),
                      predictions={name:float(v[i]) for name,v in predictions.items()}) for i,r in enumerate(rows)])
    args.output.write_text(json.dumps(result, indent=2)+'\n', encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='samples'}, indent=2))


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('measurements', type=Path)
    p.add_argument('--first-frame', type=int, required=True)
    p.add_argument('--rest-x', type=float, required=True)
    p.add_argument('--target-x', type=float, required=True)
    p.add_argument('--candidate-omega', type=float)
    p.add_argument('--output', type=Path, required=True)
    main(p.parse_args())
