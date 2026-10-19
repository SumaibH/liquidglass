/** Apache-2.0. The adapter draws what the painter draws on CanvasKit directly, pixel for pixel. */
import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import {createGlassPainter} from '../vendor/skia/painter.js';
import {CanvasKit, Skia, headless, glass, pagePixels, pageImage, canvasKitPage, pixelsOf} from './headless.js';

const W = 480, H = 320;
const scene = painter => [
  ['drawSurface', {x: 30, y: 200, width: 220, height: 84, radius: 26}, {dark: true, density: 2}],
  ['drawSurface', {x: 270, y: 30, width: 180, height: 120, radius: [30, 30, 12, 12]}, {dark: false, tintAmount: 30, light: [0, -1]}],
  ['drawLens', {x: 60, y: 40, width: 140, magnification: 1.5}],
].forEach(([method, bounds, options]) => painter[method](painter.canvasForTest, bounds, options));

/** The reference: CanvasKit with no adapter. */
function directRender() {
  const surface = CanvasKit.MakeSurface(W, H), page = canvasKitPage(), painter = createGlassPainter(CanvasKit);
  try {
    const canvas = surface.getCanvas();
    canvas.drawImage(page, 0, 0);
    painter.setSource(page);
    scene(Object.assign(painter, {canvasForTest: canvas}));
    surface.flush();
    const image = surface.makeImageSnapshot();
    try { return pixelsOf(image); } finally { image.delete(); }
  } finally { painter.dispose(); page.delete(); surface.delete(); }
}

/** The same through React Native Skia's API and the adapter. */
function adapterRender() {
  const surface = Skia.Surface.Make(W, H), page = pageImage(), painter = glass.createPainter();
  try {
    const canvas = surface.getCanvas();
    canvas.drawImage(page, 0, 0);
    painter.setSource(glass.kit.wrap(page));
    scene(Object.assign(painter, {canvasForTest: glass.kit.canvas(canvas)}));
    surface.flush();
    return pixelsOf(surface.makeImageSnapshot());
  } finally { painter.dispose(); page.dispose(); surface.dispose(); }
}

function difference(a, b) {
  let max = 0, sum = 0;
  for (let i = 0; i < a.length; i++) { const d = Math.abs(a[i] - b[i]); max = Math.max(max, d); sum += d; }
  return {max, mean: sum / a.length};
}

test('the adapter draws surfaces and lenses exactly as CanvasKit does', () => {
  const direct = directRender(), adapted = adapterRender();
  assert.equal(adapted.length, direct.length);
  const d = difference(direct, adapted);
  assert.ok(d.max <= 1, `max channel difference ${d.max} (mean ${d.mean.toFixed(4)})`);
  // And the glass did draw: the page under the dark surface changed.
  const page = pagePixels().pixels;
  assert.ok(difference(page, adapted).mean > 1, 'the glass changed the page');
});

test('GlassSurface and GlassLens render inside a React Native Skia tree', async () => {
  const source = pageImage();
  const surface = headless.makeOffscreenSurface(W, H);
  const element = React.createElement(headless.Group, null,
    React.createElement(headless.Image, {image: source, x: 0, y: 0, width: W, height: H}),
    React.createElement(glass.GlassSurface, {source, x: 30, y: 200, width: 220, height: 84, radius: 26, dark: true, pixelRatio: 1}),
    React.createElement(glass.GlassLens, {source, x: 60, y: 40, width: 140, magnification: 1.5, pixelRatio: 1}));
  const image = await headless.drawOffscreen(surface, element);
  const shot = pixelsOf(image), page = pagePixels().pixels;
  const block = (cx, cy, r) => {
    let sum = 0, n = 0;
    for (let y = cy - r; y <= cy + r; y++) for (let x = cx - r; x <= cx + r; x++) for (let c = 0; c < 3; c++) {
      const i = (y * W + x) * 4 + c; sum += Math.abs(shot[i] - page[i]); n++;
    }
    return sum / n;
  };
  assert.ok(block(140, 242, 20) > 10, `glass under the surface (${block(140, 242, 20).toFixed(1)})`);
  assert.ok(block(130, 110, 20) > 5, `glass under the lens (${block(130, 110, 20).toFixed(1)})`);
  assert.ok(block(420, 290, 10) < 1, `no glass far from both (${block(420, 290, 10).toFixed(1)})`);
});

test('the adapter releases what it creates and rejects a shader it cannot compile', () => {
  const painter = glass.createPainter();
  painter.dispose();
  painter.dispose();
  assert.throws(() => painter.setSource(glass.kit.wrap(pageImage())), /disposed/);
  let message = '';
  assert.equal(glass.kit.RuntimeEffect.Make('this is not SkSL', m => { message = m; }), null);
  assert.ok(message.length > 0);
});
