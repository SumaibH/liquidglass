# iOS parity audit — 26 October 2026

Every visible or measured difference found between iOS (26/27 recordings) and liquidglass `main`
(`34127d8` plus the uncommitted lens band 1.1 under test), state by state. The owner asked for a full list
before any more fixing (26 October: "first compare and find issues properly … at least 10+ mismatches";
then "keep finding until u find 100+ good mismatches").

**How it was compared.** iOS: the private recordings (docs/1, docs/2, IMG_6756, JGEY2190, LOLL8185,
COSG3073, IMG_7048, LQCO6923), HDR10 decoded to display sRGB with the `.local/sxs/hdr.py` chain. Ours:
`:desktop:phoneReplay` renders of the same inferred input (desktop GPU, 60 fps; folders `p20`,
`r77-lband*`, `r73-f0`, short-script renders), and Pixel 7 screenshots (`PhoneActivity`, light and dark;
`CalibrationActivity` over the same calibration chart iOS was filmed through). Side-by-side sheets were
inspected by eye at 1x-2x; numbers come from the scorers named per item (`levels.py`, `tapprofile.py`,
`tools/reference-replay/refraction.py`, `tools/measure_rim_fringe.py`, the replay scoreboard). No private
frame is committed: the call-list pages show real names.

Each item: **what iOS does / what we do** — evidence. *S* = scored (a number exists), *E* = by eye.
Severity: **H** obvious at a glance, **M** visible side by side, **L** subtle or measured only. **V** marks an
item seen in one frame or sheet only: confirm it on a second recording before fixing.
GitHub issues group these by area (see the end).

## A. Held lens, still (optics)

1. **H, S** **[done 26 Oct, see fringe-refraction-2026-10-26]** Rim colour fringes: iOS shows saturated rainbow fringes (red/green/blue, 2-6 px) along the held lens's top and bottom rims at rest; ours shows none. Fringe scorer: iOS fringed 22-90 % of frames, ours 0-3 % (#73).
2. **H, S** **[done 26 Oct, see fringe-refraction-2026-10-26]** Rim refraction strength: iOS pulls the bar's straight edge ~+11 px toward the lens centre; ours -2.2 px at band 0.3 (#77). Band 1.1 reaches +9.8-+11 but see 3-5.
3. **H, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** Rim refraction *shape*: iOS bends the bar's edge into a curve that meets the lens rim at both ends (a crescent following the capsule); ours keeps it a straight horizontal line across the whole lens, at every band tried (IMG_6756 35.5 s, 43.8 s; LOLL8185 24.3 s).
4. **H, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** With the band at 1.0-1.1, content near the rim (labels, icons) breaks into displaced fragments over content pages ("Contac" + "ts", Pixel 7, 26 Oct); iOS compresses and curls it smoothly along the rim. The band alone cannot be the fix.
5. **M, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** The dark band inside ours at band 1.1 reads as a flat slab at the bottom; iOS's is a thin curved crescent, thicker at the top than at the bottom (IMG_6756 35.5 s).
6. **H, E** **[done 26 Oct, see rim-outline-2026-10-26]** Rim outline: iOS draws a thin, crisp, dark outline with a brighter inner line; ours is a soft grey ring 2-3x wider and lower in contrast.
7. **M, E** Rim specular: iOS's lens catches a bright highlight along its upper-left and lower-right rim; ours has no directional highlight.
8. **M, S** **[measured 26 Oct: matches at holds; the IMG_6756 drag gap is the inferred input, see search-neck-2026-10-26]** Bar growth while held: iOS's bar grows 4-13 px taller when a lens is held, so the lens overhangs it less at the bottom; ours does not grow, the lens hangs 1-3 px deeper (refraction record h 13.7 vs 12.3 px).
9. **M, E** **[measured 26 Oct: within a few px]** Lens size: on Keypad iOS's held lens is ~350 px wide (IMG_6756 35.5 s), ours ~335 px and its centre sits ~10 px left.
10. **M, E** **[done 26 Oct: iOS does join, with a shallow notch; sample 24 dp]** Fusion with the search button: ours joins the button through a neck when held at the trailing end (sample `lensFusion` 12 dp, from the light IMG_7048); iOS 27 dark (IMG_6756 35.5 s, docs/2 30.0 s, LOLL8185 21.3 s) shows the lens overlapping the button with no neck.
11. **M, S** Held lens over page content reads 5 levels darker than iOS (docs/2 avatar 61.4 vs 65.3; #72); iOS's lens brightens what it shows.
12. **L, E** Lens interior on black pages: iOS's inside reads slightly clearer/brighter at the centre with the bar texture visible to the rim; ours is a uniform grey.
13. **L, E** Ink magnification under the lens: iOS 1.22-1.24, ours 1.20-1.22 (refraction record, icon fit).

## B. Held lens, moving (drag)

14. **H, S** **[done 26 Oct, see fringe-refraction-2026-10-26]** Fringes do not grow with speed: iOS's fringe energy rises 5-30x from still to fast; ours stays flat (fringe scorer, all five recordings).
15. **H, E** Mirror swirl inside a moving lens: iOS mirrors the neighbouring icon into the lens's leading cap as a smooth curl with colour (LOLL8185 26.5 s, docs/2 8.6 s); ours draws a hard-edged blue blob and cuts labels ("Corτacs", "C...s", LOLL8185 22.2 s; docs/2 8.6 s), worse at band 1.1.
16. **M, E** Labels inside a moving lens: iOS keeps the label legible and bends its ends along the bottom rim; ours clips or squashes it at the bottom rim.
17. **M, E** **[likely the same script mismatch as 33; re-check against the recording's own script]** Lens formation onset in a drag-from-rest: iOS's lens is fully formed and moving 0.2 s after touch-down (LOLL8185 21.3 s); ours is still a pill at the start position at the same moment.
18. **M, S** Drag tracking: selector path vs iOS's ink rms 9.2-31.6 px depending on recording (scoreboard), best time shift -8 ms.
19. **M, E** Moving lens shape: iOS's lens stretches into a slight oval (taller in the middle) at speed; ours stays a straight-sided capsule.
20. **L, E** At reversals iOS's lens flattens to about the bar's height; ours keeps more height (refraction record: bimodal readings at fast reversals).
21. **L, S** Neck timing to the search button: forms -50/0/-67 ms, parts 0/-33/+100 ms against iOS (docs/2 scoreboard); 34.8 s 100 ms late.

## C. Tap (pill in transit)

22. **H, S** **[done 26 Oct, see fringe-refraction-2026-10-26]** Transit fringes: iOS's travelling pill shows large rainbow streaks on its top and bottom rims and over the ink; ours peaks at 0.09 of iOS's fringe energy (docs/1 at band 1.1; 0.02 at 0.3).
23. **H, E** Ink smear in transit: iOS stretches the destination's ink upward into "flames" along the top rim and streaks the label at the bottom (docs/1 1.00-1.12 s); ours barely deforms it.
24. **H, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** Label clipping in transit: ours cuts the label at the pill's bottom rim ("Keypad" shown half height, docs/1 0.95-1.05 s); iOS's label stays whole.
25. **M, S** Plateau ends early: iOS's pill holds level 53 to ~183 ms after the ink leaves, ours drops from ~120 ms (47 at 150 ms, 42 at 183 ms; #70).
26. **M, E** Transit shape: iOS's pill bulges into an oval mid-flight (docs/1 0.95 s); ours stays capsule-shaped.
27. **M, E** Source tab during transit: iOS's leaving ink keeps colour and is mirrored inside the pill's trailing cap; ours shows a partial cut glyph.
28. **M, S** Tap timing: iOS's bar lights 33 ms before the ink leaves; our inferred press 16 ms before (scoreboard).
29. **L, S** Tap pill level docs/1 +0.8, JGEY2190 -2.0 (scoreboard).
30. **L, S** Repeated long tap: pigment returns ~50 ms (≈7 levels) later than the measured patches (scoreboard).
31. **L, S** Early-transit inset read 0.38 of the bar at video1 frame 6 vs 0.57-0.69 at rest: unresolved calibration conflict (scoreboard).
32. **M, S** Fringe timing in a tap: iOS onset +33 ms, peak +116 ms after down, fade +173 ms after lift; ours onset +83, peak +100, fade +124 (docs/1, band 1.1).

## D. Release, throw, landing

33. **H, E** **[withdrawn 26 Oct: the comparison used a render from a different inferred script; with docs/2's own script our lens is at Contacts at 8.56 s as iOS's is]** Throw start: iOS's lens has formed and left the source tab by docs/2 8.56 s; ours is still the resting pill there (later by ~40-60 ms).
34. **M, S** Wall landings: rms 6.0 px right / 5.4 px left; the 9.9 s landing passes its rest by 36.5 px vs 41 (scoreboard).
35. **M, E** Landing overshoot outside the bar: iOS's landing lens protrudes past the bar's end with a dark soft smear beyond it (docs/2 8.68 s, LOLL8185 33.25 s); ours protrudes but with a sharper outline.
36. **M, E** Pigment return after a throw: iOS's pill is still partly clear at docs/2 8.76 s; ours is already the dark pill.
37. **L, S** Release width series: first frames narrow up to 11.2 px less than iOS (scoreboard).
38. **L, E** Release fringes: iOS's fringe peaks within a frame of the lift and fades 110-120 ms later; ours has none to time (fringe record).

## E. Resting bar and pill

39. **M, E** **[corrected and done 26 Oct: iOS lights top and bottom equally, 99/79/63/45; ours now 98/88/65/45 (specular 0.25), levels on all five unchanged]** Bar outline: iOS's resting bar has a thin bright hairline mostly on the top edge; ours draws an even lighter outline all round.
40. **M, S** Resting pill on docs/2 3 levels darker than iOS (scoreboard).
41. **M, E** Page text under the bar is slightly more smeared in ours than iOS (scoreboard note).
42. **M, E, V** Resting pill size: iOS's resting pill on Keypad spans more of the bar height (docs/1 1.25 s, LOLL8185 21.1 s) than ours.
43. **L, E** Bar cap lensing (`capFold`) judged by eye only, not scored.
44. **L, S** Light bar ends: outline 177-186 vs 178-182, 5-10 overshoot (scoreboard).
45. **L, E** iOS's bar edge shows a faint coloured fringe at the caps over bright content; ours none.

## F. Search button (trailing accessory)

46. **M, S** Release overshoot: iOS's button body overshoots its rest by 6 px after a sideways release, ours 2 px; narrowest after release 83 vs 78 px.
47. **M, S** Brightness 6.4 levels rms vs iOS during pulls (scoreboard).
48. **M, E** Press stretch: iOS stretches the button into a tall ellipse when pressed and pulled (IMG_7048 36.5 s); ours stretches less (pull gain 0.2, scored only in rise).
49. **L, E** Button rim: iOS's search button has the same crisp thin outline as its lens; ours the soft ring (see 6).

## G. Light appearance (IMG_7048 vs Pixel 7 light)

50. **M, E** Held lens shadow: ours casts a visible grey shadow under the lens; iOS's is much fainter on the bar.
51. **M, E** Held lens rim in light: iOS shows a thin rainbow at the lens's right rim; ours none.
52. **M, E** Light resting pill: iOS 245 on white; ours reads close but its outline is softer.
53. **L, E** Light bar outline thickness: iOS's grey outline is thinner than ours.
54. **L, E** Light held lens: iOS's bar grows taller under the lens (as 8).

## H. Glass buttons and toolbars over the calibration chart (COSG3073 Photos viewer vs `CalibrationActivity`, Pixel 7)

55. **H, E** Interior contrast: iOS's buttons show the chart's stripes at medium contrast (white→~200, black→~90); ours flattens them to a narrow grey range (~120-160): too much frost/too little transmission.
56. **H, E** Edge bending: iOS bends straight stripes into arcs that follow the button's outline near the rim (strong, ~25 px band on 66 pt buttons); ours compresses them into straight lines with little curvature.
57. **H, E** Circular buttons: iOS shows concentric arcs and a magnified centre stripe inside round buttons; ours shows a near-uniform grey disc with faint lines.
58. **M, E** Pill caps: iOS's toolbar pill bends the stripes into arcs at both caps; ours keeps them straight to the cap.
59. **M, E** Rim highlight: iOS has a thin bright highlight on the upper rim of each button; ours a soft lighter band.
60. **M, E** Interior lift: iOS's buttons read brighter overall over the same stripes (stronger lift); ours darker.
61. **M, E** Bottom band: ours draws a dark band along the bottom of the pill; iOS's bottom edge is a lighter, refracted strip of the content below.
62. **L, E** Top dark band in ours (along the top rim) is not in iOS.
63. **M, E** Nav-bar title pill ("Today 12:53 AM") and the round back / more buttons: iOS's read as clear glass with the content refracted; we have no matching preset for a title pill.
64. **L, E, V** Small floating pills (rating, badges) on iOS are a lighter, more transparent glass than our smallest preset.

## I. Lock screen clear lenses (COSG3073 30 s)

65. **H, E** **[withdrawn 26 Oct: at full resolution (COSG3073 28, 30, 33 s) the content is heavily blurred and upright, not inverted; a frosted button, not a ball lens]** Clear round buttons (flashlight, app shortcut) act as ball lenses: the content inside is magnified and *inverted*; we have no lens that inverts.
66. **M, E, V** Their rim is a thin dark ring with a white specular dot; ours has no specular point.
67. **M, E** Wallpaper warping with rainbow at the stripe bends behind them (lock-screen depth effect) — not modelled (likely out of scope; listed for completeness).

## J. Ink, labels and icons

68. **M, E** **[done 26 Oct: sample accent 0xFF0897FF, renders (8, 151, 255) against iOS (7-8, 151, 255)]** Selected ink colour: iOS's is a brighter, more cyan blue (~#1AA3FF); ours a deeper blue (~#3B8EEA).
69. **M, E** **[won't fix in the sample: SF Pro cannot be redistributed; apps set their own font]** Label font: iOS uses SF Pro semibold with tighter tracking; the sample uses the platform font with wider spacing.
70. **M, E** Icons: iOS's Contacts glyph is a filled person in a circle with a cut-out; ours is a different person glyph (sample assets).
71. **L, E, V** Unselected ink: iOS's white glyphs are slightly brighter.
72. **L, E** Badge: iOS shows a red count badge on Calls; the sample has none.
73. **M, E, V** Ink scale in a hold: iOS's selected item grows smoothly; ours grows with a visible step when formation passes 0.5 (by eye, LOLL8185 21.3 s).
74. **L, E, V** Ink during transit: iOS keeps the selected ink's colour inside the pill all the way; ours fades it partly to white mid-transit.

## K. Timing and dynamics (scored)

75. **M, S** Docs/2 held drag tracking rms 31.6 px (highest of the five).
76. **M, S** JGEY2190 tracking rms 26.8 px.
77. **L, S** Hold onset aligned by construction (pass 7): untested against iOS.
78. **L, S** Search button rise 3-6 px short at gain 0.6 (pass 17).
79. **L, S** Short-tap first frame: the 17 ms lead in 28 comes from the inferred finger timing, not the renderer; the press-to-light latency of our bar has never been measured on the Pixel 7.
80. **L, S** Wall-landing trailing damping candidate rejected; landings remain 4.3/5.8 mean error.

## L. Device and platform

81. **H, S** First tap after install on Pixel 7: 70-119 ms slowest frame (was 326-352); iOS shows no hitch. Still 6-10 frames over budget.
82. **M, S** Second tap: 23-47 ms frames vs 11.1 ms budget at 90 Hz; nearly every frame of a tap is over budget on the Pixel 7 (gfxinfo: 55-69 frames over 11.1 ms per 1.5 s capture).
83. **M, S** Web demo: 4.46 MB transfer, ~2 s per opening frame compiling shaders, ~7.5 s to usable unthrottled (#78).
84. **M, E** Web demo sometimes never renders (not reproduced; #78).
85. **L, S** Custom-path field first build synchronous at full resolution (#81).
86. **M, E, V** On the Pixel 7 the page content shows through the bar more sharply than iOS (text legible under the bar, "Westbrook"), dark appearance.

## M. Scenes and coverage gaps

87. **M, E** No light-mode replay score exists for holds and drags (only rest and the lens shadow were scored).
88. **M, E** No score for content-page fringes (the fringe scorer excludes page hues).
89. **M, E** No measured reference for the Material 3 components (slider, switch, sheet, menu, dialog): they are authored to look iOS-like, not compared to recordings.
90. **M, E** **[radius done 26 Oct: 34 dp; material open — COSG3073 279.5 s reads light and strongly saturated, unlike the measured dark `GlassStyle.menu()`; needs the appearance and tint of that capture before changing]** Context menu (COSG3073 ~276 s: Copy/Duplicate/Hide, Set Rating …): iOS's menu platter is a light frosted glass with rounded 34 pt corners; our `GlassDropdownMenu` was not compared to it.
91. **M, E** Toolbar grouping: iOS groups three toolbar actions into one pill with separate round buttons beside it; we have no toolbar component.
92. **L, E** Status bar time pill (red recording pill) — out of scope.

## N. Our own inconsistencies found while comparing

93. **H, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** The lens band, the rim mirror and the dispersion are tuned per state (fold vs held vs transit) with separate constants; the transitions between them are visible as a jump in fringe and refraction when formation crosses 0.5.
94. **M, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** At band 1.1 the transit pill inherits the wider band through `GlassMorph` and clips labels more (items 15, 24).
95. **M, E** **[done 26 Oct, see fringe-refraction-2026-10-26]** `GlassSelectorRimDispersion` is never applied to taps (tap material takes the lens's dispersion through the formation lerp), so the transit fringe cannot be tuned independently.
96. **M, E** Diagnostics: the replay trace (`trace.csv`) does not log transit appearance or held travel, so tap and fringe timing cannot be read from our side.
97. **L, E** The light preset's lens band was changed together with dark (1.1) without a light score.
98. **L, E** Replays of IMG_6756/JGEY2190/LOLL8185 use `script3` and docs/2 two different scripts (`v2-script.txt`, `script3.txt`); baselines must be re-rendered per script.
99. **L, E** `CalibrationActivity` shows no icons on the toolbar panels, so glyph legibility over glass is not compared.
100. **L, E** Our resting selector on Calls at app start differs from the recordings' Keypad start (sample state), so first-frame comparisons need a matched start.
101. **M, E, V** Pixel 7 light: our held lens also draws a soft grey disc behind the search button area when fused; iOS has a clean gap.
102. **L, E, V** The sample's search button shows a faint blue glyph ring under the magnifier in dark (content bleed); iOS's is clean.

## Grouping into issues

| Issue | Items |
| :-- | :-- |
| Held-lens rim optics: shape, outline, highlight, slab (new) | 3, 5, 6, 7, 12, 49, 59 |
| Rim fringes (#73) | 1, 14, 22, 32, 38, 45, 51 |
| Refraction strength vs content break-up (#77) | 2, 4, 15, 16, 24, 94 |
| Bar growth and lens geometry while held (new) | 8, 9, 10, 19, 20, 54, 101 |
| Held lens over content (#72) | 11, 13, 86 |
| Tap transit: smear, shape, plateau, timing (#70 + new) | 23, 25, 26, 27, 28, 29, 30, 31, 73, 74 |
| Formation, throw and landing timing (new) | 17, 18, 21, 33-37, 75-80 |
| Resting bar, pill and search button (new) | 39-48, 52, 53 |
| Generic glass over the calibration chart (new) | 55-64, 99 |
| Clear lenses (ball-lens inversion) (new) | 65-67 |
| Ink, label and icon parity in the sample (new) | 68-72, 100, 102 |
| Device performance (new) + #78 #81 | 81-85 |
| Scoring and tooling gaps (new) | 87-89, 96, 98 |
| Components without references (new) | 89-92 |
| Internal consistency of the selector material (new) | 93, 95, 97 |
| Light appearance (new) | 50-54 |
