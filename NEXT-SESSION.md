# Resume LiquidGlass / Atlas Studio

Updated 13 October 2026 (Claude takeover from Codex). The previous, longer handoff is preserved
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
- README and repository description rewritten (owner authorized the description/homepage).

## Next, in order

1. Held bar fuses with the search button in iOS (liquid neck); ours does not.
2. Fast held reversals: iOS stretches into a long flattened, strongly refracting capsule.
3. Hold onset ~150 ms later than reference (finger-down is inferred — check before tuning).
4. Selected ink magnification inside a moving lens is weaker than iOS.
5. Atlas Studio redesign around the material (content moving under glass, one scene switcher).
6. Then: Android verification when a device is available; native navigation selector for .NET.

## Constraints (owner)

Work alone: no subagents, no computer-use. Atlas only, not Vitals. JDK 21 at
`C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`, Gradle via `tools/workspace.ps1`.
Commits/pushes authorized; no packages, tags or releases; no admin bypass for PR6. Keep private
recordings, page content and credentials off GitHub. Do not run the GPU replay while the JVM test
suite is running (CPU contention caused a historical test timeout).
