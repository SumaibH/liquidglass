/** Apache-2.0. test/material_fixture.json: ports/skia's in-app material uniforms for a few surfaces, which
 * the Dart port (lib/src/material.dart) must reproduce. `--check` fails when the fixture is stale.
 */
import {readFile, writeFile} from 'node:fs/promises';
import {inAppMaterialUniforms} from '../../skia/material.mjs';

const frag = (await readFile(new URL('../shaders/liquid_glass.frag', import.meta.url), 'utf8')).replace(/\r\n/g, '\n');
const sizes = {float: 1, vec2: 2, vec3: 3, vec4: 4};
const layout = [...frag.matchAll(/^uniform\s+(float|vec2|vec3|vec4)\s+(\w+)\s*;/gm)]
  .filter(([, , name]) => !['uTexSize', 'uOrigin', 'uFieldSize'].includes(name))
  .map(([, type, name]) => ({name, columns: sizes[type], rows: 1}));
const effects = {layout: () => layout};
// Logical sizes and a density, as the widget sees them; the JS port takes device pixels.
const cases = [
  {width: 360, height: 92, radius: 28, density: 2.625, dark: false, tintAmount: 50, light: [0, -1]},
  {width: 312, height: 72, radius: 36, density: 2.625, dark: true, tintAmount: 50, light: [-0.35, -0.94]},
  {width: 120, height: 120, radius: 20, density: 1, dark: false, tintAmount: 30, light: [-0.35, -0.94],
    tint: [0.2, 0.4, 0.9, 0.35], materialize: 0.5, reducedTransparency: 0.25, increasedContrast: 0.75},
];
const fixture = cases.map(c => {
  const d = c.density;
  const values = inAppMaterialUniforms(effects, {...c, width: c.width * d, height: c.height * d, radius: c.radius * d, pad: 2});
  return {input: c, uniforms: Object.fromEntries(Object.entries(values).map(([k, v]) => [k, typeof v === 'number' ? [v] : Array.from(v)]))};
});
const output = JSON.stringify(fixture, null, 1) + '\n';
const target = new URL('../test/material_fixture.json', import.meta.url);
if (process.argv.includes('--check')) {
  const actual = await readFile(target, 'utf8').catch(() => '');
  if (actual.replace(/\r\n/g, '\n') !== output) throw new Error('test/material_fixture.json is stale. Run node tool/generate_fixture.mjs.');
  console.log(`material_fixture.json matches ports/skia (${fixture.length} cases).`);
} else {
  await writeFile(target, output);
  console.log(`Wrote test/material_fixture.json (${fixture.length} cases).`);
}
