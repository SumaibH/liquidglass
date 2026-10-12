# Resume LiquidGlass / Atlas Studio

Updated 12 October 2026. Full iOS parity remains unfinished. Read this before launching builds.
Historical handoffs are preserved in `.local/handoffs/`; this file is the current state.

## Read and respect

- Full objective: `C:/Users/shaya/.codex/attachments/8bcea84d-a196-4b90-8a8b-4627531cee0b/goal-objective.md`.
  It incorporates the earlier objective under `79754856-c646-459c-b409-5e80eee1368e`.
- Canonical checkout: `D:/Work/AndroidStudioProjects/SideProjects/kmp/liquidglass`.
  Branch `codex/atlas-stabilization`; draft PR6. The chat working directory is different.
- Read `AGENTS.md`, `WORKSPACE.md`, `PROGRESS.md`, `TODO.md` and the latest research log entries.
- Work alone: no subagents or computer-use. Terminal and Atlas-owned input/render buffers only.
  No Vitals work. No physical device available. Preserve all staged/unstaged/untracked work.
- Use `tools/workspace.ps1 library ...`; JDK21 is
  `C:/Program Files/Microsoft/jdk-21.0.12.101-hotspot`. Private caches are under `.local/`.
- Commits, pushes and eventual merge authorized. No packages/tags/releases or blanket PR6 review bypass.
  Raw private recordings, page content, names and transcripts stay off GitHub.
- Record measured evidence separately from authored parameters. Do not refit constants silently,
  count isolated green tests as visual acceptance, or restart completed work after an interruption.

## Current verified implementation

The local candidate contains a separate dark-Calm tap pigment/ink response, the JVM wide-input
sampling correction, NVIDIA preference/actual-renderer metadata, and three split Atlas tests.
The short-tap appearance improves; the long-tap appearance still darkens too early.

The wide-input correction removes the inherited hard vertical selector seam. The bar's quarter-size
blur strip now has an explicit offset/scale input in Skia's filter graph. Kernel, through-bar,
substrate mask and controller geometry remain intact. Panel and container paths are covered.
Android/portable shader contracts are unchanged. Earlier lower-edge missing-tone pixels also change;
full-image identity with the old defective renderer is not claimed.

**Complete local inventory: 342 library passes,27 optional skips,zero failures/errors;
3Atlas passes; Android assembly successful.** Original full run was interrupted:
330complete SUCCESS testcase records were recovered before a truncated binary record,
4same-source focused cases reused, and8missing required cases run successfully in a4m50s follow-up.
All369inventory entries were matched; no partial/cancelled case counted as passed.
All43NVIDIA native phases and the static grid capture were inspected.

- `.local/wide-input-final-verification.json`: aggregate and exact case provenance/source hashes.
- `.local/wide-input-interrupted-2026-10-12`: original raw binary, complete recovered records, inventory.
- `.local/wide-input-followup-results`: fresh XML reports; follow-up result/log alongside it.
- `review/atlas/wide-input-2026-10-11`: public seam proof, native33+10/static and verification.
- All tests/captures/helpers are terminal. Revalidate current processes before using any old PID.
  Do not repeat this suite unless a subsequent runtime change invalidates its evidence.

## Performance result and rejected attempt

NVIDIA RTX3050Laptop6GB preference is verified; auto previously chose Intel UHD.
The initial auto/discrete comparison improved static median8.9→4.5ms. These are submission times,
not presented FPS or input latency, and must not be compared across unmatched later runs.

A controlled NVIDIA grid old/new/new/old comparison isolated the wide-input fix:
old medians8.9178/8.5822ms; corrected11.2053/12.3574ms. P95old14.2824/14.8722;
corrected16.2203/16.1027. This is a real ~3ms median cost in that workload; A1 remains open.
An explicit crop before upscaling produced identical pixels but12.2554ms median, so it was rejected.
The exact fully verified uncropped source was restored and compiled. No new full suite is needed
for that rejected/restored experiment. Preserve `.local/wide-bench-result.json`, benchmark timing
JSONs and `.local/GlassWideInput-{before-crop,rejected-crop}.kt` variants.

## Next work

1. Audit current git/PR state and preserve the verified checkpoint with its honest performance limit.
   Last pushed revision before this closeout is44f01de; inspect Git rather than assuming it remains HEAD.
2. Fix long-tap pigment/ink timing using existing source evidence, preserving short taps, held optics,
   bar squeeze ownership and geometry. Do not change controller shape to hide a material error.
3. Continue the full requirement checklist in `TODO.md`: every video1/video2 gesture, all edge/corner
   envelopes, magnifier/selector optics, Atlas UI/latency, native integrations, docs and final protected CI/merge.

## Long-tap evidence already available

- First clean long trip: `.local/tap-optics-long-audit`; public comparison in the tap-optics candidate packet.
  At1.08s source selector38 onbar37, candidate14.45; at1.13s source29, candidate9.98. Worst error23.55RGB.
- Independent repeat: `.local/motion-2026-10-08/video1-long-repeat` has38native frames2.63..3.263333s.
  `.local/tap-optics-long-repeat-audit` has17inspected clear patches008..024 and overlays; selector stays
 45..46 then fades39,36,33,30,27,24,21,19,18. Bar44..34. All patch ranges0..1RGB.
  Firsttwo decoded frames contain private page text; NEVER publish them. Only inspected audit crops are clean.
- `.local/measure-long-repeat-optics.py` reproduces patches. Existing production appearance CSV avoids
  rerunning controller exports just to inspect current traces.
- Hypothesis only: axial travel strain persists after area-driven appearance has decayed. It may need
  its own contribution. Do not adopt an untested law; source brighter than the bar also shows tint-only
  algebra omits optical transport. Original input duration and hidden backdrop are unknown.
- Repeated adjacent-tap width error20.64px nominal/about15px best tested body alternative remains open.
  Endpoint cap-radius recovery is still early (max12.59/11.58px). No full1:1 claim.

## Earlier rejected assumptions and supplied context

- Independent selector perpendicular squeeze was wrong; it belongs to the whole bar. Tap/landing
  deformation remains legitimate. Layout/hit targets anchored; measured foreground drawing may deform.
- Tap-only video1 is distinct from held/early/formed/soft/hard/reversal video2. Do not mix their references.
- JGEY2190n176–182 is not an isolated tap. LOLL8185CFR1944–1988 still shows a lens, so old release timing is invalid.
- Initial bounds test used wrong drawImage offset coordinates. Corrected static-image test passed both;
  only deferred filtered-layer test reproduces old missing input and proves the fix.
- No-mask/no-packed experiments retain seam; no-through/no-wide remove it but are invalid production fixes.
- Competing sources already supplied: `archive/retired-workflows/2026-10-03/README.md` and
  `C:/Users/shaya/.gemini/antigravity-ide/brain/779811df-5189-434e-b337-015a75295e06`.
- Both chats read: Implement Astra iOS parity (`01a0cabb-a5f0-73e2-a03c-27cb059c2571`) and
  IOS Astra (`01a120e8-b510-7e42-8143-a95f8d239e07`). Do not ask for sources again.

## Recovery copies

Verified825-file snapshot: `.local/handoffs/2026-10-12-after-wide-tests/verification.json`.
Earlier679-file and155-file snapshots remain under11October folders. They contain exact dirty files,
patches, reports, objectives and hashes. They are recovery material, not a release. All failed
attempts and subsequent changes are in `research/analysis/MOTION-2026-10-08.md` throughStep179.

Latest checkpoint recovery: `.local/handoffs/2026-10-12-verified-checkpoint/verification.json`
includes the subsequent A/B, rejected crop, updated handoff and strict docs result. The earlier
825-file snapshot remains unchanged. Repository links (38 targets), strict MkDocs and staged
whitespace checks pass. Inspect Git/PR for the published revision and hosted check status.
