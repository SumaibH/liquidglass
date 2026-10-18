import {test} from 'node:test';
import assert from 'node:assert/strict';
import {parseAttributes, parseRadius, parseBoolean, cornerRadius, isDark, detectLensing,
  detectBackdropFilter, selectEngine, DEFAULT_RECT_RADIUS} from '../engine.mjs';

const attrs = map => parseAttributes(name => (name in map ? map[name] : null));

test('attributes parse to their documented values and defaults', () => {
  assert.deepEqual(attrs({}), {variant: 'regular', shape: 'capsule', radius: DEFAULT_RECT_RADIUS, tint: null,
    scheme: 'auto', engine: 'html', interactive: false, lensing: 'auto'});
  assert.deepEqual(attrs({variant: ' Clear ', shape: 'RECT', radius: '24px', tint: ' #0a84ff ', scheme: 'dark',
    engine: 'canvas', interactive: '', lensing: 'off'}), {variant: 'clear', shape: 'rect', radius: 24,
    tint: '#0a84ff', scheme: 'dark', engine: 'canvas', interactive: true, lensing: 'off'});
  // Unknown values fall back rather than throwing: markup must never break the page.
  assert.deepEqual(attrs({variant: 'thick', shape: 'circle', scheme: 'sepia', engine: 'webgpu', lensing: 'on', tint: '  '}),
    attrs({}));
});

test('radius accepts nonnegative px numbers only', () => {
  assert.equal(parseRadius('0'), 0);
  assert.equal(parseRadius(12.5), 12.5);
  assert.equal(parseRadius(' 30 '), 30);
  for (const bad of ['-4', 'abc', 'NaN', 'Infinity', '', null, undefined]) assert.equal(parseRadius(bad), DEFAULT_RECT_RADIUS);
});

test('boolean attributes follow HTML presence semantics', () => {
  assert.equal(parseBoolean(''), true);
  assert.equal(parseBoolean('false'), true); // present is true, as for <button disabled="false">
  assert.equal(parseBoolean(null), false);
});

test('corner radius: capsule is half the short side; rect is limited to the inradius', () => {
  assert.equal(cornerRadius({shape: 'capsule', radius: 99}, 300, 56), 28);
  assert.equal(cornerRadius({shape: 'rect', radius: 16}, 300, 56), 16);
  assert.equal(cornerRadius({shape: 'rect', radius: 80}, 300, 56), 28);
  assert.equal(cornerRadius({shape: 'rect', radius: 0}, 300, 56), 0);
});

test('scheme auto follows the system preference; explicit schemes do not', () => {
  assert.equal(isDark('auto', true), true);
  assert.equal(isDark('auto', false), false);
  assert.equal(isDark('light', true), false);
  assert.equal(isDark('dark', false), true);
});

test('lensing needs the CSS parser AND a Chromium brand; parser support alone is not trusted', () => {
  const css = ok => ({supports: (p, v) => ok && v.startsWith('url')});
  const chromium = {userAgentData: {brands: [{brand: 'Not A;Brand'}, {brand: 'Chromium'}, {brand: 'Google Chrome'}]}};
  assert.equal(detectLensing({CSS: css(true), navigator: chromium}), true);
  // Firefox: CSS.supports('backdrop-filter', 'url(#a)') is true but nothing renders; no client hints.
  assert.equal(detectLensing({CSS: css(true), navigator: {}}), false);
  assert.equal(detectLensing({CSS: css(true), navigator: {userAgentData: {brands: [{brand: 'Firefox'}]}}}), false);
  assert.equal(detectLensing({CSS: css(false), navigator: chromium}), false);
  assert.equal(detectLensing({navigator: chromium}), false);
  assert.equal(detectLensing({CSS: {supports() { throw new Error('x'); }}, navigator: chromium}), false);
  assert.equal(detectBackdropFilter({CSS: {supports: (p, v) => p === '-webkit-backdrop-filter' && v === 'blur(1px)'}}), true);
  assert.equal(detectBackdropFilter({}), false);
});

test('engine selection: canvas only for regular glass with a source; html optics by capability', () => {
  const base = {lensingSupported: true, backdropSupported: true};
  assert.deepEqual(selectEngine({...base, engine: 'html', variant: 'regular'}), {engine: 'html', optics: 'lens', reason: null});
  assert.deepEqual(selectEngine({...base, engine: 'html', variant: 'clear', lensing: 'off'}), {engine: 'html', optics: 'frost', reason: null});
  assert.deepEqual(selectEngine({...base, engine: 'html', variant: 'regular', lensingSupported: false}), {engine: 'html', optics: 'frost', reason: null});
  assert.deepEqual(selectEngine({...base, engine: 'html', variant: 'regular', backdropSupported: false}), {engine: 'html', optics: 'flat', reason: null});
  assert.deepEqual(selectEngine({...base, engine: 'canvas', variant: 'regular', hasSource: true}), {engine: 'canvas', optics: 'shader', reason: null});
  assert.equal(selectEngine({...base, engine: 'canvas', variant: 'regular', hasSource: false}).reason, 'canvas-no-source');
  assert.equal(selectEngine({...base, engine: 'canvas', variant: 'clear', hasSource: true}).reason, 'canvas-variant');
  assert.equal(selectEngine({...base, engine: 'canvas', variant: 'regular', hasSource: true, canvasFailed: true}).reason, 'canvas-unavailable');
  assert.equal(selectEngine({...base, engine: 'canvas', variant: 'clear', hasSource: true}).engine, 'html');
});
