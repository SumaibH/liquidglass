/** Apache-2.0. Copy the CanvasKit painter from ports/skia so this package needs no sibling at runtime.
 *
 * The canvas engine draws through the production shaders in ports/skia. A published package cannot
 * reach a sibling directory, so the files it imports are copied, byte for byte (line endings
 * normalized), into vendor/skia/. `node vendor-skia.mjs --check` (part of `npm test`) fails when a
 * copy drifts from its source; run `npm run vendor` after changing ports/skia.
 */
import {mkdir, readFile, writeFile, readdir} from 'node:fs/promises';

// painter.mjs and everything it imports, plus the portable Calm controller the element presses with.
export const VENDORED = [
  'painter.mjs', 'renderer.mjs', 'material.mjs', 'backdrop.mjs', 'shaders.mjs',
  'selector-material.mjs', 'selector-material-data.mjs', 'interaction.mjs', 'LICENSE', 'NOTICE',
];
const from = name => new URL(`../skia/${name}`, import.meta.url);
const to = name => new URL(`vendor/skia/${name}`, import.meta.url);
const normalize = text => text.replace(/\r\n/g, '\n');

const check = process.argv.includes('--check');
const stale = [];
let sourceMissing = false;
for (const name of VENDORED) {
  let source;
  try { source = normalize(await readFile(from(name), 'utf8')); }
  catch (error) { if (error.code !== 'ENOENT') throw error; sourceMissing = true; continue; }
  if (check) {
    const actual = await readFile(to(name), 'utf8').catch(error => { if (error.code === 'ENOENT') return null; throw error; });
    if (actual === null || normalize(actual) !== source) stale.push(name);
  } else {
    await mkdir(new URL('vendor/skia/', import.meta.url), {recursive: true});
    await writeFile(to(name), source);
  }
}
if (sourceMissing) {
  // A published copy has no ports/skia beside it; the vendored files are then the only source.
  console.log('ports/skia is not beside this package; vendored files left as they are.');
} else if (check) {
  const present = await readdir(new URL('vendor/skia/', import.meta.url)).catch(() => []);
  const extra = present.filter(name => !VENDORED.includes(name));
  if (stale.length || extra.length)
    throw new Error(`vendor/skia is stale (${[...stale, ...extra].join(', ')}). Run npm run vendor in ports/npm.`);
  console.log(`vendor/skia matches ports/skia (${VENDORED.length} files).`);
} else {
  console.log(`Copied ${VENDORED.length} files from ports/skia into vendor/skia.`);
}
