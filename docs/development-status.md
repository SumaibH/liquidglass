# Development status and visual guide

Updated **13 October 2026**. This describes the unreleased source, not the published 0.1.0 artifact.
Full iOS parity, final owner acceptance and arbitrary-stack support are not established. Historical
research remains available; this page identifies which later changes supersede its implementation.

## Atlas and the current material

![Native Windows Atlas grid scene](https://raw.githubusercontent.com/shayann07/liquidglass/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/wide-input-2026-10-11/static.png)

Native app-buffer capture from the clipped-input checkpoint. The grid shows continuous magnification;
the card and navigation remain anchored. This resting view is not proof of continuous motion or FPS.
Atlas now selects a discrete GPU where available and prints the actual adapter. The owner's tested
hybrid machine selected NVIDIA RTX 3050; an OpenGL override selected Intel and was not an equivalent
backend comparison. [GPU evidence](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/gpu-2026-10-11/README.md).

## Features and integration paths

| Use case | Entry point | Responsibility and scope |
| --- | --- | --- |
| Compose app | `GlassScene` and scoped `Modifier.glass` | Shared recorded backdrop; host keeps layout, actions and semantics |
| Explicit Compose control | `rememberLiquidGlassState`, `liquidGlassSource`, `Modifier.liquidGlass` | Existing API and historical presets retained |
| Free magnifier | `GlassStyle.clearLens` / native `DrawLens` | Continuous mapping; distinct from a held navigation selector |
| Calm navigation | `GlassTabBarStyle.Calm()` | Tap/held travel, landing recovery, whole-bar off-axis strain and separate appearance |
| Portable production renderer | CanvasKit `createGlassScene` / `createGlassPainter` | Production shaders, explicit source, density, lifecycle and input |
| Lightweight browser lens | `ports/web` | Dependency-free lens preview; not the full material |
| Native .NET | `GlassPainter`, `CalmInteraction`, `GlassFeedback` | SkiaSharp material and whole-surface input; host schedules redraws |
| Existing WinForms controls | `ControlBackdrop.Capture` | Owned control subtree only, explicit invalidation/lifetime and supported-type limits |

[Compose setup](getting-started.md), [API](api-reference.md), [generic interaction](generic-interaction.md),
[cross-stack contracts](porting.md) and [.NET setup/examples](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/ports/dotnet/README.md).
No adapter silently captures the system desktop or another application.

## Navigation shape and appearance

**13 October: first continuous comparison.** Both iOS recordings were replayed gesture by gesture
through the real renderer at a deterministic 60 fps and compared as sequences (taps, stationary
hold, early and formed throws into both walls, held reversals, the soft landing on Contacts).
Five visible defects in the handoff build were corrected in `GlassTabBarStyle.Calm()`:

- a tapped selector now travels as refracting glass instead of fading to an invisible ghost;
- a thrown lens stays formed through travel and the wall impact, then becomes the inset;
- wall landings are a wide flattened capsule, not the tall circle of the former impact model
  (kept as `GlassPoseSpec.CalmImpact` for comparison);
- the grasp follows a fast throw closely (default 78/s instead of the authored 40/s);
- the held rim is a soft refractive band, not a white specular outline;

and the dark material was recalibrated to the recordings: bar 45 at rest and 53 held, resting
selector 28 against the reference's 24–30 (previously 6–8).

![Before and after at the same replay instants](https://raw.githubusercontent.com/shayann07/liquidglass/codex/atlas-stabilization/review/atlas/reference-replay-2026-10-13/before-after.png)

Our renders only; the private reference is not published. Finger timing is inferred from the
recordings. Method, numbers and the remaining differences (bar/search fusion while held, weaker
stretch on fast reversals, later hold onset, smaller ink magnification in transit, more smeared
text under the bar) are in the
[reference-replay record](https://github.com/shayann07/liquidglass/blob/codex/atlas-stabilization/review/atlas/reference-replay-2026-10-13/README.md).

Earlier work this builds on: resting tap travel and held formation are separate; press expansion
and whole-bar strain are separate, and the selector gets no independent perpendicular squeeze;
layout and hit targets stay fixed. The 12 October tap-appearance calibration (25 interior patches,
repeated short-tap width residual up to 20.64 px) predates this comparison and is superseded for
tap optics by the change above.

### Evidence map

| Change / investigation | Primary record |
| --- | --- |
| Continuous magnifier, calm controls, Atlas redesign, original extreme envelopes | [Stabilization record](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/ATLAS-STABILIZATION.md) |
| Coupled short/long tap shape | [Paired original/source comparison](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/phone-tap-volume-2026-10-09/README.md) |
| Both endpoint landings and remaining cap recovery | [Landing sequence](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/phone-landing-sequence-2026-10-09/README.md), [shape-aware edge](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/phone-edge-2026-10-09/README.md) |
| Rejected dark-disc appearance and corrected substrate tint | [Native before/after](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/inset-material-2026-10-10/README.md) |
| Repeated adjacent-trip timing and residual width error | [Short-tap audit](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/short-tap-timing-2026-10-10/README.md) |
| Separate tap pigment/ink, calibration and verification | [Initial optical audit](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/tap-optics-audit-2026-10-11/README.md), [candidate](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/tap-optics-candidate-2026-10-11/README.md), [long-tap recovery](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/long-tap-appearance-2026-10-12/README.md) |
| GPU clipping seam and retained blur/input behavior | [Clipped-input regression and native proof](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/wide-input-2026-10-11/README.md) |
| Rendering cost, natural recording, retained selection failure | [Frame/input investigation](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/frame-cost-2026-10-13/README.md) |
| Portable production shaders and scene | [CanvasKit guide](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/ports/skia/README.md) |
| Native .NET material, motion and controls | [Painter](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/dotnet-2026-10-10/README.md), [calm interaction](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/dotnet-calm-2026-10-13/README.md), [native backdrops](https://github.com/shayann07/liquidglass/blob/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/dotnet-controls-2026-10-13/README.md) |

These links are pinned to a published source checkpoint so the evidence remains reproducible.
For later status, follow the repository's [progress](https://github.com/shayann07/liquidglass/blob/codex/atlas-stabilization/PROGRESS.md)
and [full checklist](https://github.com/shayann07/liquidglass/blob/codex/atlas-stabilization/TODO.md).

## Native .NET interaction and controls

![Nine native interaction phases](https://raw.githubusercontent.com/shayann07/liquidglass/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/dotnet-calm-2026-10-13/native-phases.png)

`CalmInteraction` takes logical bounds, cumulative displacement and monotonic seconds. It preserves
release/re-grab continuity, viewport containment, reduced-motion reset and resize cancellation.
The native implementation matches 144 portable states and 107,520 independent outline probes. Twelve
native check groups and nine app-owned phases pass. Held/release images match exactly; recovery
and reduced motion return to baseline, with 1,092 foreground pixels remaining anchored.

![Native controls: before, text change, state change, glass and resize](https://raw.githubusercontent.com/shayann07/liquidglass/b46207d665725a0aa841102bfbdcbe74aaf4ac3c/review/atlas/dotnet-controls-2026-10-13/comparison.png)

The separate WinForms adapter records an owned Panel/Label/TextBox/CheckBox subtree. Actual TextBox
glyph changes alter 1,522 pixels within its bounds, independently of background changes. Identity
lens pixels, refraction, resize, retained-source failure and UI-thread checks pass. The example
keeps native controls interactive beside their glass preview. RichTextBox/ActiveX are rejected;
hidden children, custom controls and overlapping container order retain documented limits.

This completes usable whole-surface feedback and one native control-capture path. Native navigation
selector travel, additional frameworks, high-level portable refracted foreground and universal
widget capture remain incomplete. No NuGet package was published.

## Verification and performance

| Evidence | What it proves | What it does not prove |
| --- | --- | --- |
| Inspected cea1913 hosted XML: 344 library passes, 27 optional skips, 3 Atlas passes; Android build | Exact regression/build checkpoint | Full iOS equivalence or physical Android validation |
|43 inspected NVIDIA native motion phases | App-owned rendering and selection/layout assertions | Continuous matching trajectory or presented FPS |
| af333fa: all ten required hosted checks green | Native calm update passes Windows/Linux, docs/security and existing build gates | Later commits need their own CI |
| b46207d: all ten required checks green, including native Windows controls; five local states inspected | Standard-control capture, glyph updates, identity/refraction and failure guards | Other control types, hosts and display performance remain unverified |
| Controlled wide-input old/new/new/old comparison | About 3 ms additional median redraw submission cost | Display latency or all-scene performance |
| Natural recording and JFR | UI picture-recording/dispatch observations; 491 ms Direct3D lock wait inside one forced redraw | GPU execution/presentation timing or a complete lag diagnosis |

The original local V3 gesture timeout is preserved alongside its unchanged-source passing retry.
One natural replay failed bottom-right selection; two later observed replays passed. That failure's
cause remains unresolved. Windows refused the process-filtered PresentMon ETW session; no display
trace was produced and no permission changes were made. See [performance](performance.md).

## What remains

1. Close the remaining differences found by the continuous comparison: held bar/search fusion,
   reversal stretch and refraction, hold onset, transit ink magnification, under-bar detail.
2. Verify all directional extreme envelopes and Android composition on appropriate hardware.
3. Resolve ordinary interaction/display performance and final wide/narrow Atlas visual acceptance.
4. Complete native navigation and additional host integrations with executable evidence.
5. Finish the final requirement audit and protected PR review/merge. No release/tag is authorized.

The current checklist has 27 completed and 22 open entries of unequal size; this is not a progress
percentage. A passing checkpoint does not narrow the full goal.

## Keeping this documentation current

Each behavior/API change must update its user guide, this status/evidence map, CHANGELOG and the
relevant limitation before publication. Visible changes require app-owned screenshots at stated
size/backend, captions that distinguish measured source from candidate output, reproduction commands
and an exact verification record. Failed attempts stay labelled and historical claims retain their
original scope. Raw/private recordings and full profile traces stay outside the published site.
The maintainer checklist is in [CONTRIBUTING](https://github.com/shayann07/liquidglass/blob/codex/atlas-stabilization/CONTRIBUTING.md).
