import {test} from 'node:test';
import assert from 'node:assert/strict';
import {kotlin, haveKotlin, literal, block, namedArgs, floatArray} from './kotlin.mjs';
import {LIGHT_OPACITY, DARK_OPACITY, BEVEL_RATIO, LIGHT, DEFAULT_TINT_AMOUNT, opacity, fineKernel, fineShare,
  regularStyle, clearStyle, quantize, tintLayer, sizeFactor, parseColor, PROVENANCE} from '../material.mjs';

// Source of truth: liquidglass/src/commonMain/kotlin/com/wexpa/liquidglass/{GlassMaterial,GlassStyle,
// Glass,LiquidGlass,GlassShaderSource}.kt, read from this repository at test time.
const skip = haveKotlin ? false : 'the Kotlin library is not beside this package';
const close = (actual, expected, label, tolerance = 1e-6) =>
  assert.ok(Math.abs(actual - expected) <= tolerance, `${label}: ${actual} vs Kotlin ${expected}`);
const ctorDefault = (ctor, name) => literal(new RegExp(`val ${name}: (?:Float|Dp) = ([^,\\n]+),`).exec(ctor)[1]);

test('GlassMaterial tables and constants match the Kotlin source', {skip}, async () => {
  const kt = await kotlin('material');
  assert.deepEqual([...LIGHT_OPACITY], floatArray(kt, 'LIGHT_OPACITY'));
  assert.deepEqual([...DARK_OPACITY], floatArray(kt, 'DARK_OPACITY'));
  close(regularStyle(true).lift, literal(/DARK_LIFT: Float = ([^\n]+)/.exec(kt)[1]), 'DARK_LIFT');
  assert.match(kt, /val lightTint: Color = Color\(0xFFF1F1F1\)/);
  close(regularStyle(false).tint[0], 0xF1 / 255, 'lightTint');
  close(BEVEL_RATIO, literal(/BEVEL_RATIO: Float = ([^\n]+)/.exec(kt)[1]), 'BEVEL_RATIO');
  close(DEFAULT_TINT_AMOUNT, literal(/DEFAULT_TINT_AMOUNT: Float = ([^\n]+)/.exec(kt)[1]), 'DEFAULT_TINT_AMOUNT');
  assert.match(kt, /return \(0\.5f \+ 1\.5f \* u \* u\)\.dp/, 'fineKernel formula');
  assert.match(kt, /val wideKernel: Dp = 10\.dp/, 'wideKernel');
  assert.match(kt, /0\.95f \* \(1f - \(tintAmount \/ 100f\)\.coerceIn\(0f, 1f\)\)\.pow\(1\.5f\)/, 'fineShare formula');
  close(fineKernel(50), 0.875, 'fineKernel(50)');
  close(fineShare(50), 0.95 * Math.pow(0.5, 1.5), 'fineShare(50)');
  close(opacity(false, 50), 0.565, 'light opacity at 50');
  close(opacity(true, 50), 0.451, 'dark opacity at 50');
  close(opacity(true, 40), 0.439 + (0.451 - 0.439) * (40 - 29) / 21, 'dark opacity interpolated');
  close(regularStyle(false).wideSigma, 10, 'wide sigma');
});

test('Glass.Regular resolves to GlassStyle.inApp, edge line included', {skip}, async () => {
  const kt = await kotlin('style');
  const inApp = namedArgs(block(kt, 'fun inApp(', 'fun toolbar('));
  for (const dark of [false, true]) {
    const s = regularStyle(dark);
    for (const key of ['bevel', 'bevelPeak', 'specular', 'specularPower', 'counterLight', 'edgeLight', 'fresnel', 'innerShadow'])
      close(s[key], inApp[key], key);
    close(s.tint[3], quantize(dark ? 0.451 : 0.565), 'tint alpha (8-bit, as Compose stores it)');
  }
  assert.match(kt, /blurRadius = GlassMaterial\.fineKernel\(tintAmount\) \/ 0\.6f/, 'scatter radius = sigma / 0.6');
  const glass = await kotlin('glass');
  assert.match(glass, /val Regular: Glass = Glass\(\{ dark -> GlassStyle\.inApp\(dark\) \}/);
  assert.match(glass, /val Clear: Glass = Glass\(\{ GlassStyle\.Clear \}/);
  assert.match(glass, /if \(tintColor == null\) s else s\.copy\(tint = tintColor\)/, 'tint replaces the style tint');
  const light = /data class GlassLight\(val x: Float = ([^,]+), val y: Float = ([^)]+)\)/.exec(kt);
  close(LIGHT.x, literal(light[1]), 'GlassLight.x'); close(LIGHT.y, literal(light[2]), 'GlassLight.y');
  // Constructor defaults the inApp factory keeps.
  const ctor = block(kt, 'data class GlassStyle(', 'companion object');
  const s = regularStyle(false);
  close(s.ior, ctorDefault(ctor, 'indexOfRefraction'), 'indexOfRefraction');
  close(s.bevelPower, ctorDefault(ctor, 'bevelPower'), 'bevelPower');
  close(s.refractionDepth, ctorDefault(ctor, 'refractionDepth'), 'refractionDepth');
  close(s.legibility, ctorDefault(ctor, 'legibility'), 'legibility');
});

test('Glass.Clear resolves to GlassStyle.Clear (legacy profile)', {skip}, async () => {
  const kt = await kotlin('style');
  const text = block(kt, 'val Clear = GlassStyle(', 'val Thick');
  const clear = namedArgs(text), s = clearStyle();
  assert.match(text, /profile = GlassProfile\.Legacy/);
  assert.match(text, /tint = Color\.White\.copy\(alpha = 0\.04f\)/);
  close(s.tint[3], quantize(0.04), 'tint alpha');
  close(s.fineSigma, clear.blurRadius * 0.6, 'scatter sigma');
  for (const key of ['refractionBand', 'refractionDepth', 'dispersion', 'mirror', 'bevel', 'specular', 'fresnel',
    'innerShadow', 'dimmingLayer', 'legibility']) close(s[key], clear[key], key);
  // Parameters Clear does not set keep the constructor defaults.
  const ctor = block(kt, 'data class GlassStyle(', 'companion object');
  for (const key of ['specularPower', 'counterLight', 'edgeLight', 'bevelPeak', 'highlightChroma']) {
    assert.ok(!(key in clear), `${key} is set by Clear; update clearStyle`);
    close(s[key], ctorDefault(ctor, key), `${key} default`);
  }
});

test('host and shader terms the HTML engine reproduces match the Kotlin source', {skip}, async () => {
  const host = await kotlin('host'), shader = await kotlin('shader');
  assert.match(host, /return \(\(minEdgeDp - 56f\) \/ \(320f - 56f\)\)\.coerceIn\(0f, 1f\)/, 'elementSizeFactor');
  assert.match(host, /GlassMaterial\.BEVEL_RATIO \* minOf\(r, cap\)/, 'bandPx');
  assert.match(shader, /float strength = uTint\.a \* tintCoverage \* mix\(mix\(0\.9, 1\.25, scale\), 1\.0, measured\)/);
  assert.match(shader, /\+ clamp\(uFrost, 0\.0, 1\.0\) \* 0\.35;/);
  assert.match(shader, /strength = clamp\(strength \* mat, 0\.0, 0\.95\);/);
  assert.match(shader, /half3 blended = mix\(bg, tinted, half\(strength\)\);/);
  assert.match(shader, /col = mix\(col, solid, half\(contrastBoost \* 0\.88\)\);/);
  assert.match(shader, /col \+= half3\(half\(touchFall \* 0\.06 \* mat\)\);/);
  assert.match(shader, /float touchSigma = max\(min\(uSize\.x, uSize\.y\), 1\.0\);/);
  assert.match(shader, /mix\(0\.05, 0\.063, heldAmt\)/, 'measured rim glow');
  assert.match(shader, /float lens = uRefractDepth \* mix\(0\.85, 1\.25, scale\) \* mat;/);
  assert.equal(sizeFactor(56, 400), 0); assert.equal(sizeFactor(320, 400), 1); close(sizeFactor(188, 400), 0.5, 'mid');
});

test('tint layer reproduces mix(bg, tint, s) + lift exactly with one normal-blend colour', () => {
  for (const dark of [false, true]) {
    const style = regularStyle(dark), layer = tintLayer(style);
    assert.equal(layer.exact, true);
    for (const bg of [0, 0.25, 0.5, 1]) {
      const shader = (1 - style.tint[3]) * bg + style.tint[3] * style.tint[0] + style.lift;
      const css = (1 - layer.alpha) * bg + layer.alpha * layer.rgb[0];
      close(css, shader, `dark ${dark} over ${bg}`, 1e-12);
    }
  }
  // Dark Regular: black at 115/255 plus 35/255 is grey 35/115 at 115/255.
  const dark = tintLayer(regularStyle(true));
  close(dark.alpha, 115 / 255, 'alpha'); close(dark.rgb[0], 35 / 115, 'colour');
  // Glass.tint replaces the tint (quantized to 8 bits) and keeps the lift; strength is clamped at 0.95.
  const blue = tintLayer(regularStyle(true), {tint: [0.04, 0.52, 1, 0.5]});
  close(blue.alpha, 128 / 255, 'tint alpha'); close(blue.rgb[0], 10 / 255 + (35 / 255) / (128 / 255), 'red + lift');
  assert.equal(blue.exact, false, 'blue channel clamps at 1 once the lift is folded in');
  close(tintLayer(regularStyle(false), {tint: [0, 0, 1, 1]}).alpha, 0.95, 'clamp');
  // Clear's legacy strength grows with size; Reduce Transparency adds 0.35.
  close(tintLayer(clearStyle(), {width: 40, height: 40}).alpha, quantize(0.04) * 0.9, 'clear small');
  close(tintLayer(clearStyle(), {width: 400, height: 400}).alpha, quantize(0.04) * 1.25, 'clear large');
  close(tintLayer(regularStyle(false), {reducedTransparency: 1}).alpha, quantize(0.565) + 0.35, 'frost');
});

test('colour strings browsers serialize parse to sRGB components', () => {
  assert.deepEqual(parseColor('#0a84ff'), [10 / 255, 132 / 255, 1, 1]);
  assert.deepEqual(parseColor('#fff8'), [1, 1, 1, 0x88 / 255]);
  assert.deepEqual(parseColor('rgb(10, 132, 255)'), [10 / 255, 132 / 255, 1, 1]);
  assert.deepEqual(parseColor('rgba(10, 132, 255, 0.5)'), [10 / 255, 132 / 255, 1, 0.5]);
  assert.deepEqual(parseColor('rgb(10 132 255 / 50%)'), [10 / 255, 132 / 255, 1, 0.5]);
  for (const bad of ['blue', 'oklch(0.6 0.2 250)', '#12', 'rgb(1, 2)', null]) assert.equal(parseColor(bad), null);
});

test('every provenance row is labelled inherited or authored, and inherited rows name their Kotlin kind', () => {
  for (const row of PROVENANCE) {
    assert.ok(['inherited', 'authored'].includes(row.kind), row.name);
    if (row.kind === 'inherited') assert.ok(/^(measured|authored)/.test(row.kotlin), `${row.name} needs measured/authored`);
    else assert.equal(row.kotlin, null, row.name);
  }
});
