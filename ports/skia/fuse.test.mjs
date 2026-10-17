// Apache-2.0. The bar's fusion with a free button against Kotlin's own geometry (fixtures/accessory-fuse.json,
// GlassAccessoryFusePortFixtureTest), and the fused outline drawn by the production shader.
import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';
import CanvasKitInit from 'canvaskit-wasm';
import {accessoryFuse, accessoryYield} from './fuse.mjs';
import {createGlassPainter} from './painter.mjs';

const fixture = JSON.parse(await readFile(new URL('./fixtures/accessory-fuse.json', import.meta.url), 'utf8'));

test('accessory fuse: the rect the bar reaches for matches Compose for every press and pull', () => {
  const [W, H] = fixture.bar, [l, t, r, b] = fixture.button;
  let worst = 0;
  for (const c of fixture.cases) {
    const out = accessoryFuse({bar: {width: W, height: H, pressX: c.barPressX, pressY: c.barPressY},
      button: {x: l, y: t, width: r - l, height: b - t}, transform: c, smoothing: fixture.smoothing});
    out.forEach((v, i) => { const e = Math.abs(v - c.fuse[i]); worst = Math.max(worst, e);
      assert.ok(e < 2e-3, `case ${JSON.stringify(c)} [${i}]: ${v} vs ${c.fuse[i]}`); });
  }
  assert.ok(fixture.cases.length >= 18, `worst ${worst}`);
});

test('accessory yield: the neck gives way as a formed lens reaches the bar end', () => {
  const [W, H] = fixture.bar;
  for (const y of fixture.yields)
    assert.ok(Math.abs(accessoryYield(y.formation, y.endEdge, W, H) - y.yield) < 1e-5, JSON.stringify(y));
});

test('accessory fuse: drawCalmBar draws the neck into the gap and leaves the bar alone', async () => {
  const kit = await CanvasKitInit();
  const w = 420, h = 120, surface = kit.MakeSurface(w, h), canvas = surface.getCanvas();
  const page = new Uint8Array(w * h * 4).fill(0); for (let i = 3; i < page.length; i += 4) page[i] = 255;
  const image = kit.MakeImage({width: w, height: h, alphaType: kit.AlphaType.Unpremul, colorType: kit.ColorType.RGBA_8888,
    colorSpace: kit.ColorSpace.SRGB}, page, w * 4);
  const painter = createGlassPainter(kit);
  // A 278 x 62 bar with a 62 px button 8 px to its right, pressed (1.26): the press closes the gap.
  const rect = {x: 20, y: 30, width: 278, height: 62};
  const fuse = accessoryFuse({bar: {width: 278, height: 62}, button: {x: 286, y: 0, width: 62, height: 62},
    transform: {pressScale: 1.26}, smoothing: 6});
  const read = () => canvas.readPixels(0, 0, {width: w, height: h, colorType: kit.ColorType.RGBA_8888,
    alphaType: kit.AlphaType.Premul, colorSpace: kit.ColorSpace.SRGB});
  const lum = (p, x, y) => { const i = (y * w + x) * 4; return p[i] + p[i + 1] + p[i + 2]; };
  try {
    painter.setSource(image);
    canvas.drawImage(image, 0, 0); painter.drawCalmBar(canvas, rect, {dark: true}); surface.flush();
    const plain = read();
    canvas.drawImage(image, 0, 0); painter.drawCalmBar(canvas, rect, {dark: true, fuse}); surface.flush();
    const fused = read();
    // The gap between the bar's end (298) and the button (306) on the bar's mid line: black without
    // the fuse, glass with it.
    assert.equal(lum(plain, 302, 61), 0, 'the gap was not empty without the fuse');
    assert.ok(lum(fused, 302, 61) > 60, `no neck in the gap: ${lum(fused, 302, 61)}`);
    // The bar's middle is untouched by the fuse.
    assert.ok(Math.abs(lum(fused, 150, 61) - lum(plain, 150, 61)) <= 3, 'the fuse changed the bar');
    assert.throws(() => painter.drawCalmBar(canvas, rect, {fuse: [1, 2, 3]}), RangeError);
  } finally { painter.dispose(); image.delete(); surface.delete(); }
});
