# Continuous reference replay — 13 October 2026

The first evaluation of the Calm navigation selector against the owner's two iOS recordings
(`docs/1.MP4`, `docs/2.MP4`, private and git-ignored) as **continuous sequences**, not selected
frames. It found five visible defects in the handoff build and corrected them. Owner visual
acceptance is still pending.

![Before and after at the same replay instants](before-after.png)

*Left: handoff build `efcc3b5`. Right: this change. Both rendered by `PhoneReplay` from the same
inferred input script on an NVIDIA RTX 3050 (Direct3D) at a deterministic 60 frames per scripted
second. The private reference is not shown; the three-row comparisons including it stay in
`.local/comparisons/`.*

## Method

1. **Scene.** `PhoneScene` (sample/shared) reproduces the reference geometry measured at 3 px/pt:
   a 278 × 62 pt bar at x = 21 pt, a 62 pt search button, three tabs, a call list or keypad on
   black. Content is neutral placeholder text. Colours are in the recordings' own (dimmer)
   brightness space so levels can be compared directly.
2. **Input.** The reference has no touch data, so finger timing is **inferred** from the
   selected (blue) ink's centroid: tap onsets for video 1 (60 ms taps beginning at visible
   motion onset), and for video 2 a press at the start of the held shift, a finger path that
   follows the ink while travelling, and a stop-then-lift on arrival. Catalogue of video 2: one
   stationary hold, early throws (including the soft landing on Contacts at 6.7 s), fourteen
   formed throws into both walls, and held reversals from 18.7 to 22.3 s.
3. **Rendering.** `./gradlew :desktop:phoneReplay -Patlas.replay.script=… -Patlas.replay.out=…`
   drives Compose with a virtual clock: frame N is composed at N/60 s however long it takes, and
   is read back from an offscreen GPU target on the layer's own context (about 50 ms per frame,
   2 minutes per recording). **This says nothing about interactive frame rate, input latency or
   display presentation.**
4. **Comparison.** Three-row video (reference / before / after), 0.25× episode excerpts, paired
   bar-crop strips at 33 ms spacing, and median grey levels in fixed regions over black.

## Findings and changes

| # | Observed in the reference | Handoff build | Change (Calm only) |
| :-- | :-- | :-- | :-- |
| 1 | A tapped selector travels as clear, refracting glass that magnifies the ink it crosses, at roughly resting size | Pigment faded to an almost invisible ghost; optical formation capped at 0.25 | `GlassTabBarStyle.tapOptics = 1`: transit appearance drives the lens optics; geometry unchanged |
| 2 | A thrown lens stays formed through travel and the wall impact and becomes the pill about 170 ms after landing | Formation target dropped to 0 at lift-off; the lens collapsed in flight | `GlassPoseSpec.releaseFormationSpeed = 3` slots/s: released formation follows travel speed and only subsides |
| 3 | Wall landing is a wide, flattened capsule pressed into the end | The impact model converted speed into cap growth: a tall circle protruding above and below the bar | Impact exchange off (`releaseImpactGain = 0`); previous values kept as `GlassPoseSpec.CalmImpact` |
| 4 | The lens sits under the (inferred) finger during fast throws | The authored 40/s grasp spring trailed by ~100 ms at release | Default 78/s grasp; ~1–2 frames remain on the fastest throws |
| 5 | Soft refractive rim with a faint top highlight | A crisp white specular outline | Held-lens specular 0.49 → 0.2 |
| 6 | Bar 45 at rest / 53 held; resting selector 24–30; search 44 (grey levels over black) | 32 / 44; selector 6–8; search 34 | Calm dark: bar lift 45/255, held lift 8/255, selector black 0.42 with no negative lift. Now 45 / 53 / 28 / 47 |
| 7 | Page text under the bar is one soft, legible copy | The bar's authored straight-run fold (`edgeFold` 0.8) doubled and reversed it into a garbled smear | Calm bar `edgeFold = 0`; V3 keeps 0.8 |
| 8 | A tapped icon reads 79/69 px (1.145×) inside the moving selector | With lens optics on, the tap ink scale and the lens magnification compounded to 1.27× | Ink scale compensates for transit optics; held scale unchanged |

Rows 1–5 and 7 are behaviour observed in continuous sequences; their rates and gains are
**authored** to match them. Rows 6 and 8 are **measured** from these recordings (row 6 at eight
resting positions across both videos: selector 24–31 on a 42–45 bar). Values derived from older stills
(V3's 32/44 plateau and "0.5 × bar − 10" selector law) are left unchanged in `V3()`.

### Pass 2: the bar fuses with the search button

Video 2 at 1.37–2.23 s: while held, the bar grows (about 5%), closes the 8 pt gap to the separate
search button and joins it through a neck about 61 px (20 pt) tall at its narrowest; the neck
carries the bar's material (52) right up to the button's edge, where the button's own glass (44)
begins. It parts within about 70 ms of the bar shrinking. New opt-in API:
`GlassTabBar(trailingAccessory = GlassTabBarAccessory(62.dp, gap = 8.dp))` makes the bar's outline a smooth union
(6 dp) with the button's circle, anchored so only the bar's growth closes the gap.

At 1.80 s the rendered top silhouette across the neck matches the reference within 1 px at every
sampled column (x 840–936) and the narrowest neck is 59–61 px against 61 px. Differences: the
neck forms later and parts later than iOS (it follows our bar press timing), a brighter rim line
crosses it where the button's edge lies, and during holds on Keypad iOS shows no neck in some
frames (confounded by the overhanging lens; not resolved). iOS's bar also appears to grow more
on the finger's side (48 vs 26 px at one row while holding Calls); ours grows symmetrically.

### Pass 3: fast held reversals

Video 2 at 18.7–22.4 s: the finger sweeps Calls ↔ Keypad about every 0.3 s. In iOS the lens stays
under the finger, stretches to roughly 1.6–1.8 slots and flattens to about the bar's height, with
strong refraction at its ends. The blue-ink centroid is a poor finger proxy here (it snaps between
tabs mid-sweep), so this episode's input was rebuilt from its 24 unambiguous turning points (ink at
Calls or Keypad) with cosine sweeps between them, plus one partial first stroke peaking at 627 px
(`tools/reference-replay/reversal-script.txt`; **inferred**).

With the rebuilt input, most of the difference was the model trailing the finger. Changes (Calm
only, authored): grasp 120/s (was 40, then 78) and held formation rise 50/s (was 30; taps keep
30/s). A larger stretch gain (0.45 slot at 16 slots/s, 60/s spine, squeeze 0.6) was tried: it
matched the reversal length slightly better but broke the measured IMG6735 flattened pair
(314 × 142 px on the five-tab bar) and the 136 px compression bound, so stretch and squeeze stay
at 0.28 and 0.47.

Like-for-like (same rebuilt input, previous commit `c65c212` against this one): the median gap
between lens centre and inferred finger falls from 77 to 37 px during the reversals and from 97 to
68 px during formed throws; maximum lens width is unchanged (394 px). Most of the earlier "too
short, too far behind" impression therefore came from the ink-centroid input, not the model. With
the rebuilt input the lens follows the sweeps, stretches across Contacts–Keypad and flattens
toward the bar's height. Remaining: iOS warps icons more strongly
near the moving lens's ends; our lens still trails the inferred finger by about a frame at each
turn; the hold onset relative to the (inferred) finger-down is later than iOS.

### Pass 4: lens edge and interior optics

![Held and moving lens before and after pass 4 (our renders only)](lens-edge.png)

Measured in video 2 at full resolution, over columns where both pages are black outside the bar:

| Region (grey level) | iOS | Before | After |
| :-- | :-- | :-- | :-- |
| Bar rim at rest, outermost 4 px | 82, 72, 64, 53 | 84, 117, 90, 61 | 82, 80, 68, 57 |
| Held lens outer edge line | 62 | 27 (specular .2) | 63 |
| Band inside it, 3 px steps | 29, 21, 17, 14, 11, 9 | 21, 4, 3, 3, 2, 2 | 29, 16, 13, 11, 10, 8 |
| Page text inside the lens, p95 / p99 | 104 / 114 | 76 / 79 | 107 / 110 |

The bright white outline seen in pass 1 was the **bar's own rim** (about twice too bright) seen
through the lens, not the lens's specular; pass 1 had dimmed the wrong term. Changes (Calm):
bar bevel peak and specular halved; held lens specular .26, glow ×2 with a new
`GlassStyle.heldGlowReach` of 6 dp (new `uHeldGlowReach` shader uniform; 0 keeps the historical
bevel-length glow, so every other preset is unchanged) and `rawShare` .28 (was .01) so the page
shows through the lens as clearly as in iOS. Port shaders were regenerated; CanvasKit tests and
the .NET checks pass.

Not solved: near the ends of a **moving** lens iOS swirls and strongly magnifies the icons it
passes over. On the pose path our band is a fixed measured map (0.6 R, stepped seam from the held
stills), so this needs a different optical map in motion — the next optical task. Also visible:
a vertical seam where that map hides part of the band, and a faint doubled copy of page content
near the lens's left end (raw share over the mirrored band).

### Pass 5: the moving lens's map

![Moving lens before and after pass 5 (our renders only)](moving-lens.png)

At the ends of a travelling iOS lens the rim shows the lens's own nearby content **mirrored and
wrapped along the cap** (blue icon strokes following the curve), and every icon under the lens
shows its selected colour. Ours used the held map (exterior pulled in, measured on still holds)
and drew only the active item's selected ink, so icons were clipped at the cap and the one being
left stayed grey. Tried and rejected: a stronger, wider held band (no visible effect on the pose
path), and the measured cover-sheet fold (its constant-source seam zone stretched labels into
tall streaked glyphs, even when confined to the caps).

Adopted (Calm dark, authored):

- `GlassStyle.rimMirror` / `uRimMirror`: within a fraction b of the band a pixel at depth u shows
  depth 2b − u (a reflective rim; continuous, no seam), identity inside. b = 0.3.
- `GlassStyle.foldAtCorners` / `uFoldCorners`: confine that to the cap arcs; long top and bottom
  runs keep a thin band, as iOS does.
- An unheld moving selector (`tapOpticsFold`) uses it at its transit strength; a held lens keeps
  the measured held map when still and blends toward the rim mirror at its caps with its travel
  speed (new `heldTravel` signal, optics only).
- `lensInkAll`: every item's selected ink inside the aperture.
- Dispersion is off while the rim mirrors; with the band's split it produced saturated orange and
  blue wedges where iOS shows faint fringes.

Both new uniforms default to 0, leaving every other preset unchanged. Still different: iOS's cap
swirl is larger and carries faint rainbow fringes; ours is smaller and colourless.

### Pass 6: wall landings and the resting inset (14 October)

![Right-wall landing before and after pass 6 (our renders only)](landing.png)

*Our renders only (`447d65e` above, this pass below), video 2's first formed throw into the right
wall; 60 fps deterministic GPU replay of the inferred input. Crops 440 × 210 px around the end.*

Measured on one clear row (y = 568 px, between the icons and labels) of video 2, across six
consecutive right-wall landings (2.3, 9.9, 12.4, 14.9, 25.6 and 28.6 s) and eight resting
positions:

- **Resting inset:** 285 px (95 pt) wide; end insets sit 8 pt from the bar's ends (centres 218,
  480 and 742 px). `PhoneScene` used a slot-wide 266 px pill with 6 pt ends, so every resting
  frame was 19 px narrow. `PhoneGeometry` now sets `pillWidth = 95.dp`, `contentPadding = 8.dp`;
  resting edges now match the reference to within 1 px (603 / 880 against 603 / 881).
  This is scene geometry: library defaults are unchanged.
- **Landing:** the pill reaches the wall and presses against it (its outer edge on the bar's end
  for about 130 ms) while its trailing edge keeps moving toward the wall, about 42 px past its
  rest (to 645 px), then returns over about 0.35 s without overshooting. The leading edge then
  sits at its rest position. Width falls to about 237 px on this row (241 full width), and the
  height never grows. Slower landings narrow less (28.6 s: 3 px; the early throw at 7.7 s: 11 px).
- **Ours (`447d65e`):** the leading edge flew about 77 px past the bar's end (the free centre
  spring overshooting) and the trailing edge never overshot; no narrowing.

Adopted, Calm only (`GlassPoseSpec.Calm`; previous behaviour kept as `CalmFreeLanding`):

- `GlassPoseSpec.releaseTrailingEdge`: after a fast release toward an end tab, the leading edge
  recovers on the centre spring and stops at the bar's end (a wall, resolved after each step so
  it does not depend on the frame cadence); the trailing edge recovers on its own spring from
  its actual position and velocity. The spine is what the edges leave beside the caps, so the
  pill narrows from behind without the impact model's area exchange (no tall circle).
- Trailing-edge rate 13/s, critically damped, **authored** against the measured consecutive width
  series already in `GlassPhoneLandingSequenceTest` (241 → 285 px over 0.47 s, both walls):
  rms 3.4 / 3.2 px. 12/s and 14/s gave 4.0 and 7.3 px. Not a measured Apple constant.

Tried and rejected: the impact model's spine impulse without its area exchange collapsed the
pill into a 171 px circle; an underdamped trailing spring (ζ 0.35–0.7) cannot make a release at
rest narrow without undershooting the fast case by 50+ px.

In the full replay, landings where the inferred finger is still moving at lift (2.3, 12.4 s)
now narrow like iOS (peak 660 / 637 px against 645 / 646), about 100 ms early. Landings where
the inferred finger stops at the end tab before lifting (9.9, 25.6 s) release slowly and do not
narrow, while iOS narrows by the same 42 px. The ink tracker cannot see the finger past the end
tab (the ink stops when the lens reaches the wall), so the script's stop-before-lift is probably
an input artefact on those throws; it is recorded here rather than tuned away.

### Pass 7: input, cap swirl, finger-side growth and Atlas Studio (14 October)

**Inferred input.** Finger-down is invisible in the recordings, so a held press now begins 137 ms
before the ink's visible held shift (our 120 ms hold threshold plus a frame): hold onset is
aligned **by construction** and is no longer a test of the model. Into an end tab the finger now
lifts one frame after the ink first reaches the tab, still moving: the ink stops there when the
lens meets the wall, so it cannot show the finger beyond it. With this input the wall landings at
2.3, 12.4 and 25.6 s narrow like iOS (trailing-edge peaks 670 / 647 / 655 px against 645 / 646 /
646). At 9.9 s ours narrows only about 10 px against iOS's 42. Frame by frame, iOS's lens arrives
about two frames earlier and much more stretched (about 1.3 times its resting width, its leading
end pressed past the bar's end), then contracts from behind straight through its resting width;
ours arrives shorter, so its trailing edge has less to recover. Our travel stretch is held to the
measured IMG6735 pair (pass 3), so this stays a documented difference rather than a refit. `tools/reference-replay/v2script.py` and
`v2-script.txt` are updated; the reversal episode is spliced in by the generator.

![Moving lens before and after pass 7 (our renders only)](cap-swirl.png)

*Our renders only, the same rebuilt reversal input; `63d5d2b` above, this pass below.*

**Cap swirl.** The moving lens's reflective rim now mirrors the whole band (1.0; was 0.3; the shader
now allows up to 1), so icons near the caps wrap along a larger arc, as in iOS. A
faint colour split (0.025) is confined to the mirrored band. Authored; iOS's swirl is still
somewhat stronger.

**Finger-side growth (tried, not adopted).** Holding Calls, the iOS bar's top row grows 43 px on
the finger's side and 25 px on the other, about 8% wider overall against 5% taller: iOS grows
the bar more horizontally than vertically, with a bias toward the finger. Ours grows 28 / 24 px.
Shifting the pressed material toward the finger (an authored 0.04 share of the touch offset)
gave 35 / 13 px, but it starved the far side, so the measured fusion neck to the search button
(taken while holding this same tab) fell below its test, and two measured bar envelopes broke.
Matching iOS needs anisotropic press growth, which would conflict with the measured 1.05 press;
the shift was removed.

**Finger-side growth (adopted).** A closer look at the same rows: iOS's far side grows exactly as
a plain 1.05 press does (25 px against our 24); only the finger's side grows more (43 against 28).
So the bar reaches toward the finger rather than moving. New opt-in
`GlassInteraction.pressReach`: while pressed, the side nearer the touch grows by up to that share
of the width more (scaled by the touch's offset from the centre), with the far edge held where
the ordinary press puts it. Dark Calm's bar uses 0.03 (authored from these rows). Bar edges on
three rows, our replay against iOS, in px:

| Held tab (time) | iOS left / right | Before | After |
| :-- | :-- | :-- | :-- |
| Calls (1.80 s) | 58 / 883 | 73 / 882 | 62 / 881 |
| Calls (5.75 s) | 58 / 883 | 77 / 881 | 63 / 880 |
| Keypad (3.90 s) | 76 / 901 | 88 / 892 | 78 / 897 |

The fusion neck, the bar-envelope tests and the extreme full-screen pulls are unchanged: the
touch is bounded to the panel and the reach is drawing only.

**Atlas Studio.** Redesigned around the material: one full-window scene moving under all the glass
(a drifting night sky with a moon, a page of type that scrolls by, sliding coloured lines), the
Calm bar fused with a round magnifier button as the only scene switcher, a glass top bar and an
anchored card. A clear magnifier being dragged now takes no press glow or press scale: both
hazed and resampled its view. [Recording](atlas-studio.mp4) (deterministic GPU replay, 9 s).

### Re-reading the landing evidence

The removed impact model was calibrated to one frame, video 2 at 2.5567 s ("frame082": width
242 px, a left-cap fit of R 116.14 px; its full height was recorded as unobservable). In the
continuous sequence that frame is the **dark resting inset about 0.33 s after the wall impact**,
briefly narrower than its 285 px rest and still inside the bar. It is not a glass circle during
the impact. The narrowing itself is real and is listed below.

## Remaining differences

- Post-landing narrowing (pass 6) is reproduced for fast releases; replay landings whose
  inferred finger stops before lifting do not narrow (likely an input artefact, see pass 6), and
  ours narrows about 100 ms earlier than iOS.
- iOS's swirl at the ends of a moving lens is still somewhat stronger (pass 7).
- The rebuilt reversal input and the full video 2 script (`v2-script.txt`) are inferred; the
  per-episode lag numbers above are relative to that inference.
- Hold onset is aligned by construction (pass 7); it cannot be tested from these recordings.
- Selected ink inside a moving lens is magnified less than iOS.
- Page text under the bar is slightly more smeared than iOS.
- Calibration conflict, unresolved: an earlier early-transit patch (video1 frame 6) reads the
  inset at 0.38 of the bar, against 0.57–0.69 at rest. Its test keeps the previous pill law.
- The repeated long tap's pigment returns about 50 ms (≈7 grey levels) later than the measured
  patches with the corrected pill; its test also keeps the previous pill law.
- Android is not re-verified; no physical device is available.

## Verification

- Full library suite before the bar-fold change: 374 cases, 347 passed, 27 optional showcase
  skips, 0 failures (`:liquidglass:jvmTest --rerun`, 12 m 46 s).
- After the bar-fold change, every tab-bar, Calm, Phone, bar, ink, endpoint, material and replay
  class: 167 cases, all passed. `:desktop:desktopTest` and `:sample:assembleDebug` passed.
- Six earlier tests asserted the replaced behaviour. Three now target the retained
  `GlassPoseSpec.CalmImpact` (impact landing, its middle-landing isolation, the measured post-landing
  narrowing), two keep the previous pill law (see the calibration conflict above) and the Calm
  material reference now checks the new 53 held level. `GlassContinuousReplayTest` covers the new
  behaviour.
- Tests check behaviour and stated envelopes; owner visual acceptance is separate and pending.

## Files

- `before-after.png`, `lens-edge.png`, `moving-lens.png`, `landing.png`, `cap-swirl.png`,
  `atlas-studio.mp4` — public, our renders only.
- Local only: `.local/comparisons/2026-10-13-baseline/` (first comparison, original scripts) and
  `.local/comparisons/2026-10-13-pass1/` (reference / before / after).
- Harness: `desktop/src/jvmMain/.../PhoneReplay.kt`, `sample/.../PhoneScene.kt`; script generators
  and tracker are in `.local/sxs/` with the private recordings they read.
