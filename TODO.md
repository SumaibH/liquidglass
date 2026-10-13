# LiquidGlass / Atlas delivery checklist

Updated 2026-10-14. Start with [NEXT-SESSION.md](NEXT-SESSION.md). Checked entries describe
bounded completed work, **not completion of full iOS parity**. All open rows remain in scope.

## Now — continuous comparison against the recordings (13 October)

- [x] Deterministic 60 fps GPU replay of both recordings' gestures (`:desktop:phoneReplay`,
  `tools/reference-replay/`); three-row reference/before/after comparisons kept local.
- [x] Pass 1: tap transit optics, thrown-lens formation, wall landing shape, grasp lag, rim
  specular, dark material levels. `review/atlas/reference-replay-2026-10-13/`.
- [x] Held bar/search-button fusion (liquid neck): `GlassTabBarAccessory`; silhouette within 1 px at
  1.80 s. Neck timing follows our later press; finger-side bar growth not modelled.
- [x] Fast held reversals: lens follows the sweeps (grasp, hold formation, rebuilt input; pass 3).
  End-of-lens icon warping is still weaker than iOS.
- [x] Lens rim, band glow and interior clarity measured and matched (pass 4).
- [x] Moving-lens end optics: reflective rim at the caps and selected ink under the lens (pass 5).
  iOS's swirl is still larger and faintly rainbow-fringed.
- [x] Wall landings press into the end and narrow from behind (pass 6); comparison scene's resting
  pill corrected to the measured 95 pt. Landings with an inferred stop-before-lift do not narrow.
- [x] Inferred input corrected: hold onset aligned by construction (finger-down is invisible);
  wall throws lift moving. 2.3/12.4/25.6 s now narrow like iOS; 9.9 s (lens stopped at the wall
  before lift) does not.
- [x] Larger cap swirl with faint fringes (rim mirror 0.8) (pass 7).
- [ ] Finger-side, mostly horizontal bar growth: a shift toward the finger broke the measured
  fusion neck; needs anisotropic press growth (pass 7).
- [x] Atlas Studio redesign: moving full-window scene, one switcher, fused magnifier button.
- [ ] Owner visual acceptance of the navigation selector.

## P0 — current visible failures

- [x] Preserve the owner's rejected dark-disc screenshots and withdraw their visual sign-off.
- [x] Keep resting tint on the actual navigation substrate; preserve the resting inset and
  reveal the background through protruding caps. Published checkpoint5a81393.
- [x] Isolate the vertical moving-selector seam: no-wide and no-through remove it;
  no-mask and no-packed do not. Restore all temporary isolation variants.
- [x] Validate clipped-filter test coordinates. Canvas translation fixes the flawed image-placement
  test; a deferred-layer regression now reproduces the old defect and passes the candidate.
- [x] Verify the explicit JVM wide-input graph, retaining kernel/through-bar/substrate.
 13focused checks,43native phases/static and the complete369-case library inventory pass
 (342pass,27optional skips), with3Atlas passes and Android assembly. Original interruption
 and exact recovered/fresh coverage are preserved. Hosted confirmation remains pending.
- [x] Inspect native early taps in both directions, gradients and clipped/nested/container
 paths, plus resting/recovered insets and caps. The hard seam is removed; full optics remain open.
- [ ] Fix the separate long-tap appearance decay; current candidate becomes dark too early.
  Area+axial candidate corrects the25calibrated interior patches;371-case library inventory,
  3Atlas/Android and43native phases are verified. Complete optical/ink timing remains inM1.
  The original V3gesturetimeout and unchanged-source retry remain a reliability issue.

## M1 — complete motion, contour and optics across supplied gestures

- [x] Preserve source hashes/native PTS, gesture catalogue, decoded frames and failed baselines.
- [x] Coupled area/spine tap model, short-trip timing calibration and bilateral leading-edge
  landing recovery implemented with labelled authored parameters and regression evidence.
- [ ] Video1 short taps: close remaining width/contour error in the repeated episode; compare
  continuous appearance and neighboring content, not only centre timing or a best-fit frame.
- [ ] Video1 long taps: verify full shape and pigment/ink sequence, including the early repeated
  frame and late recovery; retain uncertainty about original input duration.
- [ ] Video2 stationary hold: verify growth, rim, refracted ink and bar response together.
- [ ] Video2 early throws before formation: both directions, material continuity and recovery.
- [ ] Video2 fully formed throws: both directions, intermediate contours and optical transitions.
- [ ] Video2 held reversals between extremes: continuous response and surrounding bar/ink behavior.
- [ ] Video2 soft intermediate-tab landing: continuous contour/material recovery and selection.
- [ ] Video2 hard endpoint collisions: both walls, close remaining cap-curvature/recovery error.
- [ ] Verify interrupted motion, re-grabs, cancellation, resize, reduced motion and frame-rate
  consistency after final model changes. Reuse already valid checks where inputs are unchanged.

## M2 / M3 — restrained interaction and measured extremes

- [x] Separate press expansion, travel deformation and whole-bar perpendicular strain.
- [x] Size-dependent subtle response for large cards; preserve small-control behavior.
- [x] Layout/hit targets anchored; navigation foreground drawing follows measured shared strain.
- [x] Native replay includes all eight app-window edges/corners, reversals and intermediate landing.
- [ ] Verify each direction against actual source extreme envelopes, with gradual onset; existing
  snapshots and selected width/height bounds alone do not prove full iOS equivalence.
- [ ] Preserve accessibility, gesture ownership, reduced transparency/motion and generic defaults.

## O1 — optical integrity

- [x] Separate continuous magnifier profile from selector-specific held optics.
- [x] Standalone magnifier continuity and endpoint composition regressions implemented.
- [x] Fix the JVM selector seam and audit native frames at original resolution; panel/container
  deferred-layer regressions and all 43 native phases pass. Android equivalence remains below.
- [ ] Close remaining rim/chromatic/side-boundary and material timing mismatches using source
  evidence; avoid opaque overhanging discs or a painted outline used to hide a sampling defect.
- [ ] Verify Android's endpoint composition equivalent when feasible; desktop's improved packed
  path is not evidence for Android's single-input fallback. Physical-device testing remains pending.

## A1 — Atlas Studio and performance

- [x] Native studio redesign, Sky/Type/Lines scenes, continuous draggable lens and responsive controls.
- [x] App-owned AWT replay, stable-layout checks, source screenshots and actual GPU metadata.
- [x] Prefer NVIDIA discrete GPU; paired static redraw submission improved on the owner's machine.
- [ ] Fix first-use/live interaction hitches and measure presented-frame/input behavior separately
  from forced redraw submission and screenshot readback. No false FPS claims from phase snapshots.
  The wide-input fix adds about 3 ms median submission time in the controlled NVIDIA comparison;
  cropping its transformed input preserved pixels but did not improve speed and was rejected.
- [ ] Review final studio at wide/narrow sizes, scene switches, zoom and visibility/accessibility
  controls. Obtain an acceptable visual result; current owner acceptance remains unresolved.

## P1 — practical integration across stacks

- [x] Compose scene/control APIs and integration documentation.
- [x] Framework-independent web continuous-lens preview and hosted browser checks.
- [x] Production CanvasKit painter/scene API, types, source ownership, cleanup, context recovery,
  source replacement and reusable calm input controller.
- [x] Native SkiaSharp painter and runnable WinForms host; Windows/Linux CI, native screenshots,
  fixture parity, source ownership and Linux visible-label guard.
- [ ] Implement and verify remaining native integration paths; document exact prerequisites and
  gaps. Compose + web + .NET is not equivalent to arbitrary-stack support.
- [ ] Complete native .NET navigation motion/controller integration and practical host-owned
  backdrop/widget capture. Whole-surface CalmInteraction, painter feedback and WinForms card
  input now pass144portable states,107520outline probes,12check groups and9native phases.
  WinForms standard-control capture now has executable native glyph/refraction/resize checks.
  Navigation-selector travel, custom/other-framework capture are still missing.
- [ ] Keep production shader contracts/packaging/licenses synchronized and validate each supported
  path with executable examples; documentation alone does not prove support.

## D1 / C1 — delivery, evidence, automation and protection

- [x] README/API/research/usage guides and per-checkpoint visual/verification packets exist.
- [x] Apache-2.0/NOTICE, free Dependabot/CodeQL/dependency checks and ten strict required checks.
- [x] Preserve Fable/Antigravity work and retire duplicate checkouts/caches; do not repeat cleanup.
- [x] Diagnose 44f01de hosted failure as combined Atlas timeout; split locally with all original
  coverage/resolution and unchanged per-case deadline, three cases passing.
- [ ] After the actual fixes, run required affected/full validation once, inspect native images,
  update source hashes, limitations, public evidence and verification ledger.
- [x] Audit this 12 October checkpoint: 132 intended files, curated native/cropped proof only;
  raw recordings and private configuration stay local. Repeat the audit for later changes.
- [ ] Rewrite PR title/body around final behavior; push coherent verified changes and inspect
  hosted results, especially the split Atlas suite. No cancelled/unfinished run counts as a pass.
- [ ] Complete full requirement-by-requirement goal audit, review and authorized merge of PR6
  under existing protection. No blanket administrator bypass, package publication, tag or release.

## Next action only

Current long-tap source is locally verified:344library passes/27optional skips,3Atlas passes,
Androidassembly and43inspected native phases. Originaltimeout/retry provenance is preserved.
All local queues are terminal; alltenaf333fa hosted checks pass. Sample-only A1 diagnostics
retain one unexplained bottom-right selection failure and two observed33-phase passing repeats.
The87ms first-input JFR is inconclusive; Windows denied the PresentMon ETW session, so display
timing remains open. See NEXT-SESSION.md and the frame-cost packet. Native.NET whole-surface
controller/host integration are hosted verified. New WinForms control capture is locally verified;
publish/check its ownCI next. Return to originalVideo1 repeated-short contour/width mismatch,
thenVideo2optical/contour parity. Native navigation and other host paths remain open.
Do not repeat unchanged library checks or these completed diagnostic probes.
