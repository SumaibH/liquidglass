# Resume LiquidGlass / Atlas Studio

Updated13October2026 after the saved handoff and resumed investigation. Full iOS parity remains
unfinished. Read TODO.md and the full objective before work; preserve every local change.

**Latest native backdrop work:** separate LiquidGlass.WinForms adapter captures an owned
Panel/Label/TextBox/CheckBox subtree through DrawToBitmap, with opaque snapshot ownership,
thread/handle guards and RichTextBox/ActiveX rejection. Actual native glyph changes, exact
identity-lens pixels, refraction, resize and retained-source failure checks pass. Five native
states inspected; locked restore/build0warnings0errors. See the dotnet-controls-2026-10-13
packet. Allten af333fa checks pass; newadapter needs ownhostedconfirmation afterpublication.
No local process running. Next return to source-aligned Video1 repeated-short contour/width
mismatch, then remainingVideo2optics/contours. Native navigation/otherhosts remainopen.

**Latest native.NET work:** CalmInteraction, optional painter feedback and WinForms card input
are locally verified:144portable states,107520outline points,12native check groups and9native
host phases. Release/recovery/reduced-motion pixel identities and1092anchored foreground pixels
pass; allphases inspected. See `review/atlas/dotnet-calm-2026-10-13/README.md`. Kotlin/SkSL
production source unchanged. Check git HEAD/status and current hostedCI for publication state.
Allten04d9016 checks pass. The newimplementation needs its own hostedconfirmation.
Next continue native navigation/controller/backdrop integration and full motion/optical scope;
whole-surfacecontroller does not completeP1. The checklist retains27completed/22open entries
of unequal size; no honest weightedcompletionpercentage or ETA has been established.

**Current A1 diagnostic checkpoint:** sample-only frame/input observation is verified locally;
production library remains the cea1913 implementation. All local queues are terminal. Exact
cea1913 hosted CI passes allten required checks; downloaded XML confirms344library passes,
27optional skips and3Atlas passes. Diagnostic source publication is identified by git HEAD/status.

- Forced redraw profiles found a491ms Direct3D lock wait during a555ms draw. This is not
  measured ordinary input/display latency; forcing redraw itself takes that lock.
- First natural replay failed bottom-right selection. Two subsequent observed33-phase replays
  passed with zero outside mouse/focus events. The original failure remains unexplained.
- Latest replay without JFR:8,903recording callbacks median.9028ms,p951.6017ms,max15.016ms;
 536dispatches,max32.1551ms. The preceding87ms first-input JFR interval is inconclusive.
- Bitmap-construction probe measures about.11ms for three mutable field snapshots, a secondary
  cost. No renderer/cache/quality/backend change was adopted.
- PresentMon2.6.0 binary digest/signature verified, but Windows refused the ETWsession (exit6).
  No presentationCSV or display metrics exist; no privileges/group settings changed.

Read [the frame-cost packet](review/atlas/frame-cost-2026-10-13/README.md) and researchStep203–207.
Next independent work: native.NET interaction/controller integration. Keep presentation timing,
selection reliability and every M1/O1/P1 acceptance gap open. Do not repeat these completed probes
or the unchanged library suite. Raw JFR/process metadata stays local.

The earlier detailed chat handoff is `.local/A1-RESUME-2026-10-13.md`; its snapshot status predates
this resumed work. Verified58-file save: `.local/handoffs/2026-10-13-a1-save/`. Preserve it and the
older snapshots. `.local/active-goal-checkpoint.txt` records the latest publication/live-job state.

## Current checkpoint

Long-tap appearance now uses projected area plus axial travel strain and gradual pigment recovery.
All 371 library cases are accounted for: **344 passes,27 optional skips,zero remaining failures**.
All **3Atlas tests pass**; Android assembly succeeds. All **43NVIDIA native phases** were inspected.
Rest/recovery in the adjacent replay are pixel-identical to the preceding wide-input renderer.
All LOCAL test/capture queues are terminal. Do not repeat completed validation for unchanged code.

This is a development checkpoint, not complete optical parity or owner acceptance. First-use redraw
771ms and the earlier controlled ~3ms wide-input submission cost remain unresolved. Original source
finger timing and hidden backdrop remain unknown. Allthree colour sequences informed calibration;
the repeated episode must NOT be presented as held-out proof.

## Read and respect

- Full objective: `C:/Users/shaya/.codex/attachments/8bcea84d-a196-4b90-8a8b-4627531cee0b/goal-objective.md`.
  It incorporates the original under `79754856-c646-459c-b409-5e80eee1368e`. Preserve full scope.
- Canonical checkout: `D:/Work/AndroidStudioProjects/SideProjects/kmp/liquidglass`;
  branch `codex/atlas-stabilization`, draft PR6. The chat working directory is different.
- Read AGENTS.md, WORKSPACE.md, PROGRESS.md, TODO.md and latest research entries.
- Work alone. NO subagents or computer-use. Terminal and app-owned input/render buffers only.
  Atlas only; no Vitals. No physical device available. Preserve all dirty/untracked work.
- Use tools/workspace.ps1 with JDK21 at `C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`.
  Private Gradle/Maven caches are under `.local/`; do not run a second suite over live work.
- Commits/pushes/eventual merge authorized; no packages/tags/releases or blanket PR6 review bypass.
  Private recordings/page content/names/transcripts stay off GitHub.
- Record findings/failures immediately; distinguish measured evidence from authored parameters.
  Tests, snapshots and calibration fits do not alone establish full 1:1 behavior.

## Verification provenance

- `.local/tap-strain-final-verification.json`: exact371-case inventory, sources and original failure.
- `.local/tap-strain-remaining-v2-results`: complete333-case run XML.305passed,27skipped,one
  historical V3 declined-drag test hit its1minute deadline after65.318s. Originalrun32m12s.
- `.local/tap-strain-timeout-recheck.xml`: the failed case passed27.451s with unchanged code,
  assertions and deadline. This is a recorded reliability issue, not a code fix or spotless run.
-38same-runtime focused cases are reused, plus305remaining passes+1retry+27skips. No duplicates,
  missing cases or interrupted test counted as passed. The initialconfiguration-only failure is saved.
- `.local/tap-strain-atlas-android-result.json`: successful6m12s completion;3Atlas total314.815s.
- `.local/tap-strain-final-results`, `.local/tap-strain-gesture-results`, focused/baseline XML/logs.
- `.local/atlas-tap-strain-full`:33phases,3m9s,inspected. `.local/atlas-tap-strain-short`:10phases,
 48s,inspected. Native timestamps include render/readback delays, not presented-frame timing.
- Public packet: `review/atlas/long-tap-appearance-2026-10-12/README.md` and verification.json.
- Previous pushed ebc12d3 passed allten required hosted contexts. CI38271522386 reports saved
  under `.local/ci-ebc12d3-reports`:342library passes27skips;3Atlas passes. The split Atlas
  tests took2m40.34s and3m40.21s, below unchanged6minute deadlines. Inspect Git/PR for the newer
  published revision and its CI state; do not apply ebc12d3 hosted results to newer code.
- CI now includes JUnitXML in the existing14day report artifact; permissions/gates unchanged.

## Implementation and source evidence

- GlassPoseDynamics.kt adds opt-in tapAppearanceSpine / tapAppearanceFallOmega, historical defaults0.
  Calm uses.13rest-width half-spine growth and45rad/s critical fall, alongside existing area.44.
  Rising demand follows already-sprung geometry; interruptions carry value/real-time velocity.
- GlassPhoneTapOpticsTest.kt covers original8+repeat17 long patches with real production shader,
  unchanged6RGB gate, and both-distance interruptions through.34s. Oldrenderer failsbothcases.
- Actual14430-row production export retains every published pose/formation/protrusion field exactly.
  Short/first-long/repeat interior RMS2.14/4.33/3.60, maxima5.01/5.23/5.24. This compares transfer
  over observed bar pixels, not hidden backdrop/edge transport or complete source-scene rendering.
- `tools/compare_phone_transit_appearance.py` reproduces numeric comparison with standard Python.
- `.local/tap-strain-study-v1/v2/v3` and scripts preserve trials. Firstfit.6/60 failed independent
  repeat max7.38. Laterfits use allthree episodes for calibration. No held-out acceptance claim.
- Repeat geometry anchor frame011: centre634px,width365px,height174px at2.796667s; inherited
  endpoints221..748 and nominal40ms input give origin2.676277683s. Timing sensitivity remains.
- The earlier JVM wide-input graph fixes clipped tone-strip sampling; deferred-layer test fails
  oldrenderer and passes corrected panel/container paths. Controlled NVIDIA A/B found~3ms cost.
  Cropping retainedpixels butdidnotspeedup andwasrejected; verifieduncropped code remains.
- No separate selector perpendicular squeeze: that belongs to the bar. Tap/landing deformation
  remains legitimate. Layout/hit targets fixed; measured foreground drawing may deform.
- Both chats were read: Implement Astra iOS parity (`01a0cabb-a5f0-73e2-a03c-27cb059c2571`) and
  IOS Astra (`01a120e8-b510-7e42-8143-a95f8d239e07`). Do not ask for supplied references again.
- Fable/Antigravity preserved in archive/retired-workflows/2026-10-03 and the supplied local
  Antigravity brain779811df-5189-434e-b337-015a75295e06. Raw docs/1.MP4 and2.MP4 stay local.
  Firsttwo long-repeat decodeframes contain private text: NEVER publish them.

## Next work

1. Confirm the latest checkpoint commit/push and its exact-head CI; current sources are locally
   verified. Keep timeout history and pending full-goal limitations in PR6.
2. Address first-use/live-render hitches and measure presentation/input behavior. Do not call
   forced-submission or screenshot timings FPS. Native cold firsttap771ms remains a counterexample.
3. Continue full source-aligned contour/optics/selected-ink validation for allvideo1/video2 gestures.
   Repeated adjacent width20.64px nominal (~15bestalternative) and early endpoint cap recovery remain.
4. Continue every open TODO: all edge/corner envelopes, Android optical equivalence, practical native
   controllers/hosts across stacks, final Atlas visual acceptance, docs and protected final merge.

## Recovery

- Latest: `.local/handoffs/2026-10-13-long-tap-verified/verification.json` (current checkpoint snapshot).
- Earlier77-file long-tap save,100-file in-progress validation snapshot and854-file ebc12d3 snapshot
  remain intact, alongside825/679/155-file older recovery sets. Preserve newer work before restoring.
- Full findings, rejected fits and checks are in research/analysis/MOTION-2026-10-08.md throughStep213.
