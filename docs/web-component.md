---
description: The <liquid-glass> web component: Liquid Glass for any web page or framework, refracting the live page in Chromium or drawing the production shaders.
---

# Web component

`<liquid-glass>` is a standard custom element for any web page: plain HTML, React, Vue, Svelte,
Angular or anything else that renders DOM. What you put inside it stays ordinary HTML above the glass:
buttons keep their clicks, focus and accessible names. The package is `@shayxo.dev/liquidglass` on npm (`npm install @shayxo.dev/liquidglass`). The full
package guide is [ports/npm/README.md](https://github.com/shayann07/liquidglass/blob/main/ports/npm/README.md).

```html
<script type="module">import '@shayxo.dev/liquidglass';</script>

<liquid-glass interactive>
  <button>Back</button><button>Share</button>
</liquid-glass>

<liquid-glass shape="rect" radius="24" variant="clear" style="padding: 20px">
  <h3>Weather</h3><p>Partly cloudy</p>
</liquid-glass>
```

![The example page in Chromium: the HTML engine over live HTML, the canvas engine over a canvas](https://raw.githubusercontent.com/shayann07/liquidglass/main/review/web-component-2026-10-18/light.png)

*Our render of the package's example page in Playwright's Chromium 156 headless shell (1100 px wide,
light scheme). Not compared with iOS. [All captures and their limits](https://github.com/shayann07/liquidglass/blob/main/review/web-component-2026-10-18/README.md).*

## Two engines

**`engine="html"`** (the default) refracts the **live page** behind the element with CSS
`backdrop-filter`. In Chromium it uses an SVG displacement filter whose map is computed from the
production shader's own geometry (the measured resting-corner map for Regular, the Snell bevel for
Clear), over the material's two blur kernels. Firefox and Safari do not render `url()` in
`backdrop-filter`, so there it frosts: the same kernels, tint, lift and edge light, without refraction.
This engine is an **approximation**: CSS cannot run the shader, and the package README lists what is
reproduced and what is not.

**`engine="canvas"`** draws the **production shaders unchanged**, through the
[CanvasKit painter](https://github.com/shayann07/liquidglass/blob/main/ports/skia/README.md), over a
`source` the page provides (an image, canvas, video or ImageBitmap). It is exact for what it draws, but
it only refracts that source, not the rest of the page, and it loads CanvasKit (`canvaskit.wasm` is
7.3 MB) the first time an element asks for it. It draws `variant="regular"`; `clear` falls back to the
HTML engine.

```js
import {configureCanvasKit} from '@shayxo.dev/liquidglass';
configureCanvasKit({locateFile: file => `/canvaskit/${file}`}); // where canvaskit.wasm is served
document.querySelector('liquid-glass[engine=canvas]').source = document.querySelector('#art');
```

## Attributes

| Attribute | Values | Default |
| :-- | :-- | :-- |
| `variant` | `regular` (`Glass.Regular`, the measured in-app material), `clear` (`Glass.Clear`) | `regular` |
| `shape` | `capsule`, `rect` | `capsule` |
| `radius` | corner radius in px for `rect` | `16` |
| `tint` | any CSS colour, as `Glass.tint(color)`: its alpha is the tint's opacity | none |
| `scheme` | `auto` (follows `prefers-color-scheme`), `light`, `dark` | `auto` |
| `engine` | `html`, `canvas` | `html` |
| `interactive` | boolean: the Calm press and a glow under the pointer | off |
| `lensing` | `auto`, `off` (frost everywhere) | `auto` |

`prefers-reduced-motion` stops the press motion; `prefers-reduced-transparency` and `prefers-contrast:
more` raise the material's opacity as the Kotlin library's settings do.

## Browser support

| Browser | HTML engine | Canvas engine | Checked |
| :-- | :-- | :-- | :-- |
| Chromium (Chrome, Edge, Opera) | refraction | yes | browser suite in Playwright's Chromium 156 |
| Firefox | frosted | yes (WebGL) | Playwright Firefox 157, headless: see below |
| Safari | frosted | yes (WebGL) | not on Safari; Playwright WebKit 27.2 on Windows: see below |

Lensing is used only when the CSS parser accepts `backdrop-filter: url(#…)` **and** the browser reports a
Chromium brand through User-Agent Client Hints, because Firefox accepts the value but renders nothing.
The browser suite runs in Chromium. Firefox 157 and WebKit 27.2 (Playwright builds, headless) were
checked for upgrade, frosted-path selection, clicks and the canvas engine; their blur itself did not
render in those headless builds, so the frosted look there is unverified.

## Numbers and provenance

The HTML engine **inherits** its numbers from the Kotlin library (`GlassStyle.inApp`, `GlassStyle.Clear`,
`GlassMaterial`, the shader's lighting and source-map functions) and says which of them Kotlin
measured and which it authored. A few values are **authored** here for CSS: 1 CSS px = 1 dp, the 32 px
filter pad, the frosted path, `saturate(1)`, the Increase Contrast solid chosen by scheme. A unit test
parses the Kotlin sources and fails if an inherited value drifts. The canvas engine sets none of these;
the production shader does. The table is in the
[package guide](https://github.com/shayann07/liquidglass/blob/main/ports/npm/README.md#where-the-numbers-come-from).

## Limits

- The glass refracts only up to the nearest *backdrop root*: an ancestor with `filter`, `opacity` below
  1, `mask`, `clip-path`, `mix-blend-mode`, `backdrop-filter` or `will-change` of those.
- The HTML engine does not reproduce the legibility term, dispersion, Clear's mirrored echo, inner
  shadow and Oklab highlight, or the press's backdrop swell; its rim is composited over the material
  rather than added.
- No physical-device or Safari check yet.
