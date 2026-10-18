import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFile, stat} from 'node:fs/promises';

const here = name => new URL(`../${name}`, import.meta.url);
const text = async url => (await readFile(url, 'utf8')).replace(/\r\n/g, '\n');
const pkg = JSON.parse(await readFile(here('package.json'), 'utf8'));

test('package metadata: unscoped name, private until the owner publishes, optional CanvasKit peer', () => {
  assert.equal(pkg.name, 'liquidglass');
  assert.equal(pkg.private, true);
  assert.equal(pkg.type, 'module');
  assert.equal(pkg.license, 'Apache-2.0');
  assert.equal(pkg.peerDependenciesMeta['canvaskit-wasm'].optional, true);
  assert.equal(pkg.dependencies, undefined, 'no runtime dependencies');
});

test('every listed file exists and every export target is listed', async () => {
  for (const file of pkg.files) await stat(here(file));
  const targets = Object.values(pkg.exports).flatMap(v => typeof v === 'string' ? [v] : Object.values(v));
  for (const target of targets) {
    const name = target.replace(/^\.\//, '');
    assert.ok(pkg.files.some(f => name === f || name.startsWith(f) || name === 'package.json'), `${name} is not in files`);
  }
});

test('LICENSE and NOTICE are the repository copies', async t => {
  let root;
  try { root = [await text(here('../../LICENSE')), await text(here('../../NOTICE'))]; }
  catch { t.skip('not inside the repository'); return; }
  assert.equal(await text(here('LICENSE')), root[0]);
  assert.equal(await text(here('NOTICE')), root[1]);
});

test('the HTML path never loads CanvasKit or the shader bundle', async () => {
  const graph = new Set();
  async function walk(name) {
    if (graph.has(name)) return;
    graph.add(name);
    const source = await text(here(name));
    for (const m of source.matchAll(/^\s*(?:import|export)\s[^'"]*?from\s+'(\.[^']+)'/gm))
      await walk(new URL(m[1], here(name)).pathname.split('/ports/npm/')[1]);
  }
  await walk('index.mjs');
  assert.ok(graph.has('element.mjs') && graph.has('canvaskit.mjs'));
  for (const heavy of ['canvas-engine.mjs', 'vendor/skia/painter.mjs', 'vendor/skia/shaders.mjs'])
    assert.ok(!graph.has(heavy), `${heavy} is imported statically`);
  assert.match(await text(here('element.mjs')), /await import\('\.\/canvas-engine\.mjs'\)/);
  assert.doesNotMatch(await text(here('canvaskit.mjs')), /^\s*import[^\n]*from 'canvaskit-wasm'/m);
});

test('importing the main entry outside a browser is safe (server rendering)', async () => {
  const mod = await import('../index.mjs');
  assert.equal(typeof mod.LiquidGlassElement, 'function');
  assert.equal(mod.define(), mod.LiquidGlassElement);
  assert.equal(typeof mod.configureCanvasKit, 'function');
});

test('canvas source placement: on-page sources keep their position; others cover the glass', async () => {
  const {sourcePlacement} = await import('../canvas-engine.mjs');
  const hostRect = {left: 100, top: 50, width: 200, height: 60};
  assert.deepEqual(sourcePlacement({hostRect, sourceRect: {left: 0, top: 0, width: 800, height: 400}, pad: 32}),
    {x: -68, y: -18, width: 800, height: 400});
  assert.deepEqual(sourcePlacement({hostRect, sourceRect: null, sourceWidth: 100, sourceHeight: 100, pad: 32}),
    {x: 32, y: 32 - 70, width: 200, height: 200});
});
