"""Score exported production tap pigment against inspected Phone output patches.

The patch file declares one time origin per episode. This is observed-bar
tint/lift algebra, not a reconstruction of hidden input pixels or finger times.
Requires only Python's standard library; never rewrites earlier result files.
"""
import argparse
import bisect
import csv
import hashlib
import json
import math
from pathlib import Path


def read_trace(path):
    with path.open(encoding="utf-8") as stream:
        return [{k: float(v) for k, v in row.items()} for row in csv.DictReader(stream)]


def interpolate(rows, times, seconds, key):
    right = bisect.bisect_right(times, seconds)
    if right == 0:
        return rows[0][key]
    if right == len(rows):
        return rows[-1][key]
    left = right - 1
    amount = (seconds - times[left]) / (times[right] - times[left])
    return rows[left][key] * (1 - amount) + rows[right][key] * amount


def compare(patches_path, baseline_path, candidate_path):
    patches = json.loads(patches_path.read_text(encoding="utf-8"))
    baseline, candidate = read_trace(baseline_path), read_trace(candidate_path)
    unchanged = ("restWidth", "restHeight", "slots", "pressSeconds", "seconds", "phase",
                 "cx", "width", "height", "formation", "protrusion")
    if len(baseline) != len(candidate) or not baseline:
        raise ValueError("Exports must have the same non-empty inventory")
    for i, (old, new) in enumerate(zip(baseline, candidate)):
        for key in unchanged:
            if old[key] != new[key]:
                raise ValueError(f"Geometry/formation changed at row {i}: {key}")
    result = {
        "method": "Actual production exports; same observed-bar interior transfer and fixed "
                  "episode time origins. All three episodes informed calibration, not held-out proof.",
        "limits": "No hidden backdrop, edge transport, complete contour or original pointer time "
                  "reconstruction. Values above the surrounding bar cannot be explained by "
                  "negative pigment alone. Use native/shader evidence separately.",
        "inputSha256": {name: hashlib.sha256(path.read_bytes()).hexdigest() for name, path in
                        [("patches", patches_path), ("baseline", baseline_path), ("candidate", candidate_path)]},
        "unchangedGeometryRows": len(candidate), "episodes": {},
    }
    for episode in patches["episodes"]:
        scored = {"timeOrigin": episode["timeOrigin"], "models": {}}
        for name, trace in [("baseline", baseline), ("candidate", candidate)]:
            rows = [r for r in trace if r["restWidth"] == episode["restWidth"] and
                    r["slots"] == episode["slots"] and r["pressSeconds"] == episode["pressSeconds"]]
            if not rows:
                raise ValueError(f"Missing trace for {episode['name']}")
            times = [r["seconds"] for r in rows]
            if any(b <= a for a, b in zip(times, times[1:])):
                raise ValueError("Export times must increase strictly")
            samples = []
            for patch in episode["samples"]:
                t = patch["seconds"] - episode["timeOrigin"]
                formation = interpolate(rows, times, t, "formation")
                appearance = interpolate(rows, times, t, "transitAppearance")
                bar = patch["observedBar"]
                predicted = bar - (bar * .5 + 10) * (1 - formation) * (1 - appearance)
                samples.append(dict(patch, formation=formation, appearance=appearance,
                                    predicted=predicted, error=predicted-patch["observedSelector"]))
            scored["models"][name] = {
                "rms": math.sqrt(sum(r["error"]**2 for r in samples)/len(samples)),
                "maxAbsolute": max(abs(r["error"]) for r in samples), "samples": samples,
            }
        result["episodes"][episode["name"]] = scored
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("patches", type=Path)
    parser.add_argument("baseline", type=Path)
    parser.add_argument("candidate", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Use a new output file; preserve earlier results")
    result = compare(args.patches, args.baseline, args.candidate)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2)+"\n", encoding="utf-8")
    for name, episode in result["episodes"].items():
        print(name, {k: (round(v["rms"], 3), round(v["maxAbsolute"], 3))
                     for k, v in episode["models"].items()})
