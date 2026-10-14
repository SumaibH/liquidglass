# Resume LiquidGlass / Atlas Studio

Updated 14 October 2026 (Claude takeover from Codex). The previous, longer handoff is preserved
at commit `efcc3b5` and in `.local/handoffs/`. Full iOS parity and owner visual acceptance are
**not** established. Read [TODO.md](TODO.md) and the full objective before changing source.

## How work is judged now

Core visible behaviour first. Every motion/material change is checked by replaying both owner
recordings through the real renderer and comparing **continuous sequences**, not best-fit frames:

1. `tools/reference-replay/` — ink tracker, inferred gesture scripts (`v1-script.txt`,
   `v2-script.txt`, catalogue), strip and three-row comparison tools.
2. `./gradlew :desktop:phoneReplay -Patlas.replay.script=<abs> -Patlas.replay.out=<abs>` renders
   `PhoneScene` at a deterministic 60 fps on the GPU (~2 min per recording). Not a performance
   measurement. Run it through `tools/workspace.ps1` on the owner's machine.
3. Three-row videos (reference / before / after) stay in `.local/comparisons/` — they contain the
   private recordings. Public evidence uses our renders only.

## Current state

- Pass 1 (13 Oct) fixed tap optics, thrown-lens formation, wall landing shape, grasp lag, rim
  specular and dark material levels. Record: `review/atlas/reference-replay-2026-10-13/README.md`.
- Baseline renders of the handoff build: `.local/sxs/base-v1`, `base-v2`; current: `fix4-v1`,
  `fix4-v2`. Comparisons: `.local/comparisons/2026-10-13-pass1/`.
- Pass 2 (13 Oct): `GlassTabBarAccessory` bar/search fusion.
- Pass 5 (13 Oct): moving-lens rim mirror at the caps (`rimMirror`, `foldAtCorners`), selected ink
  under the whole lens (`lensInkAll`), held lens blends to the mirror with travel (`heldTravel`).
- Pass 4 (13 Oct): bar rim, lens edge line, band glow (`heldGlowReach`), lens interior clarity.
- Pass 6 (14 Oct): two-edge wall landing (`releaseTrailingEdge`, 13/s; old Calm kept as
  `CalmFreeLanding`) and `PhoneGeometry` 95 pt pill / 8 pt ends. Current renders:
  `.local/sxs/land-v2`; landing clip `.local/sxs/landing-slow.mp4`.
- Pass 7 (14 Oct): input fixes (`v2script.py`: HOLD 137 ms, wall lifts moving), rim mirror 1.0
  with fringes, Atlas Studio redesign (`LoupeScreen`, replay variant
  `atlas`). Renders: `.local/sxs/p7-v2`, `.local/atlas/demo2`.
- Pass 3 (13 Oct): fast held reversals (grasp 120/s, held formation 50/s); reversal input
  rebuilt from turning points (`tools/reference-replay/reversal-script.txt`).
- README and repository description rewritten (owner authorized the description/homepage).
- PR6 merged into `main` (02f74a4, 14 Oct, owner: "permission granted for all ... merge already").
- Pass 9 (14 Oct, branch `claude/ios-gaps`): every wall landing scored (`.local/sxs/wallscore.py`:
  trailing edge underdamped toward the wall, carry .1, wall give .35; rms 4.9 px over nine
  landings, was 15.4; old Calm kept as `CalmCriticalLanding`); rim-mirror clamp bug fixed and
  `rimMirrorGain` 0.4 (cap petals); neck yields to an arriving lens (`yieldsToLens`) and the
  stationary-hold lift follows the iOS bar's release (`neck.py`: median 33 ms late, was 67); finger
  leads the ink 33 ms (`v2script.py` LEAD); ports use the exact Compose Calm material
  (`selector-material.json`). Renders: `.local/sxs/p13-v2`.

## Next, in order

1. Owner visual acceptance of the selector (pass 9) and of the redesigned Atlas Studio.
2. Pass 11 (14 Oct): dragPressShare .7 (bar relaxes while its lens is dragged), neck median 0 ms,
   rim dispersion .05, ported selected ink, mid-throw centroid stalls interpolated. Owner review
   videos: `.local/comparisons/2026-10-14-pass11/`. Residuals (pass 10, still current): 9.9 s landing passes rest by 36.5 px vs 41; left width series' first
   frames narrow up to 11.2 px less; iOS's cap petals carry stronger rainbow fringes; neck median
   17 ms late, 34.8 s 133 ms (bar and lens disagree on that lift). 28.66 s landing is an input
   artefact (source frame gap). Left landings: `.local/sxs/wallscorel.py` (rows 495–498).
3. Ports draw no selected ink (content pass); hosts own labels and ink.
4. CI: Atlas UI tests now take ~2.6 min locally (density 0.75, reduced motion, no per-move waits;
   they took 14 min on CI before). Hosted runner speed varies ~2x (36 min on 31bd210 with
   unchanged classes 1.9x slower), so the build job limit is 55 min.
5. Android: verified on an API 37 emulator (software GPU, AVD `lg_verify_37`,
   `review/atlas/android-emulator-2026-10-14`); a physical-device check remains open.

## Constraints (owner)

Work alone: no subagents, no computer-use. Atlas only, not Vitals. JDK 21 at
`C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`, Gradle via `tools/workspace.ps1`.
Commits/pushes authorized; no packages, tags or releases (PR6 was merged with the owner's
explicit go-ahead; ask again for any later admin merge). Keep private
recordings, page content and credentials off GitHub. Do not run the GPU replay while the JVM test
suite is running (CPU contention caused a historical test timeout).
