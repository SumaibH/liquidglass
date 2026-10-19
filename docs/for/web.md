---
description: Liquid Glass for the web, React, Vue, Svelte and plain HTML. One custom element, <liquid-glass>, refracts the live page in Chromium, or draws the iOS-measured production shaders.
---

# Liquid Glass for the web: React, Vue, Svelte, plain HTML

`<liquid-glass>` is one custom element, so it works in any framework or none. What you put inside stays
ordinary HTML: focusable, selectable, read by assistive technology. The glass is drawn behind it.

![The web component in Chromium: the HTML engine over live HTML, the canvas engine over a canvas](https://raw.githubusercontent.com/shayann07/liquidglass/main/review/web-component-2026-10-18/light.png)

*Our render: the example in headless Chromium, light scheme.*

```bash
npm install @shayxo.dev/liquidglass
```

```js
import '@shayxo.dev/liquidglass';   // registers <liquid-glass>
```

```html
<liquid-glass interactive>
  <button>Back</button><button>Share</button>
</liquid-glass>
```

In React, Vue or Svelte, use it like any element: `<liquid-glass variant="clear">…</liquid-glass>`.

- **HTML engine** (default): refracts the live page behind the element in Chromium (Chrome, Edge,
  Opera, Electron) and frosts it in Firefox and Safari.
- **Canvas engine** (`engine="canvas"`): the production shaders, unchanged, over an image, canvas or
  video you give it.
- **Compose for web:** the full library also runs in the browser through Compose Multiplatform's
  Wasm target. [Live demo](../demo.md).

Next: [the web component guide](../web-component.md) · [Limitations](../limitations.md)
