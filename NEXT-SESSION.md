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

## Next, in order

1. 9.9 s landing: iOS's held lens hits the wall at speed and protrudes, then contracts through
   rest (42 px); ours is slowed by the held end-tab clamp before lift (~800 px/s) and narrows 10 px.
   Finger-momentum hand-off was tried (0.6/0.8/1.0) and rejected; needs a held-lens wall impact model.
2. iOS's cap swirl is still somewhat stronger. Neck parts 60–70 ms late (inferred lift; pass 8).
3. Owner visual acceptance of the selector and of the redesigned Atlas Studio.
4. CI: Atlas UI tests now take ~2.6 min locally (density 0.75, reduced motion, no per-move waits;
   they took 14 min on CI before). Hosted runner speed varies ~2x (36 min on 31bd210 with
   unchanged classes 1.9x slower), so the build job limit is 55 min.
5. Then: Android verification when a device is available. (Ports: the Calm selector is done for
   CanvasKit and .NET, 14 Oct; its material is approximate.)

## Constraints (owner)

Work alone: no subagents, no computer-use. Atlas only, not Vitals. JDK 21 at
`C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`, Gradle via `tools/workspace.ps1`.
Commits/pushes authorized; no packages, tags or releases; no admin bypass for PR6. Keep private
recordings, page content and credentials off GitHub. Do not run the GPU replay while the JVM test
suite is running (CPU contention caused a historical test timeout).
