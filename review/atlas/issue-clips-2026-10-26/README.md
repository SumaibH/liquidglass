# Short clips for the parity issues — 26 October 2026

Short derived clips so contributors can see the iOS behaviour an issue describes without the private recordings.
Each one is cut from a private iOS 26/27 screen recording (HDR10 decoded to display sRGB with the
`.local/sxs/hdr.py` chain) and cropped to the glass. The clips were checked frame by frame before publishing; the
recordings themselves stay private.

**Pairs** (`NNN-*-xK.mp4`): iOS on top, liquidglass `main` (`2a7c7b0`, desktop GPU replay of the same inferred
finger input, `:desktop:phoneReplay`) below, scene rows 430-660 at 2/3 scale, slowed K times. The finger input is
inferred from the video, so a few pixels of position offset can be input rather than glass. Our page behind the bar
is the sample's, not iOS's.

**iOS references** (`NNN-<recording>-*.mp4`): iOS only, scaled down.

| Clip | Issue | Source | What to look at |
| :-- | :-- | :-- | :-- |
| [072-hold-over-content-x4](072-hold-over-content-x4.mp4) | #72 | docs/2 5.4 s, 4x slow | how bright the held lens reads over page content |
| [097-cosg3073-capsule-over-chart](097-cosg3073-capsule-over-chart.mp4) | #97 | COSG3073 18-32 s | a wide glass capsule bending the calibration chart's gratings round its caps |
| [102-cosg3073-context-menu](102-cosg3073-context-menu.mp4) | #102 | COSG3073 272-282 s | Photos' context menu platter over the chart |
| [104-light-lens-x4](104-light-lens-x4.mp4) | #104 | IMG_7049 40.3 s, 4x slow (light; ours `calm-light`) | the light lens's rim, outline and shadow |
| [105-ink-lift](105-ink-lift.mp4) | #105 | IMG_6756 34.5 s | iOS's blue ink brightening under the held lens |
| [106-still-release-x8](106-still-release-x8.mp4) | #106 | IMG_6756 36.05 s, 8x slow | iOS's lens gone ~50 ms after the lift; ours ~300 ms |
| [107-lens-over-search](107-lens-over-search.mp4) | #107 | IMG_6756 35.4 s | iOS's lens drawn over the search button |
| [112-search-mode-x2](112-search-mode-x2.mp4) | #112 | iOS 27 sample reference 38.6 s, 2x slow | the search button becoming a search field above the keyboard |
| [113-pusm9766-control-center](113-pusm9766-control-center.mp4) | #113 | PUSM9766 36-41 s | Control Center's modules over the chart |
| [113-img6667-flashlight-press](113-img6667-flashlight-press.mp4) | #113 | IMG_6667 0-5 s | the lock screen's flashlight button pressed |
| [119-fast-scrub-flattens-x2](119-fast-scrub-flattens-x2.mp4) | #119 | docs/2 18.7 s, 2x slow | our lens flattening during a fast scrub |
| [120-tap-full-lens-x8](120-tap-full-lens-x8.mp4) | #120 | JGEY2190 28.0 s, 8x slow | iOS's tapped pill rising into a full lens |
| [121-side-caps-x8](121-side-caps-x8.mp4) | #121 | LOLL8185 21.2 s, 8x slow | icons bent round iOS's side caps, cut by ours |
| [123-tap-travel-x8](123-tap-travel-x8.mp4) | #123 | docs/1 3.40 s, 8x slow | iOS's pill arriving ~70 ms after the touch; ours ~300 ms |
| [124-comb-streaks-x8](124-comb-streaks-x8.mp4) | #124 | JGEY2190 4.55 s, 8x slow | green and black combs along our rim in transit |
